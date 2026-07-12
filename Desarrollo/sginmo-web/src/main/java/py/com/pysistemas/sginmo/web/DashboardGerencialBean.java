package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.DashboardGerencialService;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** REQ-0056 - Backing bean del dashboard gerencial (KPIs con filtros, solo lectura). */
@Named
@ViewScoped
public class DashboardGerencialBean implements Serializable {

    public static final String PANTALLA = "dashboard-gerencial";

    @Inject
    private transient DashboardGerencialService servicio;

    @Inject
    private transient CatalogoService catalogoService;

    @Inject
    private SesionUsuario sesion;

    private LocalDate desde;
    private LocalDate hasta;
    private Long monedaId;
    private Long sucursalId;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> monedas;
    private DashboardGerencialService.Kpis kpis;

    @PostConstruct
    public void iniciar() {
        LocalDate hoy = LocalDate.now();
        desde = hoy.withDayOfMonth(1);
        hasta = hoy;
        if (!sesion.puede(PANTALLA, "VER")) return;
        monedas = catalogoService.monedasActivas();
        if (monedas != null && !monedas.isEmpty()) {
            monedaId = monedas.stream()
                    .filter(m -> m.getDescripcion() != null && m.getDescripcion().toLowerCase().contains("guaran"))
                    .map(py.com.pysistemas.sginmo.dominio.catalogo.Moneda::getId)
                    .findFirst().orElse(monedas.get(0).getId());
        }
        recalcular();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void recalcular() {
        kpis = servicio.kpis(desde, hasta, monedaId, sucursalId);
    }

    public DashboardGerencialService.Kpis getKpis() { return kpis; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public Long getMonedaId() { return monedaId; }
    public void setMonedaId(Long monedaId) { this.monedaId = monedaId; }
    public Long getSucursalId() { return sucursalId; }
    public void setSucursalId(Long sucursalId) { this.sucursalId = sucursalId; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> getMonedas() { return monedas; }
}
