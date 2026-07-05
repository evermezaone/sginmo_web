package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.pysistemas.sginmo.dominio.catalogo.Articulo;

import java.util.List;

/**
 * ABM del maestro de articulos (patron propuesto para todos los ABM del sistema).
 * Reglas de negocio y validaciones EN EL SERVICIO (estandar backend-jakarta.md);
 * el bean JSF solo orquesta la UI.
 */
@ApplicationScoped
public class ArticuloService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    // ── Consultas (paginacion lazy para p:dataTable) ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(a) FROM Articulo a WHERE (:f = '' OR lower(a.codigo) LIKE :like OR lower(a.descripcion) LIKE :like)",
            Long.class);
        aplicarFiltro(q, filtro);
        return q.getSingleResult();
    }

    public List<Articulo> listar(int primero, int cantidad, String filtro) {
        var q = em.createQuery(
            "SELECT a FROM Articulo a WHERE (:f = '' OR lower(a.codigo) LIKE :like OR lower(a.descripcion) LIKE :like) ORDER BY a.descripcion",
            Articulo.class);
        aplicarFiltro(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void aplicarFiltro(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f);
        q.setParameter("like", "%" + f + "%");
    }

    // ── Escrituras ──

    @Transactional
    public Articulo guardar(Articulo articulo) {
        validar(articulo);
        if (articulo.getId() == null) {
            em.persist(articulo);
            return articulo;
        }
        return em.merge(articulo);
    }

    /** Baja/alta logica: los maestros nunca se borran fisicamente (conservan historial). */
    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        Articulo a = em.find(Articulo.class, id);
        if (a == null) {
            throw new NegocioException("El artículo no existe");
        }
        a.setEstado(estadoNuevo);
    }

    private void validar(Articulo a) {
        // Unicidad de codigo (ademas del UNIQUE de la BD, para dar mensaje claro)
        Long repetidos = em.createQuery(
                "SELECT COUNT(x) FROM Articulo x WHERE lower(x.codigo) = :codigo AND (:id IS NULL OR x.id <> :id)",
                Long.class)
            .setParameter("codigo", a.getCodigo().trim().toLowerCase())
            .setParameter("id", a.getId())
            .getSingleResult();
        if (repetidos > 0) {
            throw new NegocioException("Ya existe un artículo con el código '" + a.getCodigo() + "'");
        }
        // Unicidad de aplicacion (clave funcional del negocio)
        if (a.getAplicacion() != null && !a.getAplicacion().isBlank()) {
            Long apl = em.createQuery(
                    "SELECT COUNT(x) FROM Articulo x WHERE x.aplicacion = :apl AND (:id IS NULL OR x.id <> :id)",
                    Long.class)
                .setParameter("apl", a.getAplicacion().trim())
                .setParameter("id", a.getId())
                .getSingleResult();
            if (apl > 0) {
                throw new NegocioException("La aplicación '" + a.getAplicacion() + "' ya está asignada a otro artículo");
            }
        }
        if (a.getStockMinimo() != null && a.getStockMaximo() != null
                && a.getStockMinimo().compareTo(a.getStockMaximo()) > 0) {
            throw new NegocioException("El stock mínimo no puede ser mayor que el máximo");
        }
    }
}
