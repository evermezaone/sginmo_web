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
                // V26: PK compuesta (tenant, clave). TODO(F6): el tenant de un parametro propio
                // sale del contexto; por ahora los defaults viven en el tenant global -1.
                if (parametro.getTenant() == null) parametro.setTenant(-1L);
                parametro.setClave(parametro.getClave().trim().toUpperCase());
                var pk = new py.com.pysistemas.sginmo.dominio.catalogo.ParametroSistemaId(
                        parametro.getTenant(), parametro.getClave());
                if (em.find(ParametroSistema.class, pk) != null) {
                    throw new NegocioException("Ya existe el parámetro '" + parametro.getClave() + "'");
                }
                em.persist(parametro);
            } else {
                parametro = em.merge(parametro);
            }
            em.flush();
            return parametro;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("El parámetro fue modificado por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }
}
