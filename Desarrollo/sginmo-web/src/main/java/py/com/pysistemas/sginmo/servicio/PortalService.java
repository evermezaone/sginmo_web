package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0055 - Portal de cuenta (solo lectura). @AislarTenant aisla por empresa (RLS); ademas TODAS
 * las consultas filtran por la persona vinculada al usuario logueado (operacion.cliente / cobro.persona),
 * de modo que un cliente nunca ve datos de otro cliente ni de otra empresa. No ejecuta cobros ni cambios.
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class PortalService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    private Path baseDir() {
        String dir = System.getenv("SGINMO_ARCHIVOS_DIR");
        if (dir == null || dir.isBlank()) dir = System.getProperty("user.home", ".") + "/sginmo/archivos";
        return Path.of(dir);
    }

    private static BigDecimal dec(Object o) { return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString()); }

    /** Una columna date puede llegar como LocalDate (Hibernate 7) o java.sql.Date segun el driver. */
    private static LocalDate aLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return LocalDate.parse(o.toString());
    }

    /** Resumen de cuenta del cliente: deuda vencida, proxima cuota, total pagado, cantidad de operaciones. */
    public ResumenCuenta resumen(Long persona) {
        if (persona == null) return new ResumenCuenta();
        ResumenCuenta r = new ResumenCuenta();
        r.deudaVencida = dec(em.createNativeQuery(
            "SELECT COALESCE(SUM(cc.saldo),0) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " WHERE o.cliente=:p AND cc.estado='PENDIENTE' AND cc.fecha_vencimiento < current_date")
            .setParameter("p", persona).getSingleResult());
        r.totalPagado = dec(em.createNativeQuery(
            "SELECT COALESCE(SUM(monto),0) FROM cobro WHERE persona=:p AND estado='ACTIVO'")
            .setParameter("p", persona).getSingleResult());
        Number ops = (Number) em.createNativeQuery(
            "SELECT COUNT(*) FROM operacion WHERE cliente=:p").setParameter("p", persona).getSingleResult();
        r.operaciones = ops == null ? 0 : ops.longValue();
        List<?> prox = em.createNativeQuery(
            "SELECT cc.fecha_vencimiento, cc.saldo FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " WHERE o.cliente=:p AND cc.estado='PENDIENTE' AND cc.fecha_vencimiento >= current_date"
          + " ORDER BY cc.fecha_vencimiento LIMIT 1").setParameter("p", persona).getResultList();
        if (!prox.isEmpty()) {
            Object[] fila = (Object[]) prox.get(0);
            r.proximaCuota = aLocalDate(fila[0]);
            r.proximaCuotaMonto = dec(fila[1]);
        }
        return r;
    }

    public List<FilaCuota> cuotas(Long persona) {
        List<FilaCuota> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT cc.numero_cuota, cc.fecha_vencimiento, cc.monto, cc.saldo, cc.estado, o.operacion"
          + " FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " WHERE o.cliente=:p ORDER BY cc.fecha_vencimiento").setParameter("p", persona).getResultList();
        for (Object[] f : filas) {
            FilaCuota c = new FilaCuota();
            c.numero = ((Number) f[0]).intValue();
            c.vencimiento = aLocalDate(f[1]);
            c.monto = dec(f[2]); c.saldo = dec(f[3]);
            c.estado = (String) f[4];
            c.operacion = ((Number) f[5]).longValue();
            out.add(c);
        }
        return out;
    }

    public List<FilaPago> pagos(Long persona) {
        List<FilaPago> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT fecha, monto, estado FROM cobro WHERE persona=:p AND estado='ACTIVO' ORDER BY fecha DESC")
            .setParameter("p", persona).getResultList();
        for (Object[] f : filas) {
            FilaPago pago = new FilaPago();
            pago.fecha = aLocalDate(f[0]);
            pago.monto = dec(f[1]); pago.estado = (String) f[2];
            out.add(pago);
        }
        return out;
    }

    // ── Vista de PROPIETARIO (REQ-0078 obs 300): activos, operaciones, liquidaciones y documentos
    //    del propietario, filtrados por persona (dueño) + tenant (RLS). Nunca ve datos ajenos. ──

    /** Activos (propiedades) de los que la persona es propietaria. */
    public List<FilaActivo> activosPropietario(Long persona) {
        List<FilaActivo> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT a.activo, a.nombre, a.estado FROM activo a"
          + " JOIN activo_propietario ap ON ap.activo = a.activo"
          + " WHERE ap.propietario = :p AND ap.estado = 'ACTIVO' ORDER BY a.nombre").setParameter("p", persona).getResultList();
        for (Object[] f : filas) {
            FilaActivo a = new FilaActivo();
            a.id = ((Number) f[0]).longValue();
            a.nombre = (String) f[1]; a.estado = (String) f[2];
            out.add(a);
        }
        return out;
    }

    /** Operaciones realizadas sobre los activos del propietario. */
    public List<FilaOperacion> operacionesPropietario(Long persona) {
        List<FilaOperacion> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT o.operacion, o.tipo_operacion, o.fecha_operacion, o.estado, a.nombre"
          + " FROM operacion o JOIN activo a ON a.activo = o.activo"
          + " JOIN activo_propietario ap ON ap.activo = o.activo"
          + " WHERE ap.propietario = :p AND ap.estado = 'ACTIVO' ORDER BY o.fecha_operacion DESC").setParameter("p", persona).getResultList();
        for (Object[] f : filas) {
            FilaOperacion o = new FilaOperacion();
            o.id = ((Number) f[0]).longValue();
            o.tipo = (String) f[1];
            o.fecha = aLocalDate(f[2]);
            o.estado = (String) f[3]; o.activo = (String) f[4];
            out.add(o);
        }
        return out;
    }

    /** Liquidaciones de las operaciones sobre los activos del propietario. */
    public List<FilaLiquidacion> liquidacionesPropietario(Long persona) {
        List<FilaLiquidacion> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT l.liquidacion, l.fecha, l.total_garantia, l.total_gastos, l.saldo, a.nombre"
          + " FROM liquidacion l JOIN operacion o ON o.operacion = l.operacion"
          + " JOIN activo a ON a.activo = o.activo"
          + " JOIN activo_propietario ap ON ap.activo = o.activo"
          + " WHERE ap.propietario = :p AND ap.estado = 'ACTIVO' ORDER BY l.fecha DESC").setParameter("p", persona).getResultList();
        for (Object[] f : filas) {
            FilaLiquidacion l = new FilaLiquidacion();
            l.id = ((Number) f[0]).longValue();
            l.fecha = aLocalDate(f[1]);
            l.totalGarantia = dec(f[2]); l.totalGastos = dec(f[3]); l.saldo = dec(f[4]);
            l.activo = (String) f[5];
            out.add(l);
        }
        return out;
    }

    /** Documentos habilitados para el portal, de los activos/operaciones del propietario. */
    public List<FilaDoc> documentosPropietario(Long persona) {
        List<FilaDoc> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT documento_adjunto, tipo, descripcion, nombre_original FROM documento_adjunto"
          + " WHERE visible_portal = true AND estado='ACTIVO' AND ("
          + "   (entidad_tipo='PERSONA' AND entidad_id=:p) OR"
          + "   (entidad_tipo='ACTIVO' AND entidad_id IN (SELECT activo FROM activo_propietario WHERE propietario=:p AND estado='ACTIVO')) OR"
          + "   (entidad_tipo='OPERACION' AND entidad_id IN (SELECT o.operacion FROM operacion o"
          + "        JOIN activo_propietario ap ON ap.activo=o.activo WHERE ap.propietario=:p AND ap.estado='ACTIVO')))"
          + " ORDER BY fecha_creacion DESC").setParameter("p", persona).getResultList();
        for (Object[] f : filas) {
            FilaDoc d = new FilaDoc();
            d.id = ((Number) f[0]).longValue();
            d.tipo = (String) f[1]; d.descripcion = (String) f[2]; d.nombre = (String) f[3];
            out.add(d);
        }
        return out;
    }

    /** Documentos habilitados para el portal, vinculados a la persona o a sus operaciones. */
    public List<FilaDoc> documentos(Long persona) {
        List<FilaDoc> out = new ArrayList<>();
        if (persona == null) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT documento_adjunto, tipo, descripcion, nombre_original FROM documento_adjunto"
          + " WHERE visible_portal = true AND estado='ACTIVO' AND ("
          + "   (entidad_tipo='PERSONA' AND entidad_id=:p) OR"
          + "   (entidad_tipo='OPERACION' AND entidad_id IN (SELECT operacion FROM operacion WHERE cliente=:p)))"
          + " ORDER BY fecha_creacion DESC").setParameter("p", persona).getResultList();
        for (Object[] f : filas) {
            FilaDoc d = new FilaDoc();
            d.id = ((Number) f[0]).longValue();
            d.tipo = (String) f[1]; d.descripcion = (String) f[2]; d.nombre = (String) f[3];
            out.add(d);
        }
        return out;
    }

    /** Registra un acceso/descarga del portal (auditoria). */
    @Transactional
    public void registrarAcceso(Long tenant, String usuario, Long persona, String accion, String recurso, String ip) {
        em.createNativeQuery(
            "INSERT INTO portal_acceso (tenant, usuario_codigo, persona, accion, recurso, ip)"
          + " VALUES (:t,:u,:p,:a,:r,:ip)")
            .setParameter("t", tenant).setParameter("u", usuario == null ? "portal" : usuario)
            .setParameter("p", persona).setParameter("a", accion)
            .setParameter("r", recurso).setParameter("ip", ip).executeUpdate();
    }

    /**
     * Descarga protegida: el documento debe estar habilitado para portal Y pertenecer a la persona
     * (o a una de sus operaciones). Devuelve {nombre, contentType, bytes}. Registra la descarga.
     */
    public Descarga descargar(Long docId, Long persona, Long tenant, String usuario, String ip) {
        if (persona == null || docId == null) throw new NegocioException("Documento no disponible");
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
            "SELECT nombre_original, content_type, nombre_fisico, tenant FROM documento_adjunto d"
          + " WHERE d.documento_adjunto=:id AND d.visible_portal=true AND d.estado='ACTIVO' AND ("
          + "   (d.entidad_tipo='PERSONA' AND d.entidad_id=:p) OR"
          + "   (d.entidad_tipo='OPERACION' AND d.entidad_id IN (SELECT operacion FROM operacion WHERE cliente=:p)) OR"
          // obs 300: tambien los documentos del propietario (sus activos y las operaciones sobre ellos).
          + "   (d.entidad_tipo='ACTIVO' AND d.entidad_id IN (SELECT activo FROM activo_propietario WHERE propietario=:p AND estado='ACTIVO')) OR"
          + "   (d.entidad_tipo='OPERACION' AND d.entidad_id IN (SELECT o.operacion FROM operacion o"
          + "        JOIN activo_propietario ap ON ap.activo=o.activo WHERE ap.propietario=:p AND ap.estado='ACTIVO')))")
            .setParameter("id", docId).setParameter("p", persona).getResultList();
        if (filas.isEmpty()) throw new NegocioException("El documento no esta disponible para su cuenta");
        Object[] f = filas.get(0);
        String nombre = (String) f[0];
        String ct = f[1] == null ? "application/octet-stream" : (String) f[1];
        String fisico = (String) f[2];
        Long tdoc = ((Number) f[3]).longValue();
        try {
            byte[] datos = Files.readAllBytes(baseDir().resolve(String.valueOf(tdoc)).resolve(fisico));
            registrarAcceso(tenant, usuario, persona, "DESCARGA", nombre, ip);
            return new Descarga(nombre, ct, datos);
        } catch (Exception e) {
            throw new NegocioException("No se pudo leer el documento");
        }
    }

    // ── DTOs ──
    public static class ResumenCuenta {
        public BigDecimal deudaVencida = BigDecimal.ZERO;
        public BigDecimal totalPagado = BigDecimal.ZERO;
        public BigDecimal proximaCuotaMonto = BigDecimal.ZERO;
        public LocalDate proximaCuota;
        public long operaciones;
        public BigDecimal getDeudaVencida() { return deudaVencida; }
        public BigDecimal getTotalPagado() { return totalPagado; }
        public BigDecimal getProximaCuotaMonto() { return proximaCuotaMonto; }
        public LocalDate getProximaCuota() { return proximaCuota; }
        public long getOperaciones() { return operaciones; }
    }
    public static class FilaCuota {
        public int numero; public LocalDate vencimiento; public BigDecimal monto, saldo; public String estado; public Long operacion;
        public int getNumero() { return numero; }
        public LocalDate getVencimiento() { return vencimiento; }
        public BigDecimal getMonto() { return monto; }
        public BigDecimal getSaldo() { return saldo; }
        public String getEstado() { return estado; }
        public Long getOperacion() { return operacion; }
    }
    public static class FilaPago {
        public LocalDate fecha; public BigDecimal monto; public String estado;
        public LocalDate getFecha() { return fecha; }
        public BigDecimal getMonto() { return monto; }
        public String getEstado() { return estado; }
    }
    public static class FilaDoc {
        public Long id; public String tipo, descripcion, nombre;
        public Long getId() { return id; }
        public String getTipo() { return tipo; }
        public String getDescripcion() { return descripcion; }
        public String getNombre() { return nombre; }
    }
    public static class FilaActivo {
        public Long id; public String nombre, estado;
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        public String getEstado() { return estado; }
    }
    public static class FilaOperacion {
        public Long id; public LocalDate fecha; public String tipo, estado, activo;
        public Long getId() { return id; }
        public LocalDate getFecha() { return fecha; }
        public String getTipo() { return tipo; }
        public String getEstado() { return estado; }
        public String getActivo() { return activo; }
    }
    public static class FilaLiquidacion {
        public Long id; public LocalDate fecha; public BigDecimal totalGarantia, totalGastos, saldo; public String activo;
        public Long getId() { return id; }
        public LocalDate getFecha() { return fecha; }
        public BigDecimal getTotalGarantia() { return totalGarantia; }
        public BigDecimal getTotalGastos() { return totalGastos; }
        public BigDecimal getSaldo() { return saldo; }
        public String getActivo() { return activo; }
    }
    public static class Descarga {
        public final String nombre, contentType; public final byte[] datos;
        public Descarga(String nombre, String contentType, byte[] datos) { this.nombre = nombre; this.contentType = contentType; this.datos = datos; }
    }
}
