package py.com.one.security.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.servicio.SeguridadService;

/** Cambio de contrasena del propio usuario (REQ-0004): voluntario u obligatorio (alta/reseteo). */
@Named
@RequestScoped
public class CambioPasswordBean {

    @Inject
    private SeguridadService seguridadService;

    @Inject
    private SesionUsuario sesion;

    private String actual;
    private String nueva;
    private String repetida;

    public String cambiar() {
        try {
            var actualizado = seguridadService.cambiarPassword(
                    sesion.getUsuario().getId(), actual, nueva, repetida);
            // refrescar la copia en sesion (flag debe_cambiar_password)
            sesion.getUsuario().setDebeCambiarPassword(actualizado.getDebeCambiarPassword());
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Contraseña actualizada", null));
            return "/index?faces-redirect=true";
        } catch (NegocioException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, e.getMessage(), null));
            return null;
        }
    }

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getNueva() { return nueva; }
    public void setNueva(String nueva) { this.nueva = nueva; }

    public String getRepetida() { return repetida; }
    public void setRepetida(String repetida) { this.repetida = repetida; }
}
