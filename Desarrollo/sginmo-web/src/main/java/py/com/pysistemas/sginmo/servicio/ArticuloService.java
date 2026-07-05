package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.pysistemas.sginmo.dominio.catalogo.Articulo;

import java.util.List;
import java.util.Map;

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

    /** Rutas JPQL permitidas para ordenar (clave = field de la columna en la vista). */
    private static final Map<String, String> CAMPOS_ORDEN = Map.of(
        "codigo", "a.codigo",
        "descripcion", "a.descripcion",
        "tipo", "a.tipo",
        "impuesto.descripcion", "i.descripcion",
        "precioUnitario", "a.precioUnitario",
        "estado", "a.estado");

    /** Rutas JPQL permitidas para filtrar por columna (igualdad exacta, combos). */
    private static final Map<String, String> CAMPOS_FILTRO = Map.of(
        "tipo", "a.tipo",
        "impuesto.descripcion", "i.descripcion",
        "estado", "a.estado");

    public long contar(String filtro, Map<String, Object> filtros) {
        var q = em.createQuery(jpql("SELECT COUNT(a)", filtros, null, true), Long.class);
        aplicarParametros(q, filtro, filtros);
        return q.getSingleResult();
    }

    public List<Articulo> listar(int primero, int cantidad, String filtro,
                                 Map<String, Object> filtros, String ordenarPor, boolean ascendente) {
        var q = em.createQuery(jpql("SELECT a", filtros, ordenarPor, ascendente), Articulo.class);
        aplicarParametros(q, filtro, filtros);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    /** El filtro global busca en codigo, descripcion, tipo e impuesto; solo se ordena/filtra por rutas whitelisted. */
    private String jpql(String select, Map<String, Object> filtros, String ordenarPor, boolean ascendente) {
        var sb = new StringBuilder(select)
            .append(" FROM Articulo a LEFT JOIN a.impuesto i")
            .append(" WHERE (:f = '' OR lower(a.codigo) LIKE :like OR lower(a.descripcion) LIKE :like")
            .append(" OR lower(a.tipo) LIKE :like OR lower(coalesce(i.descripcion, '')) LIKE :like)");
        int n = 0;
        for (String campo : filtros.keySet()) {
            String ruta = CAMPOS_FILTRO.get(campo);
            if (ruta != null) {
                sb.append(" AND ").append(ruta).append(" = :fc").append(n++);
            }
        }
        if (select.startsWith("SELECT a")) {
            String ruta = ordenarPor == null ? null : CAMPOS_ORDEN.get(ordenarPor);
            sb.append(" ORDER BY ").append(ruta == null ? "a.descripcion" : ruta)
              .append(ascendente ? " ASC" : " DESC");
        }
        return sb.toString();
    }

    private void aplicarParametros(jakarta.persistence.TypedQuery<?> q, String filtro, Map<String, Object> filtros) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f);
        q.setParameter("like", "%" + f + "%");
        int n = 0;
        for (var e : filtros.entrySet()) {
            if (CAMPOS_FILTRO.containsKey(e.getKey())) {
                q.setParameter("fc" + n++, e.getValue());
            }
        }
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

    // ── Propiedades parametrizables del articulo (solo con articulo ya guardado) ──

    public List<py.com.pysistemas.sginmo.dominio.catalogo.ArticuloPropiedad> listarPropiedades(Long articuloId) {
        return em.createQuery(
                "SELECT p FROM ArticuloPropiedad p WHERE p.articulo = :art ORDER BY p.propiedadCodigo",
                py.com.pysistemas.sginmo.dominio.catalogo.ArticuloPropiedad.class)
            .setParameter("art", articuloId)
            .getResultList();
    }

    @Transactional
    public void agregarPropiedad(Long articuloId, String codigo, String valor) {
        if (articuloId == null) {
            throw new NegocioException("Guarde el artículo antes de cargar propiedades");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("Debe elegir la propiedad");
        }
        Long repetidas = em.createQuery(
                "SELECT COUNT(p) FROM ArticuloPropiedad p WHERE p.articulo = :art AND p.propiedadCodigo = :cod",
                Long.class)
            .setParameter("art", articuloId)
            .setParameter("cod", codigo)
            .getSingleResult();
        if (repetidas > 0) {
            throw new NegocioException("El artículo ya tiene cargada esa propiedad");
        }
        var p = new py.com.pysistemas.sginmo.dominio.catalogo.ArticuloPropiedad();
        p.setArticulo(articuloId);
        p.setPropiedadCodigo(codigo);
        p.setValor(valor);
        em.persist(p);
    }

    @Transactional
    public void eliminarPropiedad(Long propiedadId) {
        var p = em.find(py.com.pysistemas.sginmo.dominio.catalogo.ArticuloPropiedad.class, propiedadId);
        if (p != null) {
            em.remove(p);
        }
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
