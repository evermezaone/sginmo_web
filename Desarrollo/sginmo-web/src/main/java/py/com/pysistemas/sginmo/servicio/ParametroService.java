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
public class ParametroService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

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
                if (em.find(ParametroSistema.class, parametro.getClave().trim()) != null) {
                    throw new NegocioException("Ya existe el parámetro '" + parametro.getClave() + "'");
                }
                parametro.setClave(parametro.getClave().trim().toUpperCase());
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
