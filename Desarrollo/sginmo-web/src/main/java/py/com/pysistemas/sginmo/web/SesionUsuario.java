package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import py.com.pysistemas.sginmo.dominio.base.UsuarioActual;
import py.com.pysistemas.sginmo.dominio.seguridad.Usuario;

import java.io.Serializable;

/**
 * Estado de la sesion web (REQ-0004): usuario autenticado.
 * Implementa UsuarioActual: la auditoria de entidades registra automaticamente
 * el codigo del usuario logueado (fallback "sistema" fuera de una sesion).
 */
@Named
@SessionScoped
public class SesionUsuario implements UsuarioActual, Serializable {

    private Usuario usuario;

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
    }

    public boolean isLogueado() {
        return usuario != null;
    }

    public Usuario getUsuario() { return usuario; }

    public boolean isAdministrador() {
        return usuario != null && "ADMINISTRADOR".equals(usuario.getPerfil());
    }

    @Override
    public String codigoUsuario() {
        return usuario != null ? usuario.getCodigoUsuario() : SISTEMA;
    }

    /** Cierra la sesion e invalida todo; vuelve al login. */
    public String cerrarSesion() {
        usuario = null;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login?faces-redirect=true";
    }
}
