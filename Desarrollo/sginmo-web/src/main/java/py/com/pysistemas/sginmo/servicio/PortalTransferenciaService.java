package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0083 (Fase 1) - Informar transferencia desde el portal + bandeja operativa + aplicacion.
 * El socio informa una transferencia y adjunta el comprobante (RECIBIDO). Un usuario interno la revisa
 * en la bandeja y, si es valida, la APLICA reutilizando el motor de cobros (forma TRANSFERENCIA), o la
 * OBSERVA/RECHAZA con motivo. Sin OCR ni conciliacion bancaria (Fases 2/3). @AislarTenant + RLS.
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class PortalTransferenciaService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;
    @Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @Inject
    private py.com.one.security.web.SesionUsuario sesion;
    @Inject
    private AuditoriaFuncionalService auditoria;
    @Inject
    private CajaService cajaService;
    @Inject
    private CatalogoService catalogoService;
    @Inject
    private ParametroConfig parametros;
    @Inject
    private ComprobanteOcrService ocr;   // REQ-0084

    public static final String PANTALLA = "transferencias";

    private Path baseDir() {
        String dir = System.getenv("SGINMO_ARCHIVOS_DIR");
        if (dir == null || dir.isBlank()) dir = System.getProperty("user.home", ".") + "/sginmo/archivos";
        return Path.of(dir);
    }

    // ── Portal (socio) ─────────────────────────────────────────────────────────

    /** El socio informa una transferencia y adjunta el comprobante. Devuelve el id creado. */
    public Long informar(Long persona, Datos d, byte[] archivo, String nombreArchivo, String mime) {
        if (persona == null) throw new NegocioException("Sesion invalida");
        if (d == null || d.importe == null || d.importe.signum() <= 0) throw new NegocioException("Indique el importe de la transferencia");
        if (archivo == null || archivo.length == 0) throw new NegocioException("Adjunte el comprobante de la transferencia");
        int maxMb = Math.max(1, parametros.entero("PORTAL_TRANSF_TAMANO_MAX_MB", 8));
        if (archivo.length > (long) maxMb * 1024 * 1024) throw new NegocioException("El comprobante supera el maximo de " + maxMb + " MB");
        // obs 305: validar el CONTENIDO REAL por firma (magic bytes), no solo extension/MIME declarados,
        // y exigir que coincida con lo declarado. Se guarda con la extension detectada del contenido.
        String ext = firmaContenido(archivo);
        if (ext == null) throw new NegocioException("El contenido del comprobante no es un PDF/JPG/PNG/WEBP valido");
        String declarada = extensionValida(nombreArchivo, mime);
        if (declarada != null && !declarada.equals(ext))
            throw new NegocioException("El archivo no coincide con su tipo declarado");

        Long t = tenant.actual();
        String fisico = "trf_" + java.util.UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = baseDir().resolve(String.valueOf(t));
            Files.createDirectories(dir);
            Files.write(dir.resolve(fisico), archivo);
        } catch (Exception e) {
            throw new NegocioException("No se pudo guardar el comprobante");
        }
        String hash = sha256(archivo);

        Object id = em.createNativeQuery(
            "INSERT INTO portal_pago_transferencia (tenant, persona, estado, importe, moneda, fecha_transferencia,"
          + " banco_origen, cuenta_origen, cuenta_destino, numero_transaccion, observacion_cliente, documento,"
          + " archivo_nombre, archivo_fisico, archivo_mime, archivo_hash, archivo_tamano)"
          + " VALUES (:t,:p,'RECIBIDO',:imp,:mon,:fec,:bo,:co,:cd,:nt,:obs,:doc,:an,:af,:am,:ah,:at)"
          + " RETURNING portal_pago_transferencia")
            .setParameter("t", t).setParameter("p", persona).setParameter("imp", d.importe).setParameter("mon", d.moneda)
            .setParameter("fec", d.fecha == null ? null : java.sql.Date.valueOf(d.fecha))
            .setParameter("bo", recorta(d.bancoOrigen, 80)).setParameter("co", recorta(d.cuentaOrigen, 40))
            .setParameter("cd", recorta(d.cuentaDestino, 40)).setParameter("nt", recorta(d.numeroTransaccion, 60))
            .setParameter("obs", recorta(d.observacion, 300)).setParameter("doc", d.documento)
            .setParameter("an", recorta(nombreArchivo, 255)).setParameter("af", fisico)
            .setParameter("am", recorta(mime, 120)).setParameter("ah", hash).setParameter("at", (long) archivo.length)
            .getSingleResult();
        Long nuevo = ((Number) id).longValue();
        auditar(nuevo, AuditoriaFuncionalService.CREAR, "transferencia informada por el socio");
        // REQ-0084: OCR/extraccion best-effort (no bloquea el flujo si falla o no hay motor).
        try {
            ComprobanteOcrService.Resultado o = ocr.extraer(archivo, mime);
            em.createNativeQuery(
                "UPDATE portal_pago_transferencia SET texto_ocr = :tx, ocr_importe = :imp, ocr_fecha = :fec,"
              + " ocr_numero = :num, ocr_banco = :ban, confianza_ocr = :cf, ocr_procesado = true, ocr_motor = :mot"
              + " WHERE portal_pago_transferencia = :id")
                .setParameter("tx", o.texto == null ? null : (o.texto.length() > 20000 ? o.texto.substring(0, 20000) : o.texto))
                .setParameter("imp", o.importe)
                .setParameter("fec", o.fecha == null ? null : java.sql.Date.valueOf(o.fecha))
                .setParameter("num", recorta(o.numero, 60)).setParameter("ban", recorta(o.banco, 80))
                .setParameter("cf", o.confianza).setParameter("mot", o.motor).setParameter("id", nuevo)
                .executeUpdate();
        } catch (RuntimeException ignore) { /* el OCR es un insumo, no bloquea informar */ }
        return nuevo;
    }

    /** Transferencias informadas por el socio (para el portal). */
    public List<Fila> mias(Long persona) {
        List<Fila> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT portal_pago_transferencia, fecha, importe, estado, numero_transaccion, motivo_revision, cobro"
          + " FROM portal_pago_transferencia WHERE persona = :p ORDER BY fecha DESC")
            .setParameter("p", persona).getResultList();
        for (Object[] f : rows) out.add(fila(f));
        return out;
    }

    // ── Bandeja (interno) ───────────────────────────────────────────────────────

    /** Bandeja operativa: transferencias del tenant, opcionalmente filtradas por estado. */
    public List<Fila> bandeja(String estado) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Fila> out = new ArrayList<>();
        String cond = (estado != null && !estado.isBlank()) ? " AND estado = :e" : "";
        var q = em.createNativeQuery(
            "SELECT t.portal_pago_transferencia, t.fecha, t.importe, t.estado, t.numero_transaccion, t.motivo_revision,"
          + " t.cobro, p.nombre, t.banco_origen, t.persona, t.documento, t.moneda, t.cuenta_origen,"
          + " t.texto_ocr, t.ocr_importe, t.ocr_fecha, t.ocr_numero, t.ocr_banco, t.confianza_ocr, t.ocr_motor"
          + " FROM portal_pago_transferencia t LEFT JOIN persona p ON p.persona = t.persona"
          + " WHERE 1=1" + cond + " ORDER BY CASE t.estado WHEN 'RECIBIDO' THEN 1 WHEN 'EN_REVISION' THEN 2"
          + " WHEN 'OBSERVADO' THEN 3 ELSE 4 END, t.fecha DESC");
        if (!cond.isEmpty()) q.setParameter("e", estado);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) {
            Fila x = fila(f);
            x.cliente = (String) f[7];
            x.bancoOrigen = (String) f[8];
            x.persona = f[9] == null ? null : ((Number) f[9]).longValue();
            x.documento = f[10] == null ? null : ((Number) f[10]).longValue();
            x.moneda = f[11] == null ? null : ((Number) f[11]).longValue();
            x.cuentaOrigen = (String) f[12];
            x.textoOcr = (String) f[13];
            x.ocrImporte = (BigDecimal) f[14];
            x.ocrFecha = aLocalDate(f[15]);
            x.ocrNumero = (String) f[16];
            x.ocrBanco = (String) f[17];
            x.confianzaOcr = (BigDecimal) f[18];
            x.ocrMotor = (String) f[19];
            out.add(x);
        }
        return out;
    }

    public Fila porId(Long id) {
        autorizacion.exigir(PANTALLA, "VER");
        var rows = bandeja(null);
        return rows.stream().filter(r -> r.id.equals(id)).findFirst().orElse(null);
    }

    /** Observa la transferencia con motivo visible para el socio. */
    public void observar(Long id, String motivo) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (motivo == null || motivo.isBlank()) throw new NegocioException("Indique el motivo de la observacion");
        cambiarEstado(id, "OBSERVADO", motivo);
    }

    /** Rechaza la transferencia con motivo visible para el socio. */
    public void rechazar(Long id, String motivo) {
        autorizacion.exigir(PANTALLA, "INACTIVAR");
        if (motivo == null || motivo.isBlank()) throw new NegocioException("Indique el motivo del rechazo");
        cambiarEstado(id, "RECHAZADO", motivo);
    }

    private void cambiarEstado(Long id, String estado, String motivo) {
        int n = em.createNativeQuery(
            "UPDATE portal_pago_transferencia SET estado = :e, motivo_revision = :m, usuario_revision = :u,"
          + " fecha_revision = now() WHERE portal_pago_transferencia = :id AND estado NOT IN ('APLICADO','RECHAZADO')")
            .setParameter("e", estado).setParameter("m", recorta(motivo, 300))
            .setParameter("u", sesion.codigoUsuario()).setParameter("id", id).executeUpdate();
        if (n == 0) throw new NegocioException("La transferencia no existe o ya fue cerrada");
        auditar(id, AuditoriaFuncionalService.EDITAR, estado + (motivo == null ? "" : ": " + motivo));
    }

    /**
     * Aprueba y APLICA la transferencia: genera el cobro reutilizando el motor de caja (forma TRANSFERENCIA)
     * contra el documento indicado y la planilla abierta. Marca APLICADO y guarda el cobro generado.
     */
    /** Forma de pago TRANSFERENCIA (codigo TRF) del catalogo de formas de pago. */
    public Long idFormaTransferencia() {
        try {
            Object r = em.createNativeQuery("SELECT forma_pago FROM forma_pago WHERE codigo = 'TRF' LIMIT 1").getSingleResult();
            return r == null ? null : ((Number) r).longValue();
        } catch (RuntimeException e) { return null; }
    }

    public void aprobar(Long id, Long documentoId, Long planillaId, Long formaPagoId, String emisor, Long monedaId) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (documentoId == null) throw new NegocioException("Elija el documento/cuota a imputar");
        if (planillaId == null) throw new NegocioException("No hay una caja abierta para aplicar el cobro");
        if (formaPagoId == null) formaPagoId = idFormaTransferencia();

        // obs 304: reclamar la fila ATOMICAMENTE antes de cobrar. El UPDATE toma el lock de escritura de la fila
        // (retenido hasta el commit): un segundo request concurrente se bloquea y, al liberarse, ya no cumple el
        // WHERE (estado APLICADO) -> 0 filas -> falla, evitando el doble cobro sobre la misma transferencia.
        @SuppressWarnings("unchecked")
        List<Object[]> claim = em.createNativeQuery(
            "UPDATE portal_pago_transferencia SET estado='EN_REVISION'"
          + " WHERE portal_pago_transferencia = :id AND estado IN ('RECIBIDO','EN_REVISION','OBSERVADO')"
          + " RETURNING persona, importe, numero_transaccion, cuenta_origen").setParameter("id", id).getResultList();
        if (claim.isEmpty()) throw new NegocioException("La transferencia ya fue cerrada o esta siendo procesada");
        Object[] t = claim.get(0);
        Long persona = ((Number) t[0]).longValue();
        BigDecimal importe = (BigDecimal) t[1];
        String numero = (String) t[2];
        String cuentaOrigen = (String) t[3];

        // Aplica el cobro con el motor de caja (forma TRANSFERENCIA: cuenta = cuenta origen, referencia = nro transaccion).
        long cobroId = cajaService.cobrar(documentoId, planillaId, formaPagoId, persona, importe, monedaId,
                sesion.codigoUsuario(), emisor, null, null, null,
                cuentaOrigen, null, numero, null, null, null, null, null, null);

        int n = em.createNativeQuery(
            "UPDATE portal_pago_transferencia SET estado='APLICADO', cobro = :c, documento = :doc,"
          + " usuario_revision = :u, fecha_revision = now()"
          + " WHERE portal_pago_transferencia = :id AND estado = 'EN_REVISION'")
            .setParameter("c", cobroId).setParameter("doc", documentoId)
            .setParameter("u", sesion.codigoUsuario()).setParameter("id", id).executeUpdate();
        if (n == 0) throw new NegocioException("La transferencia cambio de estado durante la aplicacion");
        auditar(id, AuditoriaFuncionalService.EDITAR, "APLICADO (cobro " + cobroId + ", doc " + documentoId + ")");
    }

    /** Descarga del comprobante (bandeja interna, con permiso; o el propio socio por su persona). */
    public Descarga descargar(Long id, Long personaPropia) {
        Object[] r;
        try {
            r = (Object[]) em.createNativeQuery(
                "SELECT archivo_nombre, archivo_fisico, archivo_mime, tenant, persona FROM portal_pago_transferencia"
              + " WHERE portal_pago_transferencia = :id").setParameter("id", id).getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            throw new NegocioException("El comprobante no existe");
        }
        Long persona = ((Number) r[4]).longValue();
        boolean esPropia = personaPropia != null && personaPropia.equals(persona);
        if (!esPropia) autorizacion.exigir(PANTALLA, "VER");
        String nombre = (String) r[0];
        String fisico = (String) r[1];
        String mime = r[2] == null ? "application/octet-stream" : (String) r[2];
        Long t = ((Number) r[3]).longValue();
        if (fisico == null) throw new NegocioException("La transferencia no tiene comprobante");
        try {
            byte[] datos = Files.readAllBytes(baseDir().resolve(String.valueOf(t)).resolve(fisico));
            return new Descarga(nombre, mime, datos);
        } catch (Exception e) {
            throw new NegocioException("No se pudo leer el comprobante");
        }
    }

    // ── Conciliacion bancaria (REQ-0085 Fase 3) ────────────────────────────────

    /** Registra manualmente un movimiento/aviso bancario. */
    public void registrarMovimiento(Mov m) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (m == null || m.importe == null || m.importe.signum() <= 0) throw new NegocioException("Indique el importe del movimiento");
        em.createNativeQuery(
            "INSERT INTO movimiento_bancario_importado (tenant, fuente, banco, cuenta, fecha, importe, moneda,"
          + " referencia, remitente, hash_externo, usuario_carga)"
          + " VALUES (:t,'MANUAL',:b,:c,:f,:imp,:mon,:ref,:rem,:h,:u)")
            .setParameter("t", tenant.actual()).setParameter("b", recorta(m.banco, 80)).setParameter("c", recorta(m.cuenta, 40))
            .setParameter("f", m.fecha == null ? null : java.sql.Date.valueOf(m.fecha)).setParameter("imp", m.importe)
            .setParameter("mon", m.moneda).setParameter("ref", recorta(m.referencia, 80)).setParameter("rem", recorta(m.remitente, 120))
            .setParameter("h", m.referencia == null ? null : ("MAN:" + m.referencia + ":" + m.importe))
            .setParameter("u", sesion.codigoUsuario()).executeUpdate();
    }

    /** Importa movimientos desde un archivo CSV: banco;cuenta;fecha(dd/MM/yyyy);importe;referencia;remitente. */
    public int importarCsv(byte[] datos) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (datos == null || datos.length == 0) throw new NegocioException("Archivo vacio");
        int n = 0;
        String contenido = new String(datos, java.nio.charset.StandardCharsets.UTF_8);
        for (String linea : contenido.split("\\r?\\n")) {
            if (linea.isBlank() || linea.toLowerCase().startsWith("banco;")) continue;
            String[] c = linea.split(";", -1);
            if (c.length < 4) continue;
            try {
                java.time.LocalDate fecha = c[2].isBlank() ? null
                        : java.time.LocalDate.parse(c[2].trim(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                BigDecimal imp = new BigDecimal(c[3].replace(".", "").replace(",", ".").trim());
                String ref = c.length > 4 ? c[4].trim() : null;
                String rem = c.length > 5 ? c[5].trim() : null;
                String hash = "CSV:" + (ref == null || ref.isBlank() ? (c[0] + c[2] + c[3]) : ref);
                em.createNativeQuery(
                    "INSERT INTO movimiento_bancario_importado (tenant, fuente, banco, cuenta, fecha, importe,"
                  + " referencia, remitente, hash_externo, usuario_carga)"
                  + " VALUES (:t,'ARCHIVO',:b,:cu,:f,:imp,:ref,:rem,:h,:u)"
                  + " ON CONFLICT (tenant, hash_externo) WHERE hash_externo IS NOT NULL DO NOTHING")
                    .setParameter("t", tenant.actual()).setParameter("b", recorta(c[0].trim(), 80))
                    .setParameter("cu", recorta(c[1].trim(), 40)).setParameter("f", fecha == null ? null : java.sql.Date.valueOf(fecha))
                    .setParameter("imp", imp).setParameter("ref", recorta(ref, 80)).setParameter("rem", recorta(rem, 120))
                    .setParameter("h", recorta(hash, 120)).setParameter("u", sesion.codigoUsuario()).executeUpdate();
                n++;
            } catch (RuntimeException ignore) { /* linea invalida: se omite */ }
        }
        return n;
    }

    public List<Mov> movimientos(String estado) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Mov> out = new ArrayList<>();
        String cond = (estado != null && !estado.isBlank()) ? " AND estado_conciliacion = :e" : "";
        var q = em.createNativeQuery(
            "SELECT movimiento_bancario_importado, banco, cuenta, fecha, importe, referencia, remitente,"
          + " estado_conciliacion, transferencia FROM movimiento_bancario_importado WHERE 1=1" + cond
          + " ORDER BY fecha DESC NULLS LAST, movimiento_bancario_importado DESC");
        if (!cond.isEmpty()) q.setParameter("e", estado);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) out.add(mov(f));
        return out;
    }

    /** Movimientos PENDIENTES candidatos a conciliar con una transferencia (por importe + fecha con tolerancia). */
    public List<Mov> candidatos(Long transferenciaId) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Mov> out = new ArrayList<>();
        Object[] t;
        try {
            t = (Object[]) em.createNativeQuery(
                "SELECT importe, fecha_transferencia, banco_origen, numero_transaccion FROM portal_pago_transferencia"
              + " WHERE portal_pago_transferencia = :id").setParameter("id", transferenciaId).getSingleResult();
        } catch (jakarta.persistence.NoResultException e) { return out; }
        BigDecimal importe = (BigDecimal) t[0];
        LocalDate fecha = aLocalDate(t[1]);
        String numero = (String) t[3];
        int tol = Math.max(0, parametros.entero("PORTAL_TRANSF_TOLERANCIA_DIAS", 2));
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT movimiento_bancario_importado, banco, cuenta, fecha, importe, referencia, remitente,"
          + " estado_conciliacion, transferencia FROM movimiento_bancario_importado"
          + " WHERE estado_conciliacion = 'PENDIENTE' AND importe = :imp"
          + " AND (:fec IS NULL OR fecha IS NULL OR abs(fecha - :fec) <= :tol)"
          + " AND (:num IS NULL OR referencia IS NULL OR referencia = :num)"
          + " ORDER BY fecha DESC NULLS LAST")
            .setParameter("imp", importe).setParameter("fec", fecha == null ? null : java.sql.Date.valueOf(fecha)).setParameter("tol", tol)
            .setParameter("num", numero).getResultList();
        for (Object[] f : rows) out.add(mov(f));
        return out;
    }

    /**
     * Concilia la transferencia con un movimiento bancario CONFIRMADO y aplica el pago (reutiliza aprobar()).
     * Anti-doble: el movimiento debe estar PENDIENTE; queda CONCILIADO y no se reutiliza.
     */
    public void conciliarYAplicar(Long transferenciaId, Long movimientoId, Long documentoId, Long planillaId,
                                  String emisor, Long monedaId) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        int tol = Math.max(0, parametros.entero("PORTAL_TRANSF_TOLERANCIA_DIAS", 2));
        // obs 306: validar ATOMICAMENTE en el backend que el movimiento es candidato REAL de la transferencia
        // (mismo importe + fecha con tolerancia + referencia/numero si ambos existen) antes de marcar CONCILIADO.
        // El UPDATE ... FROM con RETURNING hace el match+claim en una sola operacion; 0 filas -> no corresponde.
        @SuppressWarnings("unchecked")
        List<Object[]> ok = em.createNativeQuery(
            "UPDATE movimiento_bancario_importado m SET estado_conciliacion='CONCILIADO', transferencia = t.portal_pago_transferencia"
          + " FROM portal_pago_transferencia t"
          + " WHERE m.movimiento_bancario_importado = :m AND m.estado_conciliacion = 'PENDIENTE'"
          + "   AND t.portal_pago_transferencia = :tr AND t.estado IN ('RECIBIDO','EN_REVISION','OBSERVADO')"
          + "   AND m.importe = t.importe"
          + "   AND (t.fecha_transferencia IS NULL OR m.fecha IS NULL OR abs(m.fecha - t.fecha_transferencia) <= :tol)"
          + "   AND (t.numero_transaccion IS NULL OR m.referencia IS NULL OR m.referencia = t.numero_transaccion)"
          + " RETURNING m.movimiento_bancario_importado")
            .setParameter("m", movimientoId).setParameter("tr", transferenciaId).setParameter("tol", tol)
            .getResultList();
        if (ok.isEmpty())
            throw new NegocioException("El movimiento no corresponde a la transferencia (importe/fecha/referencia) o ya fue conciliado");
        aprobar(transferenciaId, documentoId, planillaId, null, emisor, monedaId);
    }

    private Mov mov(Object[] f) {
        Mov m = new Mov();
        m.id = ((Number) f[0]).longValue();
        m.banco = (String) f[1]; m.cuenta = (String) f[2];
        m.fecha = aLocalDate(f[3]);
        m.importe = (BigDecimal) f[4]; m.referencia = (String) f[5]; m.remitente = (String) f[6];
        m.estado = (String) f[7]; m.transferencia = f[8] == null ? null : ((Number) f[8]).longValue();
        return m;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Una columna date puede llegar como LocalDate (Hibernate 7) o java.sql.Date segun el driver. */
    private static LocalDate aLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (o instanceof java.time.LocalDateTime l) return l.toLocalDate();
        if (o instanceof java.time.OffsetDateTime ofs) return ofs.toLocalDate();
        return LocalDate.parse(o.toString());
    }

    private Fila fila(Object[] f) {
        Fila x = new Fila();
        x.id = ((Number) f[0]).longValue();
        x.fecha = f[1] instanceof java.sql.Timestamp ts ? ts.toLocalDateTime().toLocalDate()
                : (f[1] instanceof java.time.LocalDateTime l ? l.toLocalDate() : (f[1] instanceof java.time.OffsetDateTime o ? o.toLocalDate() : null));
        x.importe = (BigDecimal) f[2];
        x.estado = (String) f[3];
        x.numeroTransaccion = (String) f[4];
        x.motivoRevision = (String) f[5];
        x.cobro = f[6] == null ? null : ((Number) f[6]).longValue();
        return x;
    }

    private void auditar(Long id, String accion, String detalle) {
        try { auditoria.registrar("portal_pago_transferencia", id, accion, PANTALLA, detalle); }
        catch (RuntimeException ignore) { }
    }

    /** obs 305: tipo real por firma/magic bytes; devuelve la extension canonica o null si no es permitido. */
    private static String firmaContenido(byte[] d) {
        if (d == null || d.length < 12) return null;
        int b0 = d[0] & 0xFF, b1 = d[1] & 0xFF, b2 = d[2] & 0xFF, b3 = d[3] & 0xFF;
        if (b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46) return ".pdf";           // %PDF
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return ".jpg";                          // JPEG
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return ".png";            // PNG
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46                            // RIFF....WEBP
                && (d[8] & 0xFF) == 0x57 && (d[9] & 0xFF) == 0x45 && (d[10] & 0xFF) == 0x42 && (d[11] & 0xFF) == 0x50) return ".webp";
        return null;
    }

    private static String extensionValida(String nombre, String mime) {
        String n = nombre == null ? "" : nombre.toLowerCase();
        String m = mime == null ? "" : mime.toLowerCase();
        if (n.endsWith(".pdf") || m.contains("pdf")) return ".pdf";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || m.contains("jpeg")) return ".jpg";
        if (n.endsWith(".png") || m.contains("png")) return ".png";
        if (n.endsWith(".webp") || m.contains("webp")) return ".webp";
        return null;
    }

    private static String sha256(byte[] datos) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(datos);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private static String recorta(String s, int max) { return s == null ? null : (s.length() <= max ? s : s.substring(0, max)); }

    // ── DTOs ──
    public static class Datos {
        public BigDecimal importe; public Long moneda; public LocalDate fecha;
        public String bancoOrigen, cuentaOrigen, cuentaDestino, numeroTransaccion, observacion; public Long documento;
    }
    public static class Fila {
        public Long id, cobro, persona, documento, moneda;
        public LocalDate fecha; public BigDecimal importe;
        public String estado, numeroTransaccion, motivoRevision, cliente, bancoOrigen, cuentaOrigen;
        // REQ-0084: datos extraidos por OCR (insumo para la revision).
        public String textoOcr, ocrNumero, ocrBanco, ocrMotor;
        public BigDecimal ocrImporte, confianzaOcr;
        public LocalDate ocrFecha;
        public String getTextoOcr() { return textoOcr; }
        public String getOcrNumero() { return ocrNumero; }
        public String getOcrBanco() { return ocrBanco; }
        public String getOcrMotor() { return ocrMotor; }
        public BigDecimal getOcrImporte() { return ocrImporte; }
        public BigDecimal getConfianzaOcr() { return confianzaOcr; }
        public LocalDate getOcrFecha() { return ocrFecha; }
        public boolean isTieneOcr() { return textoOcr != null && !textoOcr.isBlank(); }
        public Long getId() { return id; }
        public Long getCobro() { return cobro; }
        public Long getPersona() { return persona; }
        public Long getDocumento() { return documento; }
        public Long getMoneda() { return moneda; }
        public LocalDate getFecha() { return fecha; }
        public BigDecimal getImporte() { return importe; }
        public String getEstado() { return estado; }
        public String getNumeroTransaccion() { return numeroTransaccion; }
        public String getMotivoRevision() { return motivoRevision; }
        public String getCliente() { return cliente; }
        public String getBancoOrigen() { return bancoOrigen; }
        public String getCuentaOrigen() { return cuentaOrigen; }
    }
    public static class Descarga {
        public final String nombre, contentType; public final byte[] datos;
        public Descarga(String nombre, String contentType, byte[] datos) { this.nombre = nombre; this.contentType = contentType; this.datos = datos; }
    }
    /** REQ-0085: movimiento bancario importado (para carga y conciliacion). */
    public static class Mov {
        public Long id, moneda, transferencia;
        public String banco, cuenta, referencia, remitente, estado;
        public LocalDate fecha; public BigDecimal importe;
        public Long getId() { return id; }
        public String getBanco() { return banco; }
        public String getCuenta() { return cuenta; }
        public String getReferencia() { return referencia; }
        public String getRemitente() { return remitente; }
        public String getEstado() { return estado; }
        public LocalDate getFecha() { return fecha; }
        public BigDecimal getImporte() { return importe; }
        public Long getTransferencia() { return transferencia; }
        public void setBanco(String v) { banco = v; }
        public void setCuenta(String v) { cuenta = v; }
        public void setReferencia(String v) { referencia = v; }
        public void setRemitente(String v) { remitente = v; }
        public void setFecha(LocalDate v) { fecha = v; }
        public void setImporte(BigDecimal v) { importe = v; }
        public void setMoneda(Long v) { moneda = v; }
    }
}
