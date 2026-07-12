package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.DashboardGerencialService;
import py.com.pysistemas.sginmo.servicio.DashboardMetricasService;
import py.com.pysistemas.sginmo.servicio.RentabilidadService;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0056/0070 - Dashboard gerencial: KPIs + comparativos + graficos de evolucion y composicion.
 * Los graficos se dibujan con Chart.js (incluido en PrimeFaces 15); el bean expone los datos como JSON
 * con comillas simples (seguro en texto HTML) para el script del cliente.
 */
@Named
@ViewScoped
public class DashboardGerencialBean implements Serializable {

    public static final String PANTALLA = "dashboard-gerencial";

    @Inject private transient DashboardGerencialService servicio;
    @Inject private transient DashboardMetricasService metricas;
    @Inject private transient RentabilidadService rentabilidad;
    @Inject private transient CatalogoService catalogoService;
    @Inject private SesionUsuario sesion;

    private LocalDate desde;
    private LocalDate hasta;
    private Long monedaId;
    private Long sucursalId;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> monedas;
    private DashboardGerencialService.Kpis kpis;
    private List<DashboardMetricasService.Comparativo> comparativos;
    // JSON (comillas simples) para Chart.js:
    private String labelsJson = "[]";
    private String evolucionDatasetsJson = "[]";
    private String ingEgrDatasetsJson = "[]";
    private String distLabelsJson = "[]";
    private String distDataJson = "[]";
    private String distColoresJson = "[]";
    private boolean hayDistribucion;

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
        comparativos = metricas.comparativos(hasta, monedaId, sucursalId);
        construirGraficos();
    }

    private void construirGraficos() {
        List<DashboardMetricasService.Punto> sCob = metricas.serieMensual(DashboardMetricasService.COBROS, monedaId, sucursalId, 12);
        List<DashboardMetricasService.Punto> sIng = metricas.serieMensual(DashboardMetricasService.INGRESOS, monedaId, sucursalId, 12);
        List<DashboardMetricasService.Punto> sEgr = metricas.serieMensual(DashboardMetricasService.EGRESOS, monedaId, sucursalId, 12);
        List<String> labels = new ArrayList<>();
        for (DashboardMetricasService.Punto p : (sCob.isEmpty() ? (sIng.isEmpty() ? sEgr : sIng) : sCob)) labels.add(p.getEtiqueta());
        labelsJson = arrStr(labels);

        evolucionDatasetsJson = "["
            + dataset("Cobros", sCob, "rgba(42,157,143,.9)", true) + ","
            + dataset("Ingresos", sIng, "rgba(69,123,157,.9)", true) + ","
            + dataset("Egresos", sEgr, "rgba(230,57,70,.9)", true) + "]";
        ingEgrDatasetsJson = "["
            + dataset("Ingresos", sIng, "rgba(69,123,157,.8)", false) + ","
            + dataset("Egresos", sEgr, "rgba(230,57,70,.8)", false) + "]";

        List<String> dLabels = new ArrayList<>();
        List<String> dData = new ArrayList<>();
        List<String> dCol = new ArrayList<>();
        String[] paleta = {"#457b9d", "#2a9d8f", "#e9c46a", "#f4a261", "#e76f51", "#8d99ae", "#1d3557"};
        int i = 0;
        for (RentabilidadService.Linea l : rentabilidad.resumen(desde, hasta).getIngresos()) {
            if (l.isPasivo()) continue;
            dLabels.add(l.getEtiqueta());
            dData.add(l.getMonto().toPlainString());
            dCol.add(paleta[i++ % paleta.length]);
        }
        hayDistribucion = !dData.isEmpty();
        distLabelsJson = arrStr(dLabels);
        distDataJson = "[" + String.join(",", dData) + "]";
        distColoresJson = arrStr(dCol);
    }

    private String dataset(String label, List<DashboardMetricasService.Punto> serie, String color, boolean linea) {
        List<String> nums = new ArrayList<>();
        for (DashboardMetricasService.Punto p : serie) nums.add(p.getValor().toPlainString());
        String data = "[" + String.join(",", nums) + "]";
        if (linea) {
            return "{label:'" + esc(label) + "',data:" + data + ",borderColor:'" + color
                 + "',backgroundColor:'" + color + "',fill:false,tension:0.25}";
        }
        return "{label:'" + esc(label) + "',data:" + data + ",backgroundColor:'" + color + "'}";
    }

    /** Array JSON de strings con comillas simples (seguro en texto HTML: sin comillas dobles). */
    private static String arrStr(List<String> vals) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vals.size(); i++) { if (i > 0) sb.append(','); sb.append('\'').append(esc(vals.get(i))).append('\''); }
        return sb.append(']').toString();
    }
    private static String esc(String s) { return s == null ? "" : s.replace("\\", "").replace("'", "").replace("<", "").replace(">", ""); }

    public DashboardGerencialService.Kpis getKpis() { return kpis; }
    public List<DashboardMetricasService.Comparativo> getComparativos() { return comparativos; }
    public String getLabelsJson() { return labelsJson; }
    public String getEvolucionDatasetsJson() { return evolucionDatasetsJson; }
    public String getIngEgrDatasetsJson() { return ingEgrDatasetsJson; }
    public String getDistLabelsJson() { return distLabelsJson; }
    public String getDistDataJson() { return distDataJson; }
    public String getDistColoresJson() { return distColoresJson; }
    public boolean isHayDistribucion() { return hayDistribucion; }
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
