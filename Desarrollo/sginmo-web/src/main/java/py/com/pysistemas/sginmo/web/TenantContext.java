package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;

import java.io.Serializable;

/**
 * Contexto de tenant multiempresa (F4). Expone el tenant efectivo para que los services
 * aislen los datos: catalogos se leen con tenant IN (-1, actual); lo transaccional con
 * tenant = actual. -1 = SUPERADMIN (global).
 *
 * F6 (selector de soporte): el SUPERADMIN puede "operar como" un tenant concreto; mientras
 * dura ese override, actual() devuelve ese tenant y el sistema entero (services + RLS via el
 * interceptor) queda acotado a esa empresa. Para un usuario NO superadmin el override es
 * inaplicable (operarComo lanza), asi que su comportamiento es identico al de F4.
 *
 * Es un CDI bean session-scoped inyectable tanto en beans web como en services
 * @ApplicationScoped (via proxy).
 */
@Named
@SessionScoped
public class TenantContext implements Serializable {

    public static final Long GLOBAL = -1L;

    @Inject
    private SesionUsuario sesion;

    /**
     * REQ-0078: sesion del portal externo. Cuando NO hay login administrativo pero SI un socio
     * autenticado en el portal, el tenant efectivo es el de su empresa, de modo que los services
     * @AislarTenant (via el interceptor) acoten la RLS a esa empresa. El portal jamas ve -1.
     */
    @Inject
    private PortalSesion portal;

    private boolean sinSesion() {
        return sesion == null || !sesion.isLogueado() || sesion.getUsuario() == null;
    }

    private boolean portalActivo() {
        return sinSesion() && portal != null && portal.isAutenticado();
    }

    /** Tenant REAL del usuario logueado (sin override); null si no hay sesion. SUPERADMIN = -1. */
    public Long tenantUsuario() {
        if (portalActivo()) return portal.getTenant();
        return sinSesion() ? null : sesion.tenantUsuario();
    }

    /** true si el USUARIO es SUPERADMIN, independientemente del override de soporte. */
    public boolean esSuperadminReal() {
        return !sinSesion() && sesion.esSuperadminReal();
    }

    /**
     * Tenant EFECTIVO. El override de soporte vive en SesionUsuario (unica fuente, obs 260): asi
     * los services (via el interceptor @AislarTenant) Y los ABM de seguridad usan el MISMO valor.
     */
    public Long actual() {
        if (portalActivo()) return portal.getTenant();
        return sinSesion() ? null : sesion.tenantActual();
    }

    /** true si el contexto EFECTIVO es global -1 (SUPERADMIN sin override): ve/edita registros -1. */
    public boolean esSuperadmin() {
        return GLOBAL.equals(actual());
    }

    /** El SUPERADMIN elige operar como un tenant (soporte); null o -1 vuelve a global. */
    public void operarComo(Long tenant) {
        sesion.operarComo(tenant);
    }

    /** Vuelve al contexto global (-1). */
    public void volverAGlobal() {
        sesion.volverAGlobal();
    }

    /** Tenant que el SUPERADMIN esta operando por soporte, o null si esta en global. */
    public Long getOverride() {
        return sinSesion() ? null : sesion.getOverrideTenant();
    }
}
