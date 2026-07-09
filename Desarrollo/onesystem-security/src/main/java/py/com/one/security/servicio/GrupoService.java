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

    /** Tenant global -1 = SUPERADMIN (ve todo) y hogar de los grupos plantilla. */
    private static final Long TENANT_GLOBAL = -1L;

    private static boolean superadmin(Long tenantCtx) {
        return tenantCtx == null || TENANT_GLOBAL.equals(tenantCtx);
    }

    /** ABM: SUPERADMIN ve todos los grupos; ADMINISTRADOR sus grupos + las plantillas -1. */
    public long contar(String filtro, Long tenantCtx) {
        var q = em.createQuery(
            "SELECT COUNT(g) FROM Grupo g WHERE (:sa = TRUE OR g.tenant = :t OR g.tenant = -1)"
            + " AND (:f = '' OR lower(g.codigo) LIKE :like OR lower(g.descripcion) LIKE :like)",
            Long.class);
        aplicarFiltro(q, filtro, tenantCtx);
        return q.getSingleResult();
    }

    public List<Grupo> listar(int primero, int cantidad, String filtro, Long tenantCtx) {
        var q = em.createQuery(
            "SELECT g FROM Grupo g WHERE (:sa = TRUE OR g.tenant = :t OR g.tenant = -1)"
            + " AND (:f = '' OR lower(g.codigo) LIKE :like OR lower(g.descripcion) LIKE :like) ORDER BY g.descripcion",
            Grupo.class);
        aplicarFiltro(q, filtro, tenantCtx);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void aplicarFiltro(jakarta.persistence.TypedQuery<?> q, String filtro, Long tenantCtx) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f);
        q.setParameter("like", "%" + f + "%");
        q.setParameter("sa", superadmin(tenantCtx));
        q.setParameter("t", tenantCtx);
    }

    /** Grupos asignables a un usuario del contexto: los propios activos + las plantillas -1. */
    public List<Grupo> gruposActivos(Long tenantCtx) {
        return em.createQuery(
                "SELECT g FROM Grupo g WHERE g.estado = 'ACTIVO' AND (:sa = TRUE OR g.tenant = :t OR g.tenant = -1)"
                + " ORDER BY g.descripcion", Grupo.class)
            .setParameter("sa", superadmin(tenantCtx))
            .setParameter("t", tenantCtx)
            .getResultList();
    }

    // ── Escrituras ──

    /** actorTenant (F6): tenant del operador (-1 = SUPERADMIN). Un ADMINISTRADOR solo crea/edita
     *  grupos de SU tenant; las plantillas -1 son de solo lectura para el (solo el SUPERADMIN las toca). */
    @Transactional
    public Grupo guardar(Grupo grupo, Long actorTenant) {
        autorizacion.exigir("grupos", grupo.getId() == null ? "CREAR" : "EDITAR");
        boolean sa = superadmin(actorTenant);
        if (grupo.getCodigo() == null || grupo.getCodigo().isBlank()) {
            throw new NegocioException("El código del grupo es obligatorio");
        }
        if (grupo.getId() == null) {
            // V26: unicidad por (tenant, codigo). El grupo nace en el tenant del operador; el
            // SUPERADMIN puede crear plantillas globales dejando tenant=-1 explicito.
            if (grupo.getTenant() == null) grupo.setTenant(sa ? -1L : actorTenant);
            if (!sa && !actorTenant.equals(grupo.getTenant())) {
                throw new NegocioException("No puede crear grupos en otra empresa");
            }
        } else {
            Grupo enBd = em.find(Grupo.class, grupo.getId());
            if (enBd == null) throw new NegocioException("El grupo no existe");
            if (!sa && !actorTenant.equals(enBd.getTenant())) {
                throw new NegocioException("El grupo pertenece a otra empresa");
            }
            grupo.setTenant(enBd.getTenant());   // el tenant no cambia por edicion
        }
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
    public void cambiarEstado(Long id, String estadoNuevo, Long actorTenant) {
        autorizacion.exigir("grupos", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        Grupo g = em.find(Grupo.class, id);
        if (g == null) {
            throw new NegocioException("El grupo no existe");
        }
        if (!superadmin(actorTenant) && !actorTenant.equals(g.getTenant())) {
            throw new NegocioException("El grupo pertenece a otra empresa");
        }
        g.setEstado(estadoNuevo);
    }

    // ── Permisos del grupo ──

    /** El grupo debe ser VISIBLE al actor: propio, plantilla -1 o SUPERADMIN (para lecturas). */
    private void exigirGrupoVisible(Long grupoId, Long actorTenant) {
        Grupo g = grupoId == null ? null : em.find(Grupo.class, grupoId);
        if (g == null) throw new NegocioException("El grupo no existe");
        boolean visible = superadmin(actorTenant) || TENANT_GLOBAL.equals(g.getTenant())
                || (actorTenant != null && actorTenant.equals(g.getTenant()));
        if (!visible) throw new NegocioException("El grupo pertenece a otra empresa");
    }

    /** El grupo debe ser EDITABLE por el actor: propio del tenant, o SUPERADMIN. Las plantillas
     *  -1 son de solo lectura para el ADMINISTRADOR (obs 257). */
    private void exigirGrupoEditable(Long grupoId, Long actorTenant) {
        Grupo g = grupoId == null ? null : em.find(Grupo.class, grupoId);
        if (g == null) throw new NegocioException("El grupo no existe");
        if (!superadmin(actorTenant) && !(actorTenant != null && actorTenant.equals(g.getTenant()))) {
            throw new NegocioException("El grupo pertenece a otra empresa");
        }
    }

    public List<PermisoGrupo> listarPermisos(Long grupoId, Long actorTenant) {
        exigirGrupoVisible(grupoId, actorTenant);   // obs 257
        return em.createQuery(
                "SELECT p FROM PermisoGrupo p WHERE p.grupo = :g ORDER BY p.pantalla, p.accion", PermisoGrupo.class)
            .setParameter("g", grupoId)
            .getResultList();
    }

    @Transactional
    public void agregarPermiso(Long grupoId, String pantalla, String accion, Long actorTenant) {
        autorizacion.exigir("grupos", "EDITAR");
        if (pantalla == null || pantalla.isBlank() || accion == null || accion.isBlank()) {
            throw new NegocioException("Elija pantalla y acción");
        }
        exigirGrupoEditable(grupoId, actorTenant);   // obs 257: no se editan grupos ajenos ni plantillas -1
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
    public void eliminarPermiso(Long permisoId, Long actorTenant) {
        autorizacion.exigir("grupos", "EDITAR");
        var p = em.find(PermisoGrupo.class, permisoId);
        if (p != null) {
            exigirGrupoEditable(p.getGrupo(), actorTenant);   // obs 257
            em.remove(p);
        }
    }

    // ── Integrantes ──

    public List<UsuarioGrupo> listarIntegrantes(Long grupoId, Long actorTenant) {
        exigirGrupoVisible(grupoId, actorTenant);   // obs 257
        // Aun en un grupo plantilla -1 visible, el ADMINISTRADOR solo ve integrantes de SU tenant
        // (usuario_grupo esta fuera de RLS); el SUPERADMIN los ve todos (obs 259).
        return em.createQuery(
                "SELECT ug FROM UsuarioGrupo ug WHERE ug.grupo = :g"
                + " AND (:sa = TRUE OR EXISTS (SELECT 1 FROM Usuario u WHERE u.id = ug.usuario AND u.tenant = :t))"
                + " ORDER BY ug.id", UsuarioGrupo.class)
            .setParameter("g", grupoId)
            .setParameter("sa", superadmin(actorTenant)).setParameter("t", actorTenant)
            .getResultList();
    }
}
