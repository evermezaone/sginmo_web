package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.ReportesConsultaService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** REQ-0062 - Reportes exportables PDF/CSV (sin Jasper). */
@Named
@ViewScoped
public class ReportesBean implements Serializable {

    public static final String PANTALLA = "reportes";

    @Inject
    private transient ReportesConsultaService servicio;
    @Inject
    private transient CatalogoService catalogoService;
    @Inject
    private SesionUsuario sesion;

    private String tipo = "PROPIEDADES";
    private LocalDate desde;
    private LocalDate hasta;
    private Long monedaId;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> monedas;

    @PostConstruct
    public void iniciar() {
        LocalDate hoy = LocalDate.now();
        desde = hoy.withDayOfMonth(1);
        hasta = hoy;
        if (sesion.puede(PANTALLA, "VER")) monedas = catalogoService.monedasActivas();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public StreamedContent getPdf() {
        return DefaultStreamedContent.builder()
                .name("reporte_" + tipo.toLowerCase() + ".pdf")
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(servicio.pdf(servicio.generar(tipo, desde, hasta, monedaId))))
                .build();
    }

    public StreamedContent getCsv() {
        return DefaultStreamedContent.builder()
                .name("reporte_" + tipo.toLowerCase() + ".csv")
                .contentType("text/csv")
                .stream(() -> new ByteArrayInputStream(servicio.csv(servicio.generar(tipo, desde, hasta, monedaId))))
                .build();
    }

    public List<String> getTipos() { return List.of("PROPIEDADES", "COBROS", "MORA"); }
    public boolean isRequierePeriodo() { return "COBROS".equals(tipo); }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate v) { this.desde = v; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate v) { this.hasta = v; }
    public Long getMonedaId() { return monedaId; }
    public void setMonedaId(Long v) { this.monedaId = v; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> getMonedas() { return monedas; }
}
