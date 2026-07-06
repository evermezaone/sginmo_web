package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import py.com.pysistemas.sginmo.servicio.NegocioException;
import py.com.pysistemas.sginmo.servicio.SeguridadService;

/** Formulario de login (REQ-0004). */
@Named
@RequestScoped
public class LoginBean {

    @Inject
    private SeguridadService seguridadService;

    @Inject
    private SesionUsuario sesion;

    private String codigo;
    private String password;

    public String entrar() {
        try {
            var usuario = seguridadService.autenticar(codigo, password);
            // renovar el id de sesion al autenticar (evita fijacion de sesion)
            var ctx = FacesContext.getCurrentInstance().getExternalContext();
            ((HttpServletRequest) ctx.getRequest()).changeSessionId();
            sesion.iniciar(usuario, seguridadService.permisosDe(usuario.getId()));
            return "/index?faces-redirect=true";
        } catch (NegocioException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, e.getMessage(), null));
            return null;
        }
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
