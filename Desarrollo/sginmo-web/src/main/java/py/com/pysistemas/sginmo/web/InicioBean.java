package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.InicioService;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Tablero de inicio (REQ-0030): indicadores del negocio + datos del esqueleto (REQ-0001).
 * Los KPIs se calculan en InicioService (@AislarTenant) para que corran con app.tenant bajo
 * RLS (obs 255); el bean solo presenta el snapshot.
 */
@Named
@RequestScoped
public class InicioBean implements Serializable {

    @Inject
    private SesionUsuario sesion;

    @Inject
    private transient InicioService inicioService;

    @Inject
    private transient py.com.pysistemas.sginmo.servicio.AgendaService agendaService;

    private long activosLibres, activosOcupados, activosVendidos;
    private long operacionesVigentes, cuotasVencidas;
    private long tareasAtrasadas, proximosVencimientos;   // REQ-0052
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
        // REQ-0052: refresca los vencimientos automaticos antes de contar (idempotente por dedup).
        try { agendaService.generarAutomaticos(); } catch (RuntimeException ignore) { /* no bloquea el tablero */ }
        // Los KPIs se calculan en el service @AislarTenant (fija app.tenant -> RLS ve el tenant).
        InicioService.Kpis k = inicioService.kpis();
        activosLibres = k.activosLibres;
        activosOcupados = k.activosOcupados;
        activosVendidos = k.activosVendidos;
        operacionesVigentes = k.operacionesVigentes;
        cuotasVencidas = k.cuotasVencidas;
        tareasAtrasadas = k.tareasAtrasadas;
        proximosVencimientos = k.proximosVencimientos;
        recaudadoHoy = k.recaudadoHoy;
        saldoPorCobrar = k.saldoPorCobrar;
    }

    /** REQ-0055: si el usuario es de portal, no ve el panel administrativo. */
    public String guardPortal() {
        if (sesion != null && sesion.isLogueado() && sesion.getUsuario() != null
                && "PORTAL".equals(sesion.getUsuario().getPerfil())) {
            return "/portal/inicio?faces-redirect=true";
        }
        return null;
    }

    public long getActivosLibres() { return activosLibres; }
    public long getActivosOcupados() { return activosOcupados; }
    public long getActivosVendidos() { return activosVendidos; }
    public long getOperacionesVigentes() { return operacionesVigentes; }
    public long getCuotasVencidas() { return cuotasVencidas; }
    public long getTareasAtrasadas() { return tareasAtrasadas; }
    public long getProximosVencimientos() { return proximosVencimientos; }
    public BigDecimal getRecaudadoHoy() { return recaudadoHoy; }
    public BigDecimal getSaldoPorCobrar() { return saldoPorCobrar; }
}
