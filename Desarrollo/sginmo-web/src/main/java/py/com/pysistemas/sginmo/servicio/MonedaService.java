package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.Moneda;

import java.util.List;
import java.util.Map;

/** ABM de monedas (contrato estandar). */
@ApplicationScoped
public class MonedaService {

    private static final Map<String, String> ORDEN = Map.of(
        "descripcion", "m.descripcion", "simbolo", "m.simbolo",
        "tipoMoneda", "m.tipoMoneda", "estado", "m.estado");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    public long contar(String filtro) {
        var q = em.createQuery("SELECT COUNT(m) FROM Moneda m WHERE (:f = '' OR lower(m.descripcion) LIKE :like OR lower(m.simbolo) LIKE :like)", Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<Moneda> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery("SELECT m FROM Moneda m WHERE (:f = '' OR lower(m.descripcion) LIKE :like OR lower(m.simbolo) LIKE :like) ORDER BY "
                + (ruta == null ? "m.descripcion" : ruta) + (asc ? " ASC" : " DESC"), Moneda.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    public boolean existeDescripcion(String descripcion, Long exceptoId) {
        if (descripcion == null || descripcion.isBlank()) return false;
        return em.createQuery("SELECT COUNT(m) FROM Moneda m WHERE lower(m.descripcion) = :d AND (:id IS NULL OR m.id <> :id)", Long.class)
            .setParameter("d", descripcion.trim().toLowerCase()).setParameter("id", exceptoId)
            .getSingleResult() > 0;
    }

    @Transactional
    public Moneda guardar(Moneda moneda) {
        autorizacion.exigir("monedas", moneda.getId() == null ? "CREAR" : "EDITAR");
        validar(moneda);
        try {
            Moneda r = moneda.getId() == null ? persistir(moneda) : em.merge(moneda);
            em.flush();
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La moneda fue modificada por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private Moneda persistir(Moneda m) { em.persist(m); return m; }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        autorizacion.exigir("monedas", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        Moneda m = em.find(Moneda.class, id);
        if (m == null) throw new NegocioException("La moneda no existe");
        if ("ACTIVO".equals(estadoNuevo)) validar(m);
        m.setEstado(estadoNuevo);
    }

    private void validar(Moneda m) {
        if (existeDescripcion(m.getDescripcion(), m.getId())) {
            throw new NegocioException("Ya existe una moneda '" + m.getDescripcion() + "'");
        }
        if (m.getPrecisionDecimales() == null || m.getPrecisionDecimales() < 0 || m.getPrecisionDecimales() > 6) {
            throw new NegocioException("La precisión decimal debe estar entre 0 y 6");
        }
    }
}
