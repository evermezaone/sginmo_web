package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0072 - Ocupacion, vacancia y brecha hacia objetivo (solo lectura). @AislarTenant + RLS (V28) para
 * que no se vean activos de otra empresa. "Alquilable" = activo con precio_alquiler > 0 y estado <> VENDIDA
 * (regla documentada; el objetivo % es configurable por parametro OCUPACION_OBJETIVO_PCT). Da la lista de
 * propiedades VACANTES ordenadas por prioridad comercial (mayor precio de alquiler primero) para el drill-down.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional
public class OcupacionService {

    public static final String PANTALLA = "ocupacion";

    /** Regla de "alquilable" (documentada). Un activo vendido no cuenta; requiere precio de alquiler. */
    private static final String ALQUILABLE = "a.precio_alquiler > 0 AND a.estado <> 'VENDIDA'";
    /** Subconsulta de ocupados a una fecha (operacion de alquiler que cubre esa fecha). */
    private static final String OCUPADOS_SUB =
        "SELECT DISTINCT o.activo FROM operacion o WHERE o.tipo_operacion='ALQUILER'"
      + " AND o.fecha_inicio_contrato <= :f AND (o.fecha_finalizacion IS NULL OR o.fecha_finalizacion > :f)";

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private ParametroConfig parametros;

    /** Objetivo de ocupacion configurable (%), default 90. */
    public int objetivoPct() { return Math.min(100, Math.max(1, parametros.entero("OCUPACION_OBJETIVO_PCT", 90))); }

    /** Resumen de ocupacion a hoy con brecha hacia el objetivo. */
    public Resumen resumen() {
        autorizacion.exigir(PANTALLA, "VER");
        Resumen r = new Resumen();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return r;
        LocalDate hoy = LocalDate.now();
        r.alquilables = num(em.createNativeQuery("SELECT COUNT(*) FROM activo a WHERE " + ALQUILABLE));
        r.ocupados = num(em.createNativeQuery(
            "SELECT COUNT(*) FROM activo a WHERE " + ALQUILABLE + " AND a.activo IN (" + OCUPADOS_SUB + ")")
            .setParameter("f", hoy));
        r.vacantes = Math.max(0, r.alquilables - r.ocupados);
        r.objetivoPct = objetivoPct();
        if (r.alquilables > 0) {
            r.ocupacionPct = BigDecimal.valueOf(r.ocupados).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(r.alquilables), 2, RoundingMode.HALF_UP);
            // Unidades necesarias para llegar al objetivo (techo) y brecha faltante.
            r.objetivoUnidades = (long) Math.ceil(r.objetivoPct / 100.0 * r.alquilables);
            r.brecha = Math.max(0, r.objetivoUnidades - r.ocupados);
        }
        return r;
    }

    /**
     * Propiedades vacantes (evidencia del drill-down), ordenadas por prioridad comercial (mayor precio de
     * alquiler primero). Las primeras {@code brecha} son las que faltan alquilar para llegar al objetivo.
     */
    public List<Vacante> vacantes(int limite) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Vacante> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        LocalDate hoy = LocalDate.now();
        Query q = em.createNativeQuery(
            "SELECT a.activo, a.nombre, COALESCE(te.descripcion,'-'), a.precio_alquiler, COALESCE(a.direccion,'')"
          + " FROM activo a LEFT JOIN entidad te ON te.entidad = a.tipo"
          + " WHERE " + ALQUILABLE + " AND a.activo NOT IN (" + OCUPADOS_SUB + ")"
          + " ORDER BY a.precio_alquiler DESC, a.nombre")
            .setParameter("f", hoy).setMaxResults(Math.max(1, limite));
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) {
            Vacante v = new Vacante();
            v.activoId = ((Number) f[0]).longValue();
            v.nombre = str(f[1]);
            v.tipo = str(f[2]);
            v.precioAlquiler = f[3] == null ? BigDecimal.ZERO : new BigDecimal(f[3].toString());
            v.direccion = str(f[4]);
            out.add(v);
        }
        return out;
    }

    /** Ocupacion/vacancia por tipo de activo (breakdown). */
    public List<PorGrupo> porTipo() {
        autorizacion.exigir(PANTALLA, "VER");
        List<PorGrupo> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        LocalDate hoy = LocalDate.now();
        Query q = em.createNativeQuery(
            "SELECT COALESCE(te.descripcion,'-') AS tipo, COUNT(*) AS alq,"
          + " SUM(CASE WHEN a.activo IN (" + OCUPADOS_SUB + ") THEN 1 ELSE 0 END) AS ocu"
          + " FROM activo a LEFT JOIN entidad te ON te.entidad = a.tipo"
          + " WHERE " + ALQUILABLE + " GROUP BY COALESCE(te.descripcion,'-') ORDER BY alq DESC")
            .setParameter("f", hoy);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) {
            PorGrupo g = new PorGrupo();
            g.grupo = str(f[0]);
            g.alquilables = ((Number) f[1]).longValue();
            g.ocupados = f[2] == null ? 0 : ((Number) f[2]).longValue();
            g.vacantes = Math.max(0, g.alquilables - g.ocupados);
            g.ocupacionPct = g.alquilables == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(g.ocupados).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(g.alquilables), 1, RoundingMode.HALF_UP);
            out.add(g);
        }
        return out;
    }

    private long num(Query q) { Object r = q.getSingleResult(); return r == null ? 0 : ((Number) r).longValue(); }
    private static String str(Object o) { return o == null ? "" : o.toString(); }

    // ── DTOs ──

    public static class Resumen {
        public long alquilables, ocupados, vacantes, objetivoUnidades, brecha;
        public int objetivoPct;
        public BigDecimal ocupacionPct = BigDecimal.ZERO;
        public long getAlquilables() { return alquilables; }
        public long getOcupados() { return ocupados; }
        public long getVacantes() { return vacantes; }
        public long getObjetivoUnidades() { return objetivoUnidades; }
        public long getBrecha() { return brecha; }
        public int getObjetivoPct() { return objetivoPct; }
        public BigDecimal getOcupacionPct() { return ocupacionPct; }
        public boolean isCumpleObjetivo() { return brecha == 0 && alquilables > 0; }
    }

    public static class Vacante {
        public Long activoId; public String nombre, tipo, direccion; public BigDecimal precioAlquiler = BigDecimal.ZERO;
        public Long getActivoId() { return activoId; }
        public String getNombre() { return nombre; }
        public String getTipo() { return tipo; }
        public String getDireccion() { return direccion; }
        public BigDecimal getPrecioAlquiler() { return precioAlquiler; }
    }

    public static class PorGrupo {
        public String grupo; public long alquilables, ocupados, vacantes; public BigDecimal ocupacionPct = BigDecimal.ZERO;
        public String getGrupo() { return grupo; }
        public long getAlquilables() { return alquilables; }
        public long getOcupados() { return ocupados; }
        public long getVacantes() { return vacantes; }
        public BigDecimal getOcupacionPct() { return ocupacionPct; }
    }
}
