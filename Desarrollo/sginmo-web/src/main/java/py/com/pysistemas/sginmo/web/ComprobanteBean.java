package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.ComprobanteService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;

/** REQ-0058 - Recibos/comprobantes: lista de cobros y descarga/reimpresion del recibo PDF. */
@Named
@ViewScoped
public class ComprobanteBean implements Serializable {

    public static final String PANTALLA = "comprobantes";

    @Inject
    private transient ComprobanteService servicio;

    @Inject
    private SesionUsuario sesion;

    private List<ComprobanteService.FilaCobro> cobros;
    private String filtroEstado;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        buscar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void buscar() {
        cobros = servicio.cobrosRecientes(filtroEstado);
    }

    /** Descarga/reimpresion del recibo (regenerado desde el cobro persistido). Stream perezoso. */
    public StreamedContent reciboCobro(ComprobanteService.FilaCobro fc) {
        final Long id = fc.getCobro();
        return DefaultStreamedContent.builder()
                .name("recibo_" + id + ".pdf")
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(servicio.reciboCobro(id)))
                .build();
    }

    public List<String> getEstados() { return List.of("ACTIVO", "ANULADO"); }

    public List<ComprobanteService.FilaCobro> getCobros() { return cobros; }
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String v) { this.filtroEstado = v; }
}
