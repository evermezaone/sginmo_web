package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REQ-0110 - Portal: reclamos del cliente con fotos de su unidad (comprada o alquilada).
 * El cliente abre un reclamo y adjunta fotos; queda en bandeja interna donde un operador responde
 * y cambia el estado. Almacenamiento controlado fuera del webroot (igual que el comprobante). @AislarTenant + RLS.
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class ReclamoService {

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
    private ParametroConfig parametros;
    @Inject
    private PersonaService personaService;

    public static final String PANTALLA = "reclamos";

    private Path baseDir() {
        String dir = System.getenv("SGINMO_ARCHIVOS_DIR");
        if (dir == null || dir.isBlank()) dir = System.getProperty("user.home", ".") + "/sginmo/archivos";
        return Path.of(dir);
    }

    // ── Portal (cliente) ────────────────────────────────────────────────────────

    /** El cliente abre un reclamo y adjunta fotos. Devuelve el id creado. */
    public Long crear(Long persona, Datos d, List<Adjunto> fotos) {
        if (persona == null) throw new NegocioException("Sesion invalida");
        if (d == null || d.titulo == null || d.titulo.isBlank()) throw new NegocioException("Ingrese un titulo para el reclamo");
        if (d.descripcion == null || d.descripcion.isBlank()) throw new NegocioException("Describa el reclamo");
        Long t = tenant.actual();
        Object id = em.createNativeQuery(
            "INSERT INTO reclamo (tenant, persona, operacion, unidad, tipo, titulo, descripcion, estado, prioridad)"
          + " VALUES (:t,:p,:op,:un,:ti,:tt,:de,'ABIERTO','MEDIA') RETURNING reclamo")
            .setParameter("t", t).setParameter("p", persona).setParameter("op", d.operacion)
            .setParameter("un", recorta(d.unidad, 160)).setParameter("ti", tipoValido(d.tipo))
            .setParameter("tt", recorta(d.titulo, 160)).setParameter("de", recorta(d.descripcion, 2000))
            .getSingleResult();
        Long rid = ((Number) id).longValue();

        int max = Math.max(1, parametros.entero("RECLAMO_MAX_FOTOS", 6));
        int maxMb = Math.max(1, parametros.entero("RECLAMO_FOTO_TAMANO_MAX_MB", 8));
        int n = 0;
        if (fotos != null) {
            for (Adjunto a : fotos) {
                if (a == null || a.datos == null || a.datos.length == 0) continue;
                if (++n > max) break;
                if (a.datos.length > (long) maxMb * 1024 * 1024)
                    throw new NegocioException("Una foto supera el maximo de " + maxMb + " MB");
                String ext = firmaContenido(a.datos);
                if (ext == null) throw new NegocioException("Las fotos deben ser JPG, PNG, WEBP o PDF validos");
                guardarFoto(rid, t, a, ext, "CLIENTE");
            }
        }
        auditar(rid, AuditoriaFuncionalService.CREAR, "reclamo creado por el cliente");
        return rid;
    }

    private void guardarFoto(Long rid, Long t, Adjunto a, String ext, String origen) {
        String fisico = "rec_" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = baseDir().resolve(String.valueOf(t));
            Files.createDirectories(dir);
            Files.write(dir.resolve(fisico), a.datos);
        } catch (Exception e) {
            throw new NegocioException("No se pudo guardar la foto");
        }
        em.createNativeQuery(
            "INSERT INTO reclamo_foto (tenant, reclamo, archivo_nombre, archivo_fisico, archivo_mime, archivo_hash, archivo_tamano, origen)"
          + " VALUES (:t,:r,:an,:af,:am,:ah,:at,:og)")
            .setParameter("t", t).setParameter("r", rid).setParameter("an", recorta(a.nombre, 255))
            .setParameter("af", fisico).setParameter("am", mimeDe(ext)).setParameter("ah", sha256(a.datos))
            .setParameter("at", (long) a.datos.length).setParameter("og", origen).executeUpdate();
    }

    /** Reclamos del cliente autenticado (para el portal), con sus fotos. */
    @SuppressWarnings("unchecked")
    public List<Fila> mios(Long persona) {
        List<Fila> out = new ArrayList<>();
        if (persona == null) return out;
        List<Object[]> rows = em.createNativeQuery(
            "SELECT reclamo, tipo, titulo, descripcion, estado, prioridad, respuesta, creado_en, unidad"
          + " FROM reclamo WHERE persona = :p ORDER BY creado_en DESC")
            .setParameter("p", persona).getResultList();
        for (Object[] r : rows) out.add(fila(r));
        for (Fila f : out) f.fotos = fotosDe(f.id);
        return out;
    }

    /** Unidades (operaciones) del cliente autenticado, por su identidad. Para elegir a cuál refiere el reclamo. */
    @SuppressWarnings("unchecked")
    public List<Unidad> unidadesDe(Long persona) {
        List<Unidad> out = new ArrayList<>();
        if (persona == null) return out;
        List<Object[]> rows = em.createNativeQuery(
            "SELECT o.operacion, a.nombre, o.tipo_operacion FROM operacion o JOIN activo a ON a.activo = o.activo"
          + " WHERE o.cliente = :p ORDER BY o.fecha_operacion DESC")
            .setParameter("p", persona).getResultList();
        for (Object[] r : rows) {
            Unidad u = new Unidad();
            u.operacion = ((Number) r[0]).longValue(); u.nombre = (String) r[1]; u.tipo = (String) r[2];
            out.add(u);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<Foto> fotosDe(Long reclamo) {
        List<Foto> out = new ArrayList<>();
        List<Object[]> rows = em.createNativeQuery(
            "SELECT reclamo_foto, archivo_nombre, archivo_mime, origen FROM reclamo_foto WHERE reclamo = :r ORDER BY reclamo_foto")
            .setParameter("r", reclamo).getResultList();
        for (Object[] r : rows) {
            Foto x = new Foto();
            x.id = ((Number) r[0]).longValue(); x.nombre = (String) r[1]; x.mime = (String) r[2];
            x.origen = (String) r[3];
            out.add(x);
        }
        return out;
    }

    /** Descarga de una foto (el propio cliente por su persona, o interno con permiso). */
    public Descarga descargarFoto(Long fotoId, Long personaPropia) {
        Object[] r;
        try {
            r = (Object[]) em.createNativeQuery(
                "SELECT f.archivo_nombre, f.archivo_fisico, f.archivo_mime, f.tenant, rec.persona"
              + " FROM reclamo_foto f JOIN reclamo rec ON rec.reclamo = f.reclamo WHERE f.reclamo_foto = :id")
                .setParameter("id", fotoId).getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            throw new NegocioException("La foto no existe");
        }
        Long persona = ((Number) r[4]).longValue();
        boolean propia = personaPropia != null && personaPropia.equals(persona);
        if (!propia) autorizacion.exigir(PANTALLA, "VER");
        String nombre = (String) r[0];
        String fisico = (String) r[1];
        String mime = r[2] == null ? "application/octet-stream" : (String) r[2];
        Long t = ((Number) r[3]).longValue();
        if (fisico == null) throw new NegocioException("La foto no tiene archivo");
        try {
            byte[] datos = Files.readAllBytes(baseDir().resolve(String.valueOf(t)).resolve(fisico));
            return new Descarga(nombre, mime, datos);
        } catch (Exception e) {
            throw new NegocioException("No se pudo leer la foto");
        }
    }

    // ── Bandeja (interno) ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Fila> bandeja(String estado) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Fila> out = new ArrayList<>();
        String cond = (estado != null && !estado.isBlank()) ? " AND r.estado = :e" : "";
        var q = em.createNativeQuery(
            "SELECT r.reclamo, r.tipo, r.titulo, r.descripcion, r.estado, r.prioridad, r.respuesta, r.creado_en, r.unidad, p.nombre,"
          + " r.asignado, r.fecha_programada, r.proveedor_nombre, r.cotizacion, r.orden_pago, r.proveedor, rop.estado"
          + " FROM reclamo r LEFT JOIN persona p ON p.persona = r.persona"
          + " LEFT JOIN reclamo_orden_pago rop ON rop.reclamo_orden_pago = r.orden_pago WHERE 1=1" + cond
          + " ORDER BY CASE r.estado WHEN 'ABIERTO' THEN 1 WHEN 'EN_PROCESO' THEN 2 WHEN 'RESUELTO' THEN 3 ELSE 4 END,"
          + " CASE r.prioridad WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END, r.creado_en DESC");
        if (!cond.isEmpty()) q.setParameter("e", estado);
        List<Object[]> rows = q.getResultList();
        for (Object[] r : rows) {
            Fila x = fila(r);
            x.cliente = (String) r[9];
            x.asignado = (String) r[10];
            x.fechaProgramada = aLocalDate(r[11]);
            x.proveedorNombre = (String) r[12];
            x.cotizacion = (java.math.BigDecimal) r[13];
            x.ordenPago = r[14] == null ? null : ((Number) r[14]).longValue();
            x.proveedor = r[15] == null ? null : ((Number) r[15]).longValue();
            x.ordenEstado = (String) r[16];
            x.fotos = fotosDe(x.id);
            out.add(x);
        }
        return out;
    }

    private static java.time.LocalDate aLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof java.time.LocalDate ld) return ld;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (o instanceof java.time.LocalDateTime l) return l.toLocalDate();
        if (o instanceof java.time.OffsetDateTime ofs) return ofs.toLocalDate();
        return null;
    }

    /** El operador responde y cambia el estado del reclamo. */
    public void responder(Long id, String estado, String respuesta) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (estado == null || estado.isBlank()) throw new NegocioException("Indique el estado del reclamo");
        int n = em.createNativeQuery(
            "UPDATE reclamo SET estado = :e, respuesta = :r, usuario_revision = :u, fecha_revision = now() WHERE reclamo = :id")
            .setParameter("e", estado).setParameter("r", recorta(respuesta, 2000))
            .setParameter("u", sesion.codigoUsuario()).setParameter("id", id).executeUpdate();
        if (n > 0) auditar(id, AuditoriaFuncionalService.EDITAR, estado + (respuesta == null ? "" : ": " + respuesta));
    }

    // ── Workflow de gestion (interno) ────────────────────────────────────────────

    public void priorizar(Long id, String prioridad) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        String p = prioridadValida(prioridad);
        em.createNativeQuery("UPDATE reclamo SET prioridad = :p WHERE reclamo = :id")
            .setParameter("p", p).setParameter("id", id).executeUpdate();
        seg(id, "NOTA", "Prioridad: " + p);
        auditar(id, AuditoriaFuncionalService.EDITAR, "prioridad " + p);
    }

    public void asignar(Long id, String asignado) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        em.createNativeQuery("UPDATE reclamo SET asignado = :a, estado = CASE WHEN estado = 'ABIERTO' THEN 'EN_PROCESO' ELSE estado END WHERE reclamo = :id")
            .setParameter("a", recorta(asignado, 60)).setParameter("id", id).executeUpdate();
        seg(id, "ASIGNACION", "Asignado a " + asignado);
        auditar(id, AuditoriaFuncionalService.EDITAR, "asignado a " + asignado);
    }

    public void programar(Long id, java.time.LocalDate fecha) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        em.createNativeQuery("UPDATE reclamo SET fecha_programada = :f, estado = CASE WHEN estado = 'ABIERTO' THEN 'EN_PROCESO' ELSE estado END WHERE reclamo = :id")
            .setParameter("f", fecha == null ? null : java.sql.Date.valueOf(fecha)).setParameter("id", id).executeUpdate();
        seg(id, "PROGRAMACION", "Programado para " + fecha);
        auditar(id, AuditoriaFuncionalService.EDITAR, "programado " + fecha);
    }

    public void asignarProveedor(Long id, String nombre, Long socio, java.math.BigDecimal cotizacion, Long moneda) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (nombre == null || nombre.isBlank()) throw new NegocioException("Indique el proveedor");
        em.createNativeQuery("UPDATE reclamo SET proveedor_nombre = :n, proveedor = :s, cotizacion = :c, moneda = :m WHERE reclamo = :id")
            .setParameter("n", recorta(nombre, 160)).setParameter("s", socio).setParameter("c", cotizacion)
            .setParameter("m", moneda).setParameter("id", id).executeUpdate();
        seg(id, "PROVEEDOR", "Proveedor " + nombre + (cotizacion != null ? " · cotizacion " + cotizacion : ""));
        auditar(id, AuditoriaFuncionalService.EDITAR, "proveedor " + nombre);
    }

    /**
     * REQ-0113: genera la orden de pago Y crea un egreso REAL en Ingresos/Egresos (por pagar),
     * vinculado al proveedor. El concepto (articulo) es el rubro de gasto elegido por el operador.
     */
    public Long generarOrdenPago(Long id, Long articuloId) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (articuloId == null) throw new NegocioException("Elija el concepto (rubro de gasto) para el egreso");
        Object[] r = (Object[]) em.createNativeQuery(
            "SELECT proveedor_nombre, proveedor, cotizacion, moneda, titulo, orden_pago FROM reclamo WHERE reclamo = :id")
            .setParameter("id", id).getSingleResult();
        java.math.BigDecimal cot = (java.math.BigDecimal) r[2];
        if (cot == null || cot.signum() <= 0) throw new NegocioException("Cargue la cotizacion del proveedor antes de generar la orden de pago");
        if (r[5] != null) throw new NegocioException("El reclamo ya tiene una orden de pago generada");
        Long t = tenant.actual();
        Long proveedorId = r[1] == null ? null : ((Number) r[1]).longValue();

        // Egreso PENDIENTE (por pagar) en Ingresos/Egresos, vinculado al proveedor.
        py.com.pysistemas.sginmo.dominio.operacion.IngresoEgreso ie = new py.com.pysistemas.sginmo.dominio.operacion.IngresoEgreso();
        ie.setFecha(java.time.LocalDate.now());
        ie.setTipo("EGRESO");
        ie.setMonto(cot);
        ie.setSaldo(cot);
        ie.setEstado("PENDIENTE");
        ie.setArticulo(articuloId);
        ie.setPersona(proveedorId);
        ie.setTenant(t);
        ie.setObservacion(recorta("Reclamo #" + id + " - " + r[4], 500));
        em.persist(ie);
        em.flush();
        Long egresoId = ie.getId();

        Object opId = em.createNativeQuery(
            "INSERT INTO reclamo_orden_pago (tenant, reclamo, proveedor_nombre, proveedor, concepto, monto, moneda, usuario, egreso)"
          + " VALUES (:t,:r,:pn,:p,:co,:mo,:cur,:u,:eg) RETURNING reclamo_orden_pago")
            .setParameter("t", t).setParameter("r", id).setParameter("pn", r[0]).setParameter("p", r[1])
            .setParameter("co", recorta("Reclamo #" + id + " - " + r[4], 200)).setParameter("mo", cot)
            .setParameter("cur", r[3]).setParameter("u", usuarioActual()).setParameter("eg", egresoId).getSingleResult();
        Long op = ((Number) opId).longValue();
        em.createNativeQuery("UPDATE reclamo SET orden_pago = :op WHERE reclamo = :id").setParameter("op", op).setParameter("id", id).executeUpdate();
        seg(id, "ORDEN_PAGO", "Orden de pago #" + op + " por " + cot + " (egreso #" + egresoId + " pendiente)");
        auditar(id, AuditoriaFuncionalService.EDITAR, "orden de pago " + op + " egreso " + egresoId + " (" + cot + ")");
        return op;
    }

    /** REQ-0113: ejecuta el pago de la orden: cancela el egreso (saldo 0) y marca la orden PAGADA. */
    public void registrarPago(Long reclamoId) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        var q = em.createNativeQuery(
            "SELECT reclamo_orden_pago, egreso, estado FROM reclamo_orden_pago"
          + " WHERE reclamo = :id AND estado <> 'ANULADA' ORDER BY creado_en DESC")
            .setParameter("id", reclamoId);
        q.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        if (rows.isEmpty()) throw new NegocioException("El reclamo no tiene una orden de pago para pagar");
        Object[] o = rows.get(0);
        Long ordenId = ((Number) o[0]).longValue();
        Long egresoId = o[1] == null ? null : ((Number) o[1]).longValue();
        if ("PAGADA".equals(o[2])) throw new NegocioException("La orden de pago ya fue pagada");
        if (egresoId != null)
            em.createNativeQuery("UPDATE ingreso_egreso SET estado = 'CANCELADO', saldo = 0 WHERE ingreso_egreso = :e")
                .setParameter("e", egresoId).executeUpdate();
        em.createNativeQuery("UPDATE reclamo_orden_pago SET estado = 'PAGADA', pagado_en = now() WHERE reclamo_orden_pago = :o")
            .setParameter("o", ordenId).executeUpdate();
        seg(reclamoId, "PAGO", "Pago registrado de la orden #" + ordenId + (egresoId != null ? " (egreso #" + egresoId + " cancelado)" : ""));
        auditar(reclamoId, AuditoriaFuncionalService.EDITAR, "pago orden " + ordenId);
    }

    /** Proveedores registrados (personas con rol PROVEEDOR) para el selector, con su CI/RUC. */
    public List<Prov> proveedores() {
        autorizacion.exigir(PANTALLA, "VER");
        List<Prov> out = new ArrayList<>();
        for (py.com.pysistemas.sginmo.dominio.persona.Persona p : personaService.porRol("PROVEEDOR")) {
            Prov v = new Prov();
            v.id = p.getId();
            v.nombre = p.getNombre();
            v.documento = p.getNumeroDocumento();
            out.add(v);
        }
        return out;
    }

    /** Conceptos (articulos) disponibles como rubro de gasto del egreso. */
    public List<Concepto> conceptos() {
        autorizacion.exigir(PANTALLA, "VER");
        List<Concepto> out = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT articulo, descripcion FROM articulo WHERE (tenant = :t OR tenant IS NULL) ORDER BY descripcion")
            .setParameter("t", tenant.actual()).getResultList();
        for (Object[] r : rows) {
            Concepto c = new Concepto();
            c.id = ((Number) r[0]).longValue();
            c.descripcion = (String) r[1];
            out.add(c);
        }
        return out;
    }

    public void agregarEvidencia(Long id, List<Adjunto> fotos) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        Long t = tenant.actual();
        int maxMb = Math.max(1, parametros.entero("RECLAMO_FOTO_TAMANO_MAX_MB", 8));
        int n = 0;
        if (fotos != null) {
            for (Adjunto a : fotos) {
                if (a == null || a.datos == null || a.datos.length == 0) continue;
                if (a.datos.length > (long) maxMb * 1024 * 1024) throw new NegocioException("Una foto supera el maximo de " + maxMb + " MB");
                String ext = firmaContenido(a.datos);
                if (ext == null) throw new NegocioException("Las fotos deben ser JPG, PNG, WEBP o PDF validos");
                guardarFoto(id, t, a, ext, "SOLUCION");
                n++;
            }
        }
        if (n > 0) { seg(id, "EVIDENCIA", n + " foto(s) de evidencia de la solucion"); auditar(id, AuditoriaFuncionalService.EDITAR, "evidencia +" + n); }
    }

    public void resolver(Long id, String respuesta) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        em.createNativeQuery("UPDATE reclamo SET estado = 'RESUELTO', respuesta = :r, usuario_revision = :u, fecha_revision = now() WHERE reclamo = :id")
            .setParameter("r", recorta(respuesta, 2000)).setParameter("u", usuarioActual()).setParameter("id", id).executeUpdate();
        seg(id, "ESTADO", "Resuelto");
        auditar(id, AuditoriaFuncionalService.EDITAR, "RESUELTO");
    }

    public void cerrar(Long id) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        em.createNativeQuery("UPDATE reclamo SET estado = 'CERRADO', cerrado_en = now() WHERE reclamo = :id").setParameter("id", id).executeUpdate();
        seg(id, "ESTADO", "Cerrado");
        auditar(id, AuditoriaFuncionalService.EDITAR, "CERRADO");
    }

    public void rechazar(Long id, String motivo) {
        autorizacion.exigir(PANTALLA, "INACTIVAR");
        if (motivo == null || motivo.isBlank()) throw new NegocioException("Indique el motivo del rechazo");
        em.createNativeQuery("UPDATE reclamo SET estado = 'RECHAZADO', respuesta = :r, usuario_revision = :u, fecha_revision = now() WHERE reclamo = :id")
            .setParameter("r", recorta(motivo, 2000)).setParameter("u", usuarioActual()).setParameter("id", id).executeUpdate();
        seg(id, "ESTADO", "Rechazado: " + motivo);
        auditar(id, AuditoriaFuncionalService.ANULAR, "RECHAZADO: " + motivo);
    }

    public void nota(Long id, String mensaje) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (mensaje != null && !mensaje.isBlank()) seg(id, "NOTA", mensaje);
    }

    @SuppressWarnings("unchecked")
    public List<Seg> seguimientoDe(Long reclamo) {
        List<Seg> out = new ArrayList<>();
        List<Object[]> rows = em.createNativeQuery(
            "SELECT tipo, mensaje, usuario, creado_en FROM reclamo_seguimiento WHERE reclamo = :r ORDER BY creado_en DESC")
            .setParameter("r", reclamo).getResultList();
        for (Object[] r : rows) {
            Seg s = new Seg();
            s.tipo = (String) r[0]; s.mensaje = (String) r[1]; s.usuario = (String) r[2];
            s.creado = r[3] instanceof java.sql.Timestamp ts ? ts.toLocalDateTime()
                     : (r[3] instanceof LocalDateTime l ? l : (r[3] instanceof java.time.OffsetDateTime o ? o.toLocalDateTime() : null));
            out.add(s);
        }
        return out;
    }

    public Dash dashboard() {
        autorizacion.exigir(PANTALLA, "VER");
        Dash d = new Dash();
        @SuppressWarnings("unchecked")
        List<Object[]> e = em.createNativeQuery("SELECT estado, count(*) FROM reclamo GROUP BY estado").getResultList();
        for (Object[] r : e) {
            String st = (String) r[0]; long c = ((Number) r[1]).longValue(); d.total += c;
            switch (st == null ? "" : st) {
                case "ABIERTO": d.abiertos = c; break;
                case "EN_PROCESO": d.enProceso = c; break;
                case "RESUELTO": d.resueltos = c; break;
                case "CERRADO": d.cerrados = c; break;
                case "RECHAZADO": d.rechazados = c; break;
                default: break;
            }
        }
        try { d.altaPrioridad = ((Number) em.createNativeQuery(
            "SELECT count(*) FROM reclamo WHERE prioridad = 'ALTA' AND estado IN ('ABIERTO','EN_PROCESO')").getSingleResult()).longValue(); } catch (RuntimeException ig) { }
        try { d.ordenes = ((Number) em.createNativeQuery(
            "SELECT count(*) FROM reclamo_orden_pago WHERE estado <> 'ANULADA'").getSingleResult()).longValue(); } catch (RuntimeException ig) { }
        try { d.montoOrdenes = (java.math.BigDecimal) em.createNativeQuery(
            "SELECT COALESCE(SUM(monto),0) FROM reclamo_orden_pago WHERE estado <> 'ANULADA'").getSingleResult(); } catch (RuntimeException ig) { }
        return d;
    }

    private void seg(Long reclamo, String tipo, String mensaje) {
        try {
            em.createNativeQuery("INSERT INTO reclamo_seguimiento (tenant, reclamo, tipo, mensaje, usuario) VALUES (:t,:r,:ti,:m,:u)")
                .setParameter("t", tenant.actual()).setParameter("r", reclamo).setParameter("ti", tipo)
                .setParameter("m", recorta(mensaje, 600)).setParameter("u", usuarioActual()).executeUpdate();
        } catch (RuntimeException ignore) { }
    }
    private String usuarioActual() { try { return sesion.codigoUsuario(); } catch (RuntimeException e) { return null; } }
    private static String prioridadValida(String p) {
        String u = p == null ? "" : p.toUpperCase();
        return Set.of("BAJA", "MEDIA", "ALTA").contains(u) ? u : "MEDIA";
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Fila fila(Object[] r) {
        Fila x = new Fila();
        x.id = ((Number) r[0]).longValue();
        x.tipo = (String) r[1]; x.titulo = (String) r[2]; x.descripcion = (String) r[3];
        x.estado = (String) r[4]; x.prioridad = (String) r[5]; x.respuesta = (String) r[6];
        x.creado = r[7] instanceof java.sql.Timestamp ts ? ts.toLocalDateTime()
                 : (r[7] instanceof LocalDateTime l ? l : (r[7] instanceof java.time.OffsetDateTime o ? o.toLocalDateTime() : null));
        x.unidad = (String) r[8];
        return x;
    }

    private void auditar(Long id, String accion, String detalle) {
        try { auditoria.registrar("reclamo", id, accion, PANTALLA, detalle); }
        catch (RuntimeException ignore) { }
    }

    private static String tipoValido(String t) {
        String u = t == null ? "" : t.toUpperCase();
        return Set.of("MANTENIMIENTO", "DANO", "SERVICIO", "ADMINISTRATIVO", "OTRO").contains(u) ? u : "OTRO";
    }

    /** tipo real por firma/magic bytes; devuelve la extension canonica o null si no es permitido. */
    private static String firmaContenido(byte[] d) {
        if (d == null || d.length < 12) return null;
        int b0 = d[0] & 0xFF, b1 = d[1] & 0xFF, b2 = d[2] & 0xFF, b3 = d[3] & 0xFF;
        if (b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46) return ".pdf";
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return ".jpg";
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return ".png";
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                && (d[8] & 0xFF) == 0x57 && (d[9] & 0xFF) == 0x45 && (d[10] & 0xFF) == 0x42 && (d[11] & 0xFF) == 0x50) return ".webp";
        return null;
    }

    private static String mimeDe(String ext) {
        if (".pdf".equals(ext)) return "application/pdf";
        if (".jpg".equals(ext)) return "image/jpeg";
        if (".png".equals(ext)) return "image/png";
        if (".webp".equals(ext)) return "image/webp";
        return "application/octet-stream";
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
        public Long operacion;
        public String unidad, tipo, titulo, descripcion;
    }
    public static class Adjunto {
        public byte[] datos; public String nombre, mime;
        public Adjunto(byte[] datos, String nombre, String mime) { this.datos = datos; this.nombre = nombre; this.mime = mime; }
    }
    public static class Fila {
        public Long id;
        public String tipo, titulo, descripcion, estado, prioridad, respuesta, cliente, unidad, asignado, proveedorNombre;
        public java.time.LocalDate fechaProgramada;
        public java.math.BigDecimal cotizacion;
        public Long ordenPago;
        public Long proveedor;              // id de la persona-proveedor vinculada (REQ-0113)
        public String ordenEstado;          // estado de la orden de pago: GENERADA | PAGADA | ANULADA
        public LocalDateTime creado;
        public List<Foto> fotos = new ArrayList<>();
        public String getAsignado() { return asignado; }
        public String getProveedorNombre() { return proveedorNombre; }
        public Long getProveedor() { return proveedor; }
        public java.time.LocalDate getFechaProgramada() { return fechaProgramada; }
        public java.math.BigDecimal getCotizacion() { return cotizacion; }
        public Long getOrdenPago() { return ordenPago; }
        public String getOrdenEstado() { return ordenEstado; }
        public boolean isTieneOrden() { return ordenPago != null; }
        public boolean isOrdenPagada() { return "PAGADA".equals(ordenEstado); }
        public boolean isTieneProveedor() { return proveedorNombre != null && !proveedorNombre.isEmpty(); }
        public boolean isTieneCotizacion() { return cotizacion != null && cotizacion.signum() > 0; }
        public String getPrioridadLabel() { return prioridad == null ? "" : (prioridad.equals("ALTA") ? "Alta" : prioridad.equals("BAJA") ? "Baja" : "Media"); }
        public List<Foto> getFotosCliente() { List<Foto> l = new ArrayList<>(); for (Foto f : fotos) if (!"SOLUCION".equals(f.origen)) l.add(f); return l; }
        public List<Foto> getFotosSolucion() { List<Foto> l = new ArrayList<>(); for (Foto f : fotos) if ("SOLUCION".equals(f.origen)) l.add(f); return l; }
        public Long getId() { return id; }
        public String getTipo() { return tipo; }
        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
        public String getEstado() { return estado; }
        public String getPrioridad() { return prioridad; }
        public String getRespuesta() { return respuesta; }
        public String getCliente() { return cliente; }
        public String getUnidad() { return unidad; }
        public LocalDateTime getCreado() { return creado; }
        public List<Foto> getFotos() { return fotos; }
        public String getEstadoLabel() {
            if (estado == null) return "";
            switch (estado) {
                case "ABIERTO":    return "Abierto";
                case "EN_PROCESO": return "En proceso";
                case "RESUELTO":   return "Resuelto";
                case "RECHAZADO":  return "Rechazado";
                case "CERRADO":    return "Cerrado";
                default:           return estado;
            }
        }
        public String getTipoLabel() {
            if (tipo == null) return "";
            switch (tipo) {
                case "MANTENIMIENTO":  return "Mantenimiento";
                case "DANO":           return "Daño";
                case "SERVICIO":       return "Servicio";
                case "ADMINISTRATIVO": return "Administrativo";
                default:               return "Otro";
            }
        }
        public boolean isAbierto() { return "ABIERTO".equals(estado) || "EN_PROCESO".equals(estado); }
    }
    public static class Foto {
        public Long id; public String nombre, mime, origen;
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        public String getMime() { return mime; }
        public String getOrigen() { return origen; }
        public boolean isEvidencia() { return "SOLUCION".equals(origen); }
    }
    public static class Seg {
        public String tipo, mensaje, usuario;
        public LocalDateTime creado;
        public String getTipo() { return tipo; }
        public String getMensaje() { return mensaje; }
        public String getUsuario() { return usuario; }
        public LocalDateTime getCreado() { return creado; }
    }
    public static class Dash {
        public long total, abiertos, enProceso, resueltos, cerrados, rechazados, altaPrioridad, ordenes;
        public java.math.BigDecimal montoOrdenes = java.math.BigDecimal.ZERO;
        public long getTotal() { return total; }
        public long getAbiertos() { return abiertos; }
        public long getEnProceso() { return enProceso; }
        public long getResueltos() { return resueltos; }
        public long getCerrados() { return cerrados; }
        public long getRechazados() { return rechazados; }
        public long getAltaPrioridad() { return altaPrioridad; }
        public long getOrdenes() { return ordenes; }
        public java.math.BigDecimal getMontoOrdenes() { return montoOrdenes; }
    }
    public static class Unidad {
        public Long operacion; public String nombre, tipo;
        public Long getOperacion() { return operacion; }
        public String getNombre() { return nombre; }
        public String getTipo() { return tipo; }
        public String getLabel() {
            String t = tipo == null ? "" : (tipo.toUpperCase().startsWith("ALQ") ? " (alquiler)" : " (venta)");
            return (nombre == null ? "Unidad " + operacion : nombre) + t;
        }
    }
    public static class Descarga {
        public final String nombre, contentType; public final byte[] datos;
        public Descarga(String nombre, String contentType, byte[] datos) { this.nombre = nombre; this.contentType = contentType; this.datos = datos; }
    }
    /** Proveedor para el selector (persona con rol PROVEEDOR). */
    public static class Prov {
        public Long id; public String nombre, documento;
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        public String getDocumento() { return documento; }
        public String getLabel() { return nombre + (documento == null || documento.isBlank() ? "" : " — " + documento); }
    }
    /** Concepto/rubro de gasto (articulo) para el egreso. */
    public static class Concepto {
        public Long id; public String descripcion;
        public Long getId() { return id; }
        public String getDescripcion() { return descripcion; }
    }
}
