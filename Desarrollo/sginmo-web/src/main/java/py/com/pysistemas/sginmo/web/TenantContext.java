package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;

import java.io.Serializable;

/**
 * Contexto de tenant multiempresa (F4). Expone el tenant del usuario logueado para
 * que los services aislen los datos: catalogos se leen con tenant IN (-1, actual);
 * lo transaccional con tenant = actual. -1 = SUPERADMIN (global).
 *
 * Es un CDI bean session-scoped inyectable tanto en beans web como en services
 * @ApplicationScoped (via proxy). No mantiene estado propio: deriva del usuario.
 */
@Named
@SessionScoped
public class TenantContext implements Serializable {

    public static final Long GLOBAL = -1L;

    @Inject
    private SesionUsuario sesion;

    /** Tenant (empresa) del usuario logueado; null si no hay sesion. SUPERADMIN = -1. */
    public Long actual() {
        if (sesion == null || !sesion.isLogueado() || sesion.getUsuario() == null) {
            return null;
        }
        return sesion.getUsuario().getTenant();
    }

    /** true si el usuario es SUPERADMIN (tenant global -1): ve/edita registros -1. */
    public boolean esSuperadmin() {
        return GLOBAL.equals(actual());
    }
}
