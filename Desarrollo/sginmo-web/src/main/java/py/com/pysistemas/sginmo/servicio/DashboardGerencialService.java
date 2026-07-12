package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * REQ-0056 - KPIs gerenciales (solo lectura). @AislarTenant + @Transactional para que RLS (V28)
 * filtre por empresa. Los montos NUNCA mezclan monedas: se calculan para la moneda seleccionada.
 * Todos los indicadores son datos REALES (transaccionales), no proyecciones.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional
public class DashboardGerencialService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    /** KPIs del tenant efectivo. En contexto global (-1) devuelve cero. */
    public Kpis kpis(LocalDate desde, LocalDate hasta, Long moneda, Long sucursal) {
        Kpis k = new Kpis();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return k;

        String sucOp = sucursal != null ? " AND o.sucursal = :suc" : "";
        String sucCb = sucursal != null ? " AND cobro.sucursal = :suc" : "";

        k.cuotasVencidas = num(bind(em.createNativeQuery(
            "SELECT COUNT(*) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " WHERE cc.estado='PENDIENTE' AND cc.fecha_vencimiento < current_date" + sucOp), sucursal, null, null, null));

        k.montoVencido = dec(bind(em.createNativeQuery(
            "SELECT COALESCE(SUM(cc.saldo),0) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " WHERE cc.estado='PENDIENTE' AND cc.fecha_vencimiento < current_date AND cc.moneda = :mon" + sucOp),
            sucursal, moneda, null, null));

        k.operacionesActivas = num(bind(em.createNativeQuery(
            "SELECT COUNT(*) FROM operacion o WHERE o.estado='VIGENTE'" + sucOp), sucursal, null, null, null));
        k.ventas = num(bind(em.createNativeQuery(
            "SELECT COUNT(*) FROM operacion o WHERE o.tipo_operacion='VENTA'" + sucOp), sucursal, null, null, null));
        k.alquileres = num(bind(em.createNativeQuery(
            "SELECT COUNT(*) FROM operacion o WHERE o.tipo_operacion='ALQUILER'" + sucOp), sucursal, null, null, null));

        k.propiedadesDisponibles = num(em.createNativeQuery(
            "SELECT COUNT(*) FROM activo WHERE estado='LIBRE'"));

        k.cobradoPeriodo = dec(bind(em.createNativeQuery(
            "SELECT COALESCE(SUM(cobro.monto),0) FROM cobro WHERE cobro.estado='ACTIVO'"
          + " AND cobro.fecha BETWEEN :desde AND :hasta AND cobro.moneda = :mon" + sucCb),
            sucursal, moneda, desde, hasta));
        return k;
    }

    private Query bind(Query q, Long sucursal, Long moneda, LocalDate desde, LocalDate hasta) {
        if (sucursal != null) q.setParameter("suc", sucursal);
        if (moneda != null) q.setParameter("mon", moneda);
        if (desde != null) q.setParameter("desde", desde);
        if (hasta != null) q.setParameter("hasta", hasta);
        return q;
    }

    private long num(Query q) {
        Object r = q.getSingleResult();
        return r == null ? 0 : ((Number) r).longValue();
    }

    private BigDecimal dec(Query q) {
        Object r = q.getSingleResult();
        return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString());
    }

    public static class Kpis {
        public long cuotasVencidas, operacionesActivas, ventas, alquileres, propiedadesDisponibles;
        public BigDecimal montoVencido = BigDecimal.ZERO;
        public BigDecimal cobradoPeriodo = BigDecimal.ZERO;
        public long getCuotasVencidas() { return cuotasVencidas; }
        public long getOperacionesActivas() { return operacionesActivas; }
        public long getVentas() { return ventas; }
        public long getAlquileres() { return alquileres; }
        public long getPropiedadesDisponibles() { return propiedadesDisponibles; }
        public BigDecimal getMontoVencido() { return montoVencido; }
        public BigDecimal getCobradoPeriodo() { return cobradoPeriodo; }
    }
}
