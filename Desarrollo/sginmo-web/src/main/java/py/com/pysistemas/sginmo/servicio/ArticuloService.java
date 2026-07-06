package py.com.pysistemas.sginmo.servicio;

import py.com.one.core.NegocioException;
import py.com.one.core.ErroresBd;
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

    /** Rutas JPQL permitidas para filtrar por columna con igualdad exacta (combos). */
    private static final Map<String, String> CAMPOS_FILTRO_IGUAL = Map.of(
        "tipo", "a.tipo",
        "impuesto.descripcion", "i.descripcion",
        "estado", "a.estado");

    /** Rutas JPQL permitidas para filtrar por columna con LIKE (campos de texto libre). */
    private static final Map<String, String> CAMPOS_FILTRO_LIKE = Map.of(
        "codigo", "a.codigo",
        "descripcion", "a.descripcion");

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
            String igual = CAMPOS_FILTRO_IGUAL.get(campo);
            String like = CAMPOS_FILTRO_LIKE.get(campo);
            if (igual != null) {
                sb.append(" AND ").append(igual).append(" = :fc").append(n++);
            } else if (like != null) {
                sb.append(" AND lower(").append(like).append(") LIKE :fc").append(n++);
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
            if (CAMPOS_FILTRO_IGUAL.containsKey(e.getKey())) {
                q.setParameter("fc" + n++, e.getValue());
            } else if (CAMPOS_FILTRO_LIKE.containsKey(e.getKey())) {
                q.setParameter("fc" + n++, "%" + e.getValue().toString().trim().toLowerCase() + "%");
            }
        }
    }

    // ── Escrituras ──

    @Transactional
    public Articulo guardar(Articulo articulo) {
        validar(articulo);
        try {
            Articulo resultado;
            if (articulo.getId() == null) {
                em.persist(articulo);
                resultado = articulo;
            } else {
                resultado = em.merge(articulo);
            }
            em.flush(); // fuerza el chequeo de @Version aca, para poder dar un mensaje claro
            return resultado;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException(
                "El registro fue modificado por otro usuario. Cierre el diálogo, vuelva a abrirlo y cargue de nuevo sus cambios.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);   // regla 8: constraint por concurrencia -> mensaje de negocio
        }
    }

    /** Baja/alta logica: los maestros nunca se borran fisicamente (conservan historial). */
    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        Articulo a = em.find(Articulo.class, id);
        if (a == null) {
            throw new NegocioException("El artículo no existe");
        }
        if ("ACTIVO".equals(estadoNuevo)) {
            validar(a);   // reactivacion segura (regla 7): re-valida unicidades y reglas vigentes
        }
        a.setEstado(estadoNuevo);
    }

    /** Copia las propiedades de un articulo a otro (clonado, regla 3); omite las ya cargadas. */
    @Transactional
    public void copiarPropiedades(Long origenId, Long destinoId) {
        var origen = listarPropiedades(origenId);
        for (var p : origen) {
            Long repetidas = em.createQuery(
                    "SELECT COUNT(x) FROM ArticuloPropiedad x WHERE x.articulo = :art AND x.propiedadCodigo = :cod",
                    Long.class)
                .setParameter("art", destinoId)
                .setParameter("cod", p.getPropiedadCodigo())
                .getSingleResult();
            if (repetidas == 0) {
                var copia = new py.com.pysistemas.sginmo.dominio.catalogo.ArticuloPropiedad();
                copia.setArticulo(destinoId);
                copia.setPropiedadCodigo(p.getPropiedadCodigo());
                copia.setValor(p.getValor());
                em.persist(copia);
            }
        }
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
        if (!existeEntidadActiva("PROPIEDADES_ARTICULO", codigo)) {
            throw new NegocioException("La propiedad elegida no existe o está inactiva");
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

    /** Chequeo remoto (al salir del campo en la UI) y tambien parte de validar() al guardar. */
    public boolean existeCodigo(String codigo, Long exceptoId) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        return em.createQuery(
                "SELECT COUNT(x) FROM Articulo x WHERE lower(x.codigo) = :codigo AND (:id IS NULL OR x.id <> :id)",
                Long.class)
            .setParameter("codigo", codigo.trim().toLowerCase())
            .setParameter("id", exceptoId)
            .getSingleResult() > 0;
    }

    /** Chequeo remoto de la clave funcional aplicacion. */
    public boolean existeAplicacion(String aplicacion, Long exceptoId) {
        if (aplicacion == null || aplicacion.isBlank()) {
            return false;
        }
        return em.createQuery(
                "SELECT COUNT(x) FROM Articulo x WHERE x.aplicacion = :apl AND (:id IS NULL OR x.id <> :id)",
                Long.class)
            .setParameter("apl", aplicacion.trim())
            .setParameter("id", exceptoId)
            .getSingleResult() > 0;
    }

    private void validar(Articulo a) {
        // Unicidad de codigo (ademas del UNIQUE de la BD, para dar mensaje claro)
        if (existeCodigo(a.getCodigo(), a.getId())) {
            throw new NegocioException("Ya existe un artículo con el código '" + a.getCodigo() + "'");
        }
        // Unicidad de aplicacion (clave funcional del negocio)
        if (existeAplicacion(a.getAplicacion(), a.getId())) {
            throw new NegocioException("La aplicación '" + a.getAplicacion() + "' ya está asignada a otro artículo");
        }
        if (a.getStockMinimo() != null && a.getStockMaximo() != null
                && a.getStockMinimo().compareTo(a.getStockMaximo()) > 0) {
            throw new NegocioException("El stock mínimo no puede ser mayor que el máximo");
        }
        // Regla 11 del estandar: los dominios se re-validan en el Service aunque el combo
        // "no deberia" mandar otra cosa (anti-manipulacion de request / pantalla vieja)
        if (a.getCategoriaCodigo() != null && !a.getCategoriaCodigo().isBlank()
                && !existeEntidadActiva("TIPOS_ARTICULO", a.getCategoriaCodigo())) {
            throw new NegocioException("La categoría elegida no existe o está inactiva");
        }
        if (a.getUnidadMedidaCodigo() != null && !a.getUnidadMedidaCodigo().isBlank()
                && !existeEntidadActiva("UNIDADES_MEDIDA", a.getUnidadMedidaCodigo())) {
            throw new NegocioException("La unidad de medida elegida no existe o está inactiva");
        }
    }

    private boolean existeEntidadActiva(String lista, String codigo) {
        return em.createQuery(
                "SELECT COUNT(e) FROM Entidad e WHERE e.entidad = :lista AND e.codigo = :codigo AND e.estado = 'ACTIVO'",
                Long.class)
            .setParameter("lista", lista)
            .setParameter("codigo", codigo)
            .getSingleResult() > 0;
    }
}
