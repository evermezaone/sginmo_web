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

    /** Soporte: tenant que el SUPERADMIN eligio operar; null = opera como global (-1). */
    private Long override;

    /** Tenant REAL del usuario logueado (sin override); null si no hay sesion. SUPERADMIN = -1. */
    public Long tenantUsuario() {
        if (sesion == null || !sesion.isLogueado() || sesion.getUsuario() == null) {
            return null;
        }
        return sesion.getUsuario().getTenant();
    }

    /** true si el USUARIO es SUPERADMIN, independientemente del override de soporte. */
    public boolean esSuperadminReal() {
        return GLOBAL.equals(tenantUsuario());
    }

    /** Tenant EFECTIVO: el override de soporte (solo si el usuario es SUPERADMIN) o el propio. */
    public Long actual() {
        if (override != null && esSuperadminReal()) {
            return override;
        }
        return tenantUsuario();
    }

    /** true si el contexto EFECTIVO es global -1 (SUPERADMIN sin override): ve/edita registros -1. */
    public boolean esSuperadmin() {
        return GLOBAL.equals(actual());
    }

    /** El SUPERADMIN elige operar como un tenant (soporte); null o -1 vuelve a global. */
    public void operarComo(Long tenant) {
        if (!esSuperadminReal()) {
            throw new py.com.one.core.NegocioException("Solo el superadministrador puede cambiar de empresa");
        }
        override = (tenant == null || GLOBAL.equals(tenant)) ? null : tenant;
    }

    /** Vuelve al contexto global (-1). */
    public void volverAGlobal() {
        override = null;
    }

    /** Tenant que el SUPERADMIN esta operando por soporte, o null si esta en global. */
    public Long getOverride() {
        return override;
    }
}
