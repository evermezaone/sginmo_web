package py.com.one.security.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import py.com.one.core.UsuarioActual;
import py.com.one.security.dominio.Usuario;

import java.io.Serializable;

/**
 * Estado de la sesion web (REQ-0004): usuario autenticado.
 * Implementa UsuarioActual: la auditoria de entidades registra automaticamente
 * el codigo del usuario logueado (fallback "sistema" fuera de una sesion).
 */
@Named
@SessionScoped
public class SesionUsuario implements UsuarioActual, Serializable {

    /** Tenant global -1 = SUPERADMIN. */
    public static final Long GLOBAL = -1L;

    private Usuario usuario;

    /** Soporte (F6, obs 260): tenant que el SUPERADMIN eligio "operar como"; null = global. Es la
     *  UNICA fuente del override; TenantContext (SGInmo) delega aca, de modo que services (via el
     *  interceptor) Y los ABM de seguridad comparten el MISMO tenant efectivo. */
    private Long overrideTenant;

    /** Permisos explicitos "pantalla:accion" cargados al iniciar sesion (perfil USUARIO). */
    private java.util.Set<String> permisos = java.util.Set.of();

    public void iniciar(Usuario usuario, java.util.Set<String> permisos) {
        this.usuario = usuario;
        this.permisos = permisos == null ? java.util.Set.of() : permisos;
        this.overrideTenant = null;
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

    /** Tenant REAL del usuario logueado (sin override); -1 = SUPERADMIN. null si no hay sesion. */
    public Long tenantUsuario() {
        return usuario != null ? usuario.getTenant() : null;
    }

    /** true si el USUARIO es SUPERADMIN (independiente del override de soporte). */
    public boolean esSuperadminReal() {
        return GLOBAL.equals(tenantUsuario());
    }

    /** Tenant EFECTIVO: el override de soporte (solo si el usuario es SUPERADMIN) o el propio.
     *  Los ABM de seguridad y los services deben usar ESTE valor (obs 260). */
    public Long tenantActual() {
        if (overrideTenant != null && esSuperadminReal()) {
            return overrideTenant;
        }
        return tenantUsuario();
    }

    /** El SUPERADMIN elige operar como un tenant (soporte); null o -1 vuelve a global. */
    public void operarComo(Long tenant) {
        if (!esSuperadminReal()) {
            throw new py.com.one.core.NegocioException("Solo el superadministrador puede cambiar de empresa");
        }
        overrideTenant = (tenant == null || GLOBAL.equals(tenant)) ? null : tenant;
    }

    public void volverAGlobal() { overrideTenant = null; }

    /** Tenant que el SUPERADMIN esta operando por soporte, o null si esta en global. */
    public Long getOverrideTenant() { return overrideTenant; }

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
