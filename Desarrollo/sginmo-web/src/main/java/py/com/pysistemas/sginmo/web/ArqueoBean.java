package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.dominio.operacion.Planilla;
import py.com.pysistemas.sginmo.servicio.ArqueoService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** REQ-0059 - Arqueo y cierre controlado de caja (sobre la planilla existente). */
@Named
@ViewScoped
public class ArqueoBean implements Serializable {

    public static final String PANTALLA = "arqueo";

    @Inject
    private transient ArqueoService servicio;
    @Inject
    private SesionUsuario sesion;

    private List<Planilla> planillas;
    private Planilla seleccionada;
    private ArqueoService.ResumenArqueo resumen;
    private BigDecimal efectivoContado;
    private String observacion;
    private String motivoReapertura;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        buscar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void buscar() { planillas = servicio.planillasRecientes(); }

    public void abrirArqueo(Planilla p) {
        seleccionada = p;
        resumen = servicio.resumen(p.getId());
        efectivoContado = null;
        observacion = null;
    }

    public void prepararReapertura(Planilla p) {
        seleccionada = p;
        motivoReapertura = null;
    }

    public void cerrar() {
        try {
            servicio.cerrarConArqueo(seleccionada.getId(), efectivoContado, observacion);
            aviso(FacesMessage.SEVERITY_INFO, "Caja cerrada con arqueo", "Planilla " + seleccionada.getId());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgArqueo').hide()");
            buscar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cerrar", e.getMessage());
        }
    }

    public void reabrir() {
        try {
            servicio.reabrir(seleccionada.getId(), motivoReapertura);
            aviso(FacesMessage.SEVERITY_INFO, "Caja reabierta", "Planilla " + seleccionada.getId());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgReabrir').hide()");
            buscar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo reabrir", e.getMessage());
        }
    }

    public StreamedContent arqueoPdf(Planilla p) {
        final Long id = p.getId();
        return DefaultStreamedContent.builder()
                .name("arqueo_" + id + ".pdf")
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(servicio.arqueoPdf(id)))
                .build();
    }

    private void aviso(FacesMessage.Severity sev, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, t, d));
    }

    public List<Planilla> getPlanillas() { return planillas; }
    public Planilla getSeleccionada() { return seleccionada; }
    public ArqueoService.ResumenArqueo getResumen() { return resumen; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal v) { this.efectivoContado = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { this.observacion = v; }
    public String getMotivoReapertura() { return motivoReapertura; }
    public void setMotivoReapertura(String v) { this.motivoReapertura = v; }
}
