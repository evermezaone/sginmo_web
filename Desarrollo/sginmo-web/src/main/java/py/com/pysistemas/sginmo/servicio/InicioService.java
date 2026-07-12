package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;

/**
 * KPIs del tablero de inicio (REQ-0030) por tenant. Vive en un service @AislarTenant +
 * @Transactional (obs 255) para que el interceptor F5 fije app.tenant antes de consultar:
 * las tablas transaccionales (activo/operacion/cobro/v_operacion_saldo) estan bajo RLS (V28)
 * y sin app.tenant quedarian invisibles (tablero en cero). El bean JSF NO accede a esas tablas.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional
public class InicioService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    /** Snapshot de indicadores del tenant efectivo; en contexto global (-1) queda en cero. */
    public Kpis kpis() {
        Kpis k = new Kpis();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) {
            return k;   // sin empresa propia (SUPERADMIN global): tablero en cero hasta elegir una
        }
        k.activosLibres    = num("SELECT COUNT(*) FROM activo WHERE estado = 'LIBRE' AND tenant = :emp", emp);
        k.activosOcupados  = num("SELECT COUNT(*) FROM activo WHERE estado = 'OCUPADA' AND tenant = :emp", emp);
        k.activosVendidos  = num("SELECT COUNT(*) FROM activo WHERE estado = 'VENDIDA' AND tenant = :emp", emp);
        k.operacionesVigentes = num("SELECT COUNT(*) FROM operacion WHERE estado = 'VIGENTE' AND tenant = :emp", emp);
        k.cuotasVencidas   = num("SELECT COUNT(*) FROM cronograma_cuota cc JOIN operacion o ON o.operacion = cc.operacion"
                + " WHERE cc.estado = 'PENDIENTE' AND cc.fecha_vencimiento < current_date AND o.tenant = :emp", emp);
        k.recaudadoHoy     = dec("SELECT COALESCE(SUM(monto),0) FROM cobro WHERE estado = 'ACTIVO' AND fecha = current_date AND tenant = :emp", emp);
        k.saldoPorCobrar   = dec("SELECT COALESCE(SUM(s.saldo_pendiente),0) FROM v_operacion_saldo s"
                + " JOIN operacion o ON o.operacion = s.operacion WHERE o.tenant = :emp", emp);
        // REQ-0052: agenda operativa
        k.tareasAtrasadas  = num("SELECT COUNT(*) FROM agenda_evento WHERE estado IN ('PENDIENTE','EN_CURSO')"
                + " AND fecha_evento < current_date AND tenant = :emp", emp);
        k.proximosVencimientos = num("SELECT COUNT(*) FROM agenda_evento WHERE tipo = 'VENCIMIENTO'"
                + " AND estado = 'PENDIENTE' AND fecha_evento BETWEEN current_date AND current_date + 7 AND tenant = :emp", emp);
        return k;
    }

    private long num(String sql, Long emp) {
        Object r = em.createNativeQuery(sql).setParameter("emp", emp).getSingleResult();
        return r == null ? 0 : ((Number) r).longValue();
    }

    private BigDecimal dec(String sql, Long emp) {
        Object r = em.createNativeQuery(sql).setParameter("emp", emp).getSingleResult();
        return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString());
    }

    /** Contenedor simple de KPIs (evita 7 llamadas y 7 transacciones). */
    public static class Kpis {
        public long activosLibres, activosOcupados, activosVendidos, operacionesVigentes, cuotasVencidas;
        public long tareasAtrasadas, proximosVencimientos;   // REQ-0052
        public BigDecimal recaudadoHoy = BigDecimal.ZERO;
        public BigDecimal saldoPorCobrar = BigDecimal.ZERO;
    }
}
