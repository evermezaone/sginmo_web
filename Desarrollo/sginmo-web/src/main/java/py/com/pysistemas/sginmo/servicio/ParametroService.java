package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.ParametroSistema;

import java.util.List;

/** ABM de parametros de configuracion (clave inmutable; sin borrado: son configuracion viva). */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class ParametroService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private ParametroConfig parametroConfig;   // REQ-0060: invalidar cache al guardar

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;   // obs 264: alcance por empresa

    @jakarta.inject.Inject
    private AuditoriaFuncionalService auditoria;   // obs 269: auditoria funcional visible

    public long contar(String filtro) {
        var q = em.createQuery("SELECT COUNT(p) FROM ParametroSistema p WHERE (:f = '' OR lower(p.clave) LIKE :like OR lower(p.descripcion) LIKE :like)", Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<ParametroSistema> listar(int primero, int cantidad, String filtro) {
        var q = em.createQuery("SELECT p FROM ParametroSistema p WHERE (:f = '' OR lower(p.clave) LIKE :like OR lower(p.descripcion) LIKE :like) ORDER BY p.clave", ParametroSistema.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    @Transactional
    public ParametroSistema guardar(ParametroSistema parametro, boolean esNuevo) {
        autorizacion.exigir("parametros", esNuevo ? "CREAR" : "EDITAR");
        if (parametro.getClave() == null || parametro.getClave().isBlank()) {
            throw new NegocioException("La clave es obligatoria");
        }
        if (parametro.getValor() == null) {
            parametro.setValor("");
        }
        if (parametro.getDescripcion() == null || parametro.getDescripcion().isBlank()) {
            throw new NegocioException("La descripción es obligatoria");
        }
        try {
            if (esNuevo) {
                // V26: PK compuesta (tenant, clave). Obs 264: el alta normal crea un OVERRIDE de la
                // empresa activa (tenant efectivo); el default global (tenant -1) queda reservado al
                // SUPERADMIN. Asi el ABM no pisa/crea defaults globales desde una empresa comun y se
                // respeta la prioridad "valor de empresa sobre default global" (REQ-0060/ParametroConfig).
                Long emp = tenant.actual();
                Long objetivo = parametro.getTenant();
                if (objetivo == null) {
                    objetivo = (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp))
                            ? py.com.pysistemas.sginmo.web.TenantContext.GLOBAL : emp;
                }
                if (py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(objetivo) && !tenant.esSuperadmin()) {
                    throw new NegocioException("Solo un administrador global puede crear parametros por defecto. "
                            + "Seleccione una empresa para crear un valor propio.");
                }
                if (!tenant.esSuperadmin() && emp != null && !emp.equals(objetivo)) {
                    throw new NegocioException("No puede crear parametros de otra empresa");
                }
                parametro.setTenant(objetivo);
                parametro.setClave(parametro.getClave().trim().toUpperCase());
                var pk = new py.com.pysistemas.sginmo.dominio.catalogo.ParametroSistemaId(
                        parametro.getTenant(), parametro.getClave());
                if (em.find(ParametroSistema.class, pk) != null) {
                    throw new NegocioException("Ya existe el parámetro '" + parametro.getClave() + "'");
                }
                em.persist(parametro);
            } else {
                // obs 269: snapshot del valor previo para el diff de auditoria
                ParametroSistema o = em.find(ParametroSistema.class,
                        new py.com.pysistemas.sginmo.dominio.catalogo.ParametroSistemaId(
                                parametro.getTenant(), parametro.getClave()));
                java.util.Map<String, Object> antes = o == null ? null : snap(o);
                parametro = em.merge(parametro);
                em.flush();
                if (antes != null) {
                    auditoria.registrarCambios("parametro_sistema", parametro.getClave(), "parametros", null, antes, snap(parametro));
                }
                parametroConfig.invalidar();   // REQ-0060: refresca la config cacheada
                return parametro;
            }
            em.flush();
            auditoria.registrarAlta("parametro_sistema", parametro.getClave(), "parametros");   // obs 269
            parametroConfig.invalidar();   // REQ-0060: refresca la config cacheada
            return parametro;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("El parámetro fue modificado por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    /** Snapshot de campos auditables del parametro (obs 269). */
    private static java.util.Map<String, Object> snap(ParametroSistema p) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("valor", p.getValor());
        m.put("descripcion", p.getDescripcion());
        return m;
    }
}
