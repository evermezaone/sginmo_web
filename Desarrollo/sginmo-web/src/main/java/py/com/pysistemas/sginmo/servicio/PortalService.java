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
            r.proximaCuota = ((java.sql.Date) fila[0]).toLocalDate();
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
            c.vencimiento = ((java.sql.Date) f[1]).toLocalDate();
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
            pago.fecha = ((java.sql.Date) f[0]).toLocalDate();
            pago.monto = dec(f[1]); pago.estado = (String) f[2];
            out.add(pago);
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
          + "   (d.entidad_tipo='OPERACION' AND d.entidad_id IN (SELECT operacion FROM operacion WHERE cliente=:p)))")
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
    public static class Descarga {
        public final String nombre, contentType; public final byte[] datos;
        public Descarga(String nombre, String contentType, byte[] datos) { this.nombre = nombre; this.contentType = contentType; this.datos = datos; }
    }
}
