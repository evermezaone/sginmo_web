package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica;

import java.util.List;
import java.util.Map;

/** ABM de geografia (REQ-0007): 8.276 ubicaciones INE, recursivo con autocomplete de padre. */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class GeografiaService {

    private static final Map<String, String> ORDEN = Map.of(
        "nombre", "u.nombre", "nivel", "u.nivel", "codigoOficial", "u.codigoOficial", "estado", "u.estado");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    /** Aislamiento por tenant (F4): geografia = arbol INE global (-1) + ubicaciones del tenant. */
    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    @jakarta.inject.Inject
    private CatalogoService catalogoService;

    public long contar(String filtro) {
        var q = em.createQuery("SELECT COUNT(u) FROM UbicacionGeografica u WHERE (u.tenant = -1 OR u.tenant = :t) AND (:f = '' OR lower(u.nombre) LIKE :like OR u.codigoOficial LIKE :like)", Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<UbicacionGeografica> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery("SELECT u FROM UbicacionGeografica u LEFT JOIN FETCH u.padre WHERE (u.tenant = -1 OR u.tenant = :t) AND (:f = '' OR lower(u.nombre) LIKE :like OR u.codigoOficial LIKE :like) ORDER BY "
                + (ruta == null ? "u.nombre" : ruta) + (asc ? " ASC" : " DESC"), UbicacionGeografica.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%").setParameter("t", tenant.actual());
    }

    /** Autocomplete lazy para elegir padre (regla: combos grandes jamas cargan todo). */
    public List<UbicacionGeografica> buscar(String texto) {
        String f = texto == null ? "" : texto.trim().toLowerCase();
        return em.createQuery(
                "SELECT u FROM UbicacionGeografica u LEFT JOIN FETCH u.padre WHERE u.estado = 'ACTIVO' AND (u.tenant = -1 OR u.tenant = :t) AND lower(u.nombre) LIKE :like ORDER BY u.nombre",
                UbicacionGeografica.class)
            .setParameter("t", tenant.actual()).setParameter("like", f + "%")
            .setMaxResults(15)
            .getResultList();
    }

    public UbicacionGeografica buscarPorId(Long id) {
        if (id == null) return null;
        UbicacionGeografica u = em.find(UbicacionGeografica.class, id);
        if (u == null) return null;
        // Visible (obs 254): global -1, propia del tenant, o cualquiera para SUPERADMIN.
        Long t = u.getTenant();
        boolean visible = tenant.esSuperadmin() || (t != null
                && (t.equals(tenant.actual()) || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(t)));
        return visible ? u : null;
    }

    public List<UbicacionGeografica> porNivel(String nivelCodigo) {
        Long nivel = catalogoService.idOpcion("NIVELES_UBICACION", nivelCodigo);
        if (nivel == null) return java.util.List.of();
        return em.createQuery(
                "SELECT u FROM UbicacionGeografica u WHERE u.estado = 'ACTIVO'"
                + " AND (u.tenant = -1 OR u.tenant = :t) AND u.nivel = :nivel ORDER BY u.nombre",
                UbicacionGeografica.class)
            .setParameter("t", tenant.actual())
            .setParameter("nivel", nivel)
            .getResultList();
    }

    public List<UbicacionGeografica> hijosDe(Long padreId) {
        if (padreId == null) return java.util.List.of();
        return em.createQuery(
                "SELECT u FROM UbicacionGeografica u WHERE u.estado = 'ACTIVO'"
                + " AND (u.tenant = -1 OR u.tenant = :t) AND u.padre.id = :padre ORDER BY u.nombre",
                UbicacionGeografica.class)
            .setParameter("t", tenant.actual())
            .setParameter("padre", padreId)
            .getResultList();
    }

    public String codigoNivel(Long nivelId) {
        return catalogoService.codigoOpcion(nivelId);
    }

    @Transactional
    public UbicacionGeografica guardar(UbicacionGeografica u) {
        autorizacion.exigir("geografia", u.getId() == null ? "CREAR" : "EDITAR");
        if (u.getNombre() == null || u.getNombre().isBlank()) {
            throw new NegocioException("El nombre es obligatorio");
        }
        if (u.getNivel() == null) {
            throw new NegocioException("El nivel es obligatorio");
        }
        if (u.getPadre() != null && u.getId() != null && u.getId().equals(u.getPadre().getId())) {
            throw new NegocioException("Una ubicación no puede ser su propio padre");
        }
        // Pertenencia por tenant (F4): el alta toma el tenant del usuario; el arbol INE (-1) solo
        // lo edita SUPERADMIN; una ubicacion del tenant no cambia de tenant.
        if (u.getId() == null) {
            u.setTenant(tenant.actual());
        } else {
            UbicacionGeografica enBd = em.find(UbicacionGeografica.class, u.getId());
            if (enBd == null) throw new NegocioException("La ubicación no existe");
            Long tOrig = enBd.getTenant();
            boolean editable = tOrig != null && (tOrig.equals(tenant.actual())
                    || (py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(tOrig) && tenant.esSuperadmin()));
            if (!editable) throw new NegocioException("La ubicación pertenece a otra empresa");
            u.setTenant(tOrig);
        }
        try {
            UbicacionGeografica r = u.getId() == null ? persistir(u) : em.merge(u);
            em.flush();
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La ubicación fue modificada por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);   // codigo_oficial UNIQUE -> mensaje claro
        }
    }

    private UbicacionGeografica persistir(UbicacionGeografica u) { em.persist(u); return u; }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        autorizacion.exigir("geografia", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        UbicacionGeografica u = em.find(UbicacionGeografica.class, id);
        if (u == null) throw new NegocioException("La ubicación no existe");
        // Pertenencia (obs 251): propio del tenant, o global -1 solo por SUPERADMIN.
        Long tOrig = u.getTenant();
        boolean editable = tOrig != null && (tOrig.equals(tenant.actual())
                || (py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(tOrig) && tenant.esSuperadmin()));
        if (!editable) throw new NegocioException("La ubicación pertenece a otra empresa");
        u.setEstado(estadoNuevo);
    }
}
