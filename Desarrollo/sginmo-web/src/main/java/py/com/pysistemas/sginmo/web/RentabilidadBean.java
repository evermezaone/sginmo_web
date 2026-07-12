package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.RentabilidadService;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** REQ-0071 - Rentabilidad e ingresos/egresos por tipo (base caja, moneda base). */
@Named
@ViewScoped
public class RentabilidadBean implements Serializable {

    public static final String PANTALLA = "rentabilidad";

    @Inject
    private transient RentabilidadService servicio;
    @Inject
    private SesionUsuario sesion;

    private LocalDate desde;
    private LocalDate hasta;
    private RentabilidadService.Resumen resumen;
    private List<RentabilidadService.ActivoRent> ranking;        // mejores
    private List<RentabilidadService.ActivoRent> rankingPeores;  // obs 278: peores

    @PostConstruct
    public void iniciar() {
        LocalDate hoy = LocalDate.now();
        desde = hoy.withDayOfMonth(1);
        hasta = hoy;
        if (!sesion.puede(PANTALLA, "VER")) return;
        recalcular();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void recalcular() {
        resumen = servicio.resumen(desde, hasta);
        ranking = servicio.rankingActivos(desde, hasta, 10, false);
        rankingPeores = servicio.rankingActivos(desde, hasta, 10, true);
    }

    public RentabilidadService.Resumen getResumen() { return resumen; }
    public List<RentabilidadService.ActivoRent> getRanking() { return ranking; }
    public List<RentabilidadService.ActivoRent> getRankingPeores() { return rankingPeores; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
}
