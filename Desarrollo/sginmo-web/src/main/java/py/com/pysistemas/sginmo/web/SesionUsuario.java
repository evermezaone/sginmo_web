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

    /** Permisos explicitos "pantalla:accion" cargados al iniciar sesion (perfil USUARIO). */
    private java.util.Set<String> permisos = java.util.Set.of();

    public void iniciar(Usuario usuario, java.util.Set<String> permisos) {
        this.usuario = usuario;
        this.permisos = permisos == null ? java.util.Set.of() : permisos;
    }

    /**
     * Autorizacion por accion (REQ-0004): ADMINISTRADOR todo; USUARIO solo permisos
     * explicitos (pantalla exacta o comodin '*'). OPERAR cubre todas las acciones del
     * ABM salvo VER_AUDITORIA, que siempre requiere permiso explicito (decision del
     * usuario 2026-07-05).
     */
    public boolean puede(String pantalla, String accion) {
        if (usuario == null) {
            return false;
        }
        if (isAdministrador()) {
            return true;
        }
        if (permisos.contains(pantalla + ":" + accion) || permisos.contains("*:" + accion)) {
            return true;
        }
        return !"VER_AUDITORIA".equals(accion)
                && (permisos.contains(pantalla + ":OPERAR") || permisos.contains("*:OPERAR"));
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
