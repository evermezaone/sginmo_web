package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import py.com.one.security.web.SesionUsuario;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Tablero de inicio (REQ-0030): indicadores del negocio + datos del esqueleto (REQ-0001).
 * Consultas de agregacion directas; los saldos salen de la vista v_operacion_saldo del motor.
 */
@Named
@RequestScoped
public class InicioBean implements Serializable {

    @PersistenceContext(unitName = "sginmoPU")
    private transient EntityManager em;

    @Inject
    private SesionUsuario sesion;

    @Inject
    private ContextoEmpresa contexto;

    private long activosLibres, activosOcupados, activosVendidos;
    private long operacionesVigentes, cuotasVencidas;
    private BigDecimal recaudadoHoy = BigDecimal.ZERO;
    private BigDecimal saldoPorCobrar = BigDecimal.ZERO;

    public String getSistema() { return "SGInmo Web"; }
    public String getVersion() { return "0.1.0-SNAPSHOT"; }
    public String getStack() {
        return "WildFly 40 · Jakarta EE 11 · PrimeFaces 15 · Java " + System.getProperty("java.version");
    }
    public String getHora() {
        return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @PostConstruct
    public void iniciar() {
        if (sesion == null || !sesion.isLogueado()) {
            return;
        }
        // Aislamiento multiempresa (obs 238): TODOS los KPIs se filtran por la empresa
        // del contexto. Sin empresa seleccionada, el tablero queda en cero.
        Long emp = contexto == null || contexto.getEmpresa() == null ? null : contexto.getEmpresa().getId();
        if (emp == null) {
            return;
        }
        // activos sin empresa asignada son globales (mismo criterio que listadoActivos, obs 236)
        activosLibres = num("SELECT COUNT(*) FROM activo WHERE estado = 'LIBRE' AND tenant = :emp", emp);
        activosOcupados = num("SELECT COUNT(*) FROM activo WHERE estado = 'OCUPADA' AND tenant = :emp", emp);
        activosVendidos = num("SELECT COUNT(*) FROM activo WHERE estado = 'VENDIDA' AND tenant = :emp", emp);
        operacionesVigentes = num("SELECT COUNT(*) FROM operacion WHERE estado = 'VIGENTE' AND tenant = :emp", emp);
        cuotasVencidas = num("SELECT COUNT(*) FROM cronograma_cuota cc JOIN operacion o ON o.operacion = cc.operacion"
                + " WHERE cc.estado = 'PENDIENTE' AND cc.fecha_vencimiento < current_date AND o.tenant = :emp", emp);
        recaudadoHoy = dec("SELECT COALESCE(SUM(monto),0) FROM cobro WHERE estado = 'ACTIVO' AND fecha = current_date AND tenant = :emp", emp);
        saldoPorCobrar = dec("SELECT COALESCE(SUM(s.saldo_pendiente),0) FROM v_operacion_saldo s"
                + " JOIN operacion o ON o.operacion = s.operacion WHERE o.tenant = :emp", emp);
    }

    private long num(String sql, Long emp) {
        try {
            Object r = em.createNativeQuery(sql).setParameter("emp", emp).getSingleResult();
            return r == null ? 0 : ((Number) r).longValue();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private BigDecimal dec(String sql, Long emp) {
        try {
            Object r = em.createNativeQuery(sql).setParameter("emp", emp).getSingleResult();
            return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString());
        } catch (RuntimeException e) {
            return BigDecimal.ZERO;
        }
    }

    public long getActivosLibres() { return activosLibres; }
    public long getActivosOcupados() { return activosOcupados; }
    public long getActivosVendidos() { return activosVendidos; }
    public long getOperacionesVigentes() { return operacionesVigentes; }
    public long getCuotasVencidas() { return cuotasVencidas; }
    public BigDecimal getRecaudadoHoy() { return recaudadoHoy; }
    public BigDecimal getSaldoPorCobrar() { return saldoPorCobrar; }
}
