package py.com.pysistemas.sginmo.servicio;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Fija app.tenant en la sesion PostgreSQL al inicio de cada metodo @AislarTenant, para que
 * las politicas RLS (V28) aíslen por tenant. Usa set_config(..., true) = SET LOCAL: el valor
 * dura solo la transaccion en curso y se descarta al commit/rollback (seguro con pool de
 * conexiones; no se filtra entre requests).
 *
 * Prioridad PLATFORM_BEFORE+300: corre DESPUES (mas adentro) del interceptor de @Transactional
 * (PLATFORM_BEFORE+200), asi que ya hay una transaccion activa cuando se ejecuta el SET LOCAL.
 * SUPERADMIN => app.tenant = -1 (las politicas le dan acceso total). Sin sesion (batch) no se
 * fija nada y las tablas con RLS quedan fail-closed.
 */
@AislarTenant
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 300)
public class TenantInterceptor {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    @AroundInvoke
    public Object aislar(InvocationContext ctx) throws Exception {
        Long t = tenant.actual();
        if (t != null) {
            em.createNativeQuery("SELECT set_config('app.tenant', :t, true)")
                .setParameter("t", String.valueOf(t))
                .getSingleResult();
        }
        return ctx.proceed();
    }
}
