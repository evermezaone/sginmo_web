package py.com.one.security.servicio;

import py.com.one.core.NegocioException;
import py.com.one.core.ErroresBd;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.security.dominio.Grupo;
import py.com.one.security.dominio.PermisoGrupo;
import py.com.one.security.dominio.UsuarioGrupo;

import java.util.List;

/**
 * ABM de grupos de seguridad (REQ-0004, V10): el grupo actua como perfil funcional.
 * Sus permisos se suman a los directos de cada integrante al iniciar sesion.
 */
@ApplicationScoped
public class GrupoService {

    @PersistenceContext
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    // ── Consultas ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(g) FROM Grupo g WHERE (:f = '' OR lower(g.codigo) LIKE :like OR lower(g.descripcion) LIKE :like)",
            Long.class);
        aplicarFiltro(q, filtro);
        return q.getSingleResult();
    }

    public List<Grupo> listar(int primero, int cantidad, String filtro) {
        var q = em.createQuery(
            "SELECT g FROM Grupo g WHERE (:f = '' OR lower(g.codigo) LIKE :like OR lower(g.descripcion) LIKE :like) ORDER BY g.descripcion",
            Grupo.class);
        aplicarFiltro(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void aplicarFiltro(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f);
        q.setParameter("like", "%" + f + "%");
    }

    public List<Grupo> gruposActivos() {
        return em.createQuery("SELECT g FROM Grupo g WHERE g.estado = 'ACTIVO' ORDER BY g.descripcion", Grupo.class)
            .getResultList();
    }

    // ── Escrituras ──

    @Transactional
    public Grupo guardar(Grupo grupo) {
        autorizacion.exigir("grupos", grupo.getId() == null ? "CREAR" : "EDITAR");
        if (grupo.getCodigo() == null || grupo.getCodigo().isBlank()) {
            throw new NegocioException("El código del grupo es obligatorio");
        }
        // V26: unicidad por (tenant, codigo). Default -1 = grupo plantilla global; el modulo
        // consumidor (SGInmo, F6) puede asignar el tenant del contexto antes de guardar.
        if (grupo.getTenant() == null) grupo.setTenant(-1L);
        Long repetidos = em.createQuery(
                "SELECT COUNT(g) FROM Grupo g WHERE g.tenant = :t AND lower(g.codigo) = :codigo AND (:id IS NULL OR g.id <> :id)",
                Long.class)
            .setParameter("t", grupo.getTenant())
            .setParameter("codigo", grupo.getCodigo().trim().toLowerCase())
            .setParameter("id", grupo.getId())
            .getSingleResult();
        if (repetidos > 0) {
            throw new NegocioException("Ya existe el grupo '" + grupo.getCodigo() + "'");
        }
        try {
            Grupo resultado;
            if (grupo.getId() == null) {
                em.persist(grupo);
                resultado = grupo;
            } else {
                resultado = em.merge(grupo);
            }
            em.flush();
            return resultado;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("El grupo fue modificado por otra sesión. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        autorizacion.exigir("grupos", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        Grupo g = em.find(Grupo.class, id);
        if (g == null) {
            throw new NegocioException("El grupo no existe");
        }
        g.setEstado(estadoNuevo);
    }

    // ── Permisos del grupo ──

    public List<PermisoGrupo> listarPermisos(Long grupoId) {
        return em.createQuery(
                "SELECT p FROM PermisoGrupo p WHERE p.grupo = :g ORDER BY p.pantalla, p.accion", PermisoGrupo.class)
            .setParameter("g", grupoId)
            .getResultList();
    }

    @Transactional
    public void agregarPermiso(Long grupoId, String pantalla, String accion) {
        autorizacion.exigir("grupos", "EDITAR");
        if (pantalla == null || pantalla.isBlank() || accion == null || accion.isBlank()) {
            throw new NegocioException("Elija pantalla y acción");
        }
        Long repetidos = em.createQuery(
                "SELECT COUNT(p) FROM PermisoGrupo p WHERE p.grupo = :g AND p.pantalla = :p AND p.accion = :a",
                Long.class)
            .setParameter("g", grupoId).setParameter("p", pantalla).setParameter("a", accion)
            .getSingleResult();
        if (repetidos > 0) {
            throw new NegocioException("El grupo ya tiene ese permiso");
        }
        var permiso = new PermisoGrupo();
        permiso.setGrupo(grupoId);
        permiso.setPantalla(pantalla);
        permiso.setAccion(accion);
        em.persist(permiso);
    }

    @Transactional
    public void eliminarPermiso(Long permisoId) {
        autorizacion.exigir("grupos", "EDITAR");
        var p = em.find(PermisoGrupo.class, permisoId);
        if (p != null) {
            em.remove(p);
        }
    }

    // ── Integrantes ──

    public List<UsuarioGrupo> listarIntegrantes(Long grupoId) {
        return em.createQuery(
                "SELECT ug FROM UsuarioGrupo ug WHERE ug.grupo = :g ORDER BY ug.id", UsuarioGrupo.class)
            .setParameter("g", grupoId)
            .getResultList();
    }
}
