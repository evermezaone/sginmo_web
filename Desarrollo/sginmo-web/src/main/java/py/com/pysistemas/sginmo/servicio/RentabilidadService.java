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
 * REQ-0071 - Rentabilidad gerencial, ingresos y egresos por tipo (solo lectura). @AislarTenant + RLS.
 * Base CAJA: se calcula desde `ingreso_egreso` (ledger de ingresos/egresos realizados -estado CANCELADO-,
 * en la moneda base de la empresa; no mezcla monedas). La clasificacion "por tipo" viene de
 * `articulo.aplicacion` (dato, no texto hardcodeado). El DEPOSITO/GARANTIA es pasivo de terceros y NO
 * cuenta como rentabilidad (se muestra aparte). Cada linea/activo expone drillKey para el detalle (0074).
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional
public class RentabilidadService {

    public static final String PANTALLA = "rentabilidad";

    /** Aplicaciones que son pasivos de terceros (no rentabilidad). */
    private static final java.util.Set<String> PASIVOS = java.util.Set.of("DEPOSITO_GARANTIA", "GARANTIA");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    /** Resumen de rentabilidad base caja del periodo. */
    public Resumen resumen(LocalDate desde, LocalDate hasta) {
        autorizacion.exigir(PANTALLA, "VER");
        Resumen r = new Resumen();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return r;
        r.ingresos = porTipo("INGRESO", desde, hasta);
        r.egresos = porTipo("EGRESO", desde, hasta);
        for (Linea l : r.ingresos) {
            if (l.pasivo) r.totalDepositos = r.totalDepositos.add(l.monto);
            else r.totalIngresos = r.totalIngresos.add(l.monto);
        }
        for (Linea l : r.egresos) r.totalEgresos = r.totalEgresos.add(l.monto);
        r.neto = r.totalIngresos.subtract(r.totalEgresos);
        if (r.totalIngresos.signum() > 0) {
            r.margenPct = r.neto.multiply(BigDecimal.valueOf(100))
                    .divide(r.totalIngresos, 2, RoundingMode.HALF_UP);
        }
        return r;
    }

    private List<Linea> porTipo(String tipo, LocalDate desde, LocalDate hasta) {
        Query q = em.createNativeQuery(
            "SELECT COALESCE(art.aplicacion,'OTROS') AS apl, COALESCE(SUM(ie.monto),0) AS total"
          + " FROM ingreso_egreso ie LEFT JOIN articulo art ON art.articulo = ie.articulo"
          + " WHERE ie.tipo = :tipo AND ie.estado='CANCELADO' AND ie.fecha BETWEEN :d AND :h"
          + " GROUP BY COALESCE(art.aplicacion,'OTROS') ORDER BY total DESC")
            .setParameter("tipo", tipo).setParameter("d", desde).setParameter("h", hasta);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<Linea> out = new ArrayList<>();
        for (Object[] f : rows) {
            Linea l = new Linea();
            l.aplicacion = str(f[0]);
            l.etiqueta = etiqueta(l.aplicacion);
            l.pasivo = PASIVOS.contains(l.aplicacion);
            l.monto = f[1] == null ? BigDecimal.ZERO : new BigDecimal(f[1].toString());
            l.drillKey = "ingreso_egreso:" + tipo + ":" + l.aplicacion;
            out.add(l);
        }
        return out;
    }

    /** Ranking de activos por rentabilidad neta (ingresos operativos - egresos) del periodo. */
    public List<ActivoRent> rankingActivos(LocalDate desde, LocalDate hasta, int limite) {
        autorizacion.exigir(PANTALLA, "VER");
        List<ActivoRent> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        Query q = em.createNativeQuery(
            "SELECT a.activo, a.nombre,"
          + " COALESCE(SUM(CASE WHEN ie.tipo='INGRESO' AND COALESCE(art.aplicacion,'') NOT IN ('DEPOSITO_GARANTIA','GARANTIA') THEN ie.monto"
          + "               WHEN ie.tipo='EGRESO' THEN -ie.monto ELSE 0 END),0) AS neto"
          + " FROM ingreso_egreso ie JOIN activo a ON a.activo = ie.activo"
          + " LEFT JOIN articulo art ON art.articulo = ie.articulo"
          + " WHERE ie.estado='CANCELADO' AND ie.fecha BETWEEN :d AND :h"
          + " GROUP BY a.activo, a.nombre ORDER BY neto DESC")
            .setParameter("d", desde).setParameter("h", hasta).setMaxResults(Math.max(1, limite));
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) {
            ActivoRent x = new ActivoRent();
            x.activoId = ((Number) f[0]).longValue();
            x.nombre = str(f[1]);
            x.neto = f[2] == null ? BigDecimal.ZERO : new BigDecimal(f[2].toString());
            x.drillKey = "rentabilidad_activo:" + x.activoId;
            out.add(x);
        }
        return out;
    }

    private static String etiqueta(String aplicacion) {
        if (aplicacion == null) return "Otros";
        return switch (aplicacion) {
            case "ALQUILER" -> "Alquiler";
            case "VENTA" -> "Venta";
            case "COMISION_ALQUILER" -> "Comision alquiler";
            case "COMISION_VENTA" -> "Comision venta";
            case "DEPOSITO_GARANTIA", "GARANTIA" -> "Deposito/garantia (pasivo)";
            case "INTERES", "MORA" -> "Mora/interes";
            case "MANTENIMIENTO" -> "Mantenimiento";
            case "IMPUESTO" -> "Impuestos";
            case "DEVOLUCION" -> "Devoluciones";
            case "LIQUIDACION" -> "Gastos de liquidacion";
            case "OTROS", "" -> "Otros";
            default -> aplicacion.charAt(0) + aplicacion.substring(1).toLowerCase().replace('_', ' ');
        };
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    // ── DTOs ──

    public static class Linea {
        public String aplicacion, etiqueta, drillKey; public boolean pasivo; public BigDecimal monto = BigDecimal.ZERO;
        public String getAplicacion() { return aplicacion; }
        public String getEtiqueta() { return etiqueta; }
        public String getDrillKey() { return drillKey; }
        public boolean isPasivo() { return pasivo; }
        public BigDecimal getMonto() { return monto; }
    }

    public static class ActivoRent {
        public Long activoId; public String nombre, drillKey; public BigDecimal neto = BigDecimal.ZERO;
        public Long getActivoId() { return activoId; }
        public String getNombre() { return nombre; }
        public String getDrillKey() { return drillKey; }
        public BigDecimal getNeto() { return neto; }
    }

    public static class Resumen {
        public List<Linea> ingresos = new ArrayList<>(), egresos = new ArrayList<>();
        public BigDecimal totalIngresos = BigDecimal.ZERO, totalEgresos = BigDecimal.ZERO,
                totalDepositos = BigDecimal.ZERO, neto = BigDecimal.ZERO, margenPct = BigDecimal.ZERO;
        public List<Linea> getIngresos() { return ingresos; }
        public List<Linea> getEgresos() { return egresos; }
        public BigDecimal getTotalIngresos() { return totalIngresos; }
        public BigDecimal getTotalEgresos() { return totalEgresos; }
        public BigDecimal getTotalDepositos() { return totalDepositos; }
        public BigDecimal getNeto() { return neto; }
        public BigDecimal getMargenPct() { return margenPct; }
    }
}
