package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.SeguridadPoliticaService;

import java.io.Serializable;
import java.util.List;

/** REQ-0064 - Politicas de seguridad: ver politicas, desbloquear usuarios y auditoria de accesos. */
@Named
@ViewScoped
public class SeguridadBean implements Serializable {

    public static final String PANTALLA = "seguridad";

    @Inject
    private transient SeguridadPoliticaService servicio;
    @Inject
    private SesionUsuario sesion;

    private List<SeguridadPoliticaService.Fila> politicas;
    private List<SeguridadPoliticaService.Fila> bloqueados;
    private List<SeguridadPoliticaService.Fila> eventos;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        recargar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void recargar() {
        politicas = servicio.politicas();
        bloqueados = servicio.usuariosBloqueados();
        eventos = servicio.eventosRecientes();
    }

    public void desbloquear(SeguridadPoliticaService.Fila u) {
        try {
            servicio.desbloquear(u.getId());
            aviso(FacesMessage.SEVERITY_INFO, "Usuario desbloqueado", String.valueOf(u.getB()));
            recargar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo desbloquear", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity sev, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, t, d));
    }

    public List<SeguridadPoliticaService.Fila> getPoliticas() { return politicas; }
    public List<SeguridadPoliticaService.Fila> getBloqueados() { return bloqueados; }
    public List<SeguridadPoliticaService.Fila> getEventos() { return eventos; }
}
