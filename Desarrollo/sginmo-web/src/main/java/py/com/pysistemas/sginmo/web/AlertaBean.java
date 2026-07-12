package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.AlertaService;

import java.io.Serializable;
import java.util.List;

/** REQ-0075 - Alertas gerenciales: lista accionable con evidencia y descarte auditado. */
@Named
@ViewScoped
public class AlertaBean implements Serializable {

    public static final String PANTALLA = "alertas";

    @Inject
    private transient AlertaService servicio;
    @Inject
    private SesionUsuario sesion;

    private List<AlertaService.Alerta> alertas;
    private AlertaService.Alerta seleccionada;
    private String motivo;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        recalcular();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    /** Regenera (idempotente) y recarga las alertas. */
    public void recalcular() {
        try { servicio.generar(); } catch (RuntimeException ignore) { /* generar no debe romper la vista */ }
        alertas = servicio.listar();
    }

    public void prepararDescartar(AlertaService.Alerta a) { seleccionada = a; motivo = null; }

    public void descartar() {
        try {
            servicio.cerrar(seleccionada.getId(), "DESCARTADA", motivo);
            aviso(FacesMessage.SEVERITY_INFO, "Alerta descartada", null);
            seleccionada = null; alertas = servicio.listar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo descartar", e.getMessage());
        }
    }

    public void revisar(AlertaService.Alerta a) {
        try {
            servicio.cerrar(a.getId(), "REVISADA", null);
            alertas = servicio.listar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo marcar revisada", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public List<AlertaService.Alerta> getAlertas() { return alertas; }
    public AlertaService.Alerta getSeleccionada() { return seleccionada; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
