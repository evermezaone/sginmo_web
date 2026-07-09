package py.com.one.security.servicio;

import py.com.one.core.NegocioException;
import py.com.one.core.ErroresBd;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.security.dominio.PermisoUsuario;
import py.com.one.security.dominio.Usuario;

import java.util.List;

/**
 * ABM de usuarios y sus permisos por accion (REQ-0004).
 * Reglas: codigo unico; nunca DELETE (baja logica); un usuario no puede inactivarse
 * a si mismo; alta y reseteo de contrasena fuerzan cambio al proximo ingreso.
 */
@ApplicationScoped
public class UsuarioService {

    @PersistenceContext
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @Inject
    private SeguridadService seguridadService;

    // ── Consultas ──

    /** Tenant global -1 = SUPERADMIN: ve/gestiona los usuarios de TODAS las empresas. */
    private static final Long TENANT_GLOBAL = -1L;

    /** true si el contexto es SUPERADMIN (ve todo); null se trata como global por compatibilidad. */
    private static boolean superadmin(Long tenantCtx) {
        return tenantCtx == null || TENANT_GLOBAL.equals(tenantCtx);
    }

    public long contar(String filtro, Long tenantCtx) {
        var q = em.createQuery(
            "SELECT COUNT(u) FROM Usuario u WHERE (:sa = TRUE OR u.tenant = :t)"
            + " AND (:f = '' OR lower(u.codigoUsuario) LIKE :like OR lower(u.perfil) LIKE :like)",
            Long.class);
        aplicarFiltro(q, filtro, tenantCtx);
        return q.getSingleResult();
    }

    public List<Usuario> listar(int primero, int cantidad, String filtro, Long tenantCtx) {
        var q = em.createQuery(
            "SELECT u FROM Usuario u WHERE (:sa = TRUE OR u.tenant = :t)"
            + " AND (:f = '' OR lower(u.codigoUsuario) LIKE :like OR lower(u.perfil) LIKE :like) ORDER BY u.codigoUsuario",
            Usuario.class);
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

    public boolean existeCodigo(String codigo, Long exceptoId) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        return em.createQuery(
                "SELECT COUNT(u) FROM Usuario u WHERE lower(u.codigoUsuario) = :codigo AND (:id IS NULL OR u.id <> :id)",
                Long.class)
            .setParameter("codigo", codigo.trim().toLowerCase())
            .setParameter("id", exceptoId)
            .getSingleResult() > 0;
    }

    // ── Escrituras ──

    /**
     * Alta/edicion. passwordPlano: obligatorio al crear; en edicion, si viene, resetea (y fuerza cambio).
     * actorTenant (F6): tenant del usuario que opera (-1 = SUPERADMIN). Un ADMINISTRADOR solo puede
     * crear/editar usuarios de SU tenant (usuario/grupo estan fuera de RLS: la guarda es de la capa app).
     */
    @Transactional
    public Usuario guardar(Usuario usuario, String passwordPlano, Long empresaDefecto, Long actorTenant) {
        autorizacion.exigir("usuarios", usuario.getId() == null ? "CREAR" : "EDITAR");
        boolean sa = superadmin(actorTenant);
        if (usuario.getCodigoUsuario() == null || usuario.getCodigoUsuario().isBlank()) {
            throw new NegocioException("El código de usuario es obligatorio");
        }
        if (existeCodigo(usuario.getCodigoUsuario(), usuario.getId())) {
            throw new NegocioException("Ya existe el usuario '" + usuario.getCodigoUsuario() + "'");
        }
        boolean esNuevo = usuario.getId() == null;
        if (esNuevo) {
            if (passwordPlano == null || passwordPlano.isBlank()) {
                throw new NegocioException("La contraseña inicial es obligatoria");
            }
            seguridadService.validarNueva(passwordPlano, passwordPlano);
            usuario.setPasswordHash(seguridadService.hash(passwordPlano));
            usuario.setDebeCambiarPassword(true);
            if (usuario.getTenant() == null) {
                usuario.setTenant(empresaDefecto);
            }
            // Pertenencia: un ADMINISTRADOR no puede crear usuarios en otra empresa.
            if (!sa && !actorTenant.equals(usuario.getTenant())) {
                throw new NegocioException("No puede crear usuarios en otra empresa");
            }
            em.persist(usuario);
            return usuario;
        }
        // Edicion: el usuario existente debe ser del tenant del actor (salvo SUPERADMIN); el tenant
        // no se reasigna desde el ABM comun (lo fija el alta / el SUPERADMIN).
        Usuario enBd = em.find(Usuario.class, usuario.getId());
        if (enBd == null) {
            throw new NegocioException("El usuario no existe");
        }
        if (!sa && !actorTenant.equals(enBd.getTenant())) {
            throw new NegocioException("El usuario pertenece a otra empresa");
        }
        if (!sa) {
            usuario.setTenant(enBd.getTenant());
        }
        if (passwordPlano != null && !passwordPlano.isBlank()) {
            seguridadService.validarNueva(passwordPlano, passwordPlano);
            usuario.setPasswordHash(seguridadService.hash(passwordPlano));
            usuario.setDebeCambiarPassword(true);   // reseteo por administrador
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }
        try {
            var resultado = em.merge(usuario);
            em.flush();
            return resultado;
        } catch (jakarta.persistence.OptimisticLockException e) {
            // ESTE era el bug del "no acepta el pass": el choque de version fallaba EN SILENCIO
            throw new NegocioException(
                "El usuario fue modificado por otra sesión. Cierre el diálogo, vuelva a abrirlo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    // ── Grupos del usuario (V10) ──

    /** El usuario objetivo debe ser del tenant del actor (SUPERADMIN sin restriccion). Guarda
     *  comun de las lecturas/escrituras por id sobre usuario (obs 257: usuario fuera de RLS). */
    private void exigirUsuarioDelTenant(Long usuarioId, Long actorTenant) {
        if (superadmin(actorTenant)) return;
        Usuario u = usuarioId == null ? null : em.find(Usuario.class, usuarioId);
        if (u == null) throw new NegocioException("El usuario no existe");
        if (!actorTenant.equals(u.getTenant())) {
            throw new NegocioException("El usuario pertenece a otra empresa");
        }
    }

    public List<py.com.one.security.dominio.UsuarioGrupo> listarGruposDe(Long usuarioId, Long actorTenant) {
        exigirUsuarioDelTenant(usuarioId, actorTenant);   // obs 257
        return em.createQuery(
                "SELECT ug FROM UsuarioGrupo ug WHERE ug.usuario = :u ORDER BY ug.id",
                py.com.one.security.dominio.UsuarioGrupo.class)
            .setParameter("u", usuarioId)
            .getResultList();
    }

    public java.util.Map<Long, py.com.one.security.dominio.Grupo> gruposPorId(Long tenantCtx) {
        var mapa = new java.util.LinkedHashMap<Long, py.com.one.security.dominio.Grupo>();
        // Visibles al contexto: SUPERADMIN todos; ADMINISTRADOR sus grupos + las plantillas -1.
        em.createQuery("SELECT g FROM Grupo g WHERE (:sa = TRUE OR g.tenant = :t OR g.tenant = -1)"
                + " ORDER BY g.descripcion", py.com.one.security.dominio.Grupo.class)
            .setParameter("sa", superadmin(tenantCtx))
            .setParameter("t", tenantCtx)
            .getResultList()
            .forEach(g -> mapa.put(g.getId(), g));
        return mapa;
    }

    @Transactional
    public void agregarAGrupo(Long usuarioId, Long grupoId, Long actorTenant) {
        autorizacion.exigir("usuarios", "EDITAR");
        if (usuarioId == null || grupoId == null) {
            throw new NegocioException("Elija el grupo");
        }
        // Pertenencia (F6): el usuario debe ser del tenant del actor y el grupo asignable
        // (plantilla -1 o del propio tenant); el SUPERADMIN no tiene restriccion.
        if (!superadmin(actorTenant)) {
            Usuario u = em.find(Usuario.class, usuarioId);
            if (u == null) throw new NegocioException("El usuario no existe");
            if (!actorTenant.equals(u.getTenant())) {
                throw new NegocioException("El usuario pertenece a otra empresa");
            }
            py.com.one.security.dominio.Grupo g = em.find(py.com.one.security.dominio.Grupo.class, grupoId);
            if (g == null) throw new NegocioException("El grupo no existe");
            if (!TENANT_GLOBAL.equals(g.getTenant()) && !actorTenant.equals(g.getTenant())) {
                throw new NegocioException("El grupo pertenece a otra empresa");
            }
        }
        Long repetidos = em.createQuery(
                "SELECT COUNT(ug) FROM UsuarioGrupo ug WHERE ug.usuario = :u AND ug.grupo = :g", Long.class)
            .setParameter("u", usuarioId).setParameter("g", grupoId)
            .getSingleResult();
        if (repetidos > 0) {
            throw new NegocioException("El usuario ya integra ese grupo");
        }
        var ug = new py.com.one.security.dominio.UsuarioGrupo();
        ug.setUsuario(usuarioId);
        ug.setGrupo(grupoId);
        em.persist(ug);
    }

    @Transactional
    public void quitarDeGrupo(Long usuarioGrupoId, Long actorTenant) {
        autorizacion.exigir("usuarios", "EDITAR");
        var ug = em.find(py.com.one.security.dominio.UsuarioGrupo.class, usuarioGrupoId);
        if (ug != null) {
            exigirUsuarioDelTenant(ug.getUsuario(), actorTenant);   // obs 257
            em.remove(ug);
        }
    }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo, Long usuarioActualId, Long actorTenant) {
        autorizacion.exigir("usuarios", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        if (id != null && id.equals(usuarioActualId) && "INACTIVO".equals(estadoNuevo)) {
            throw new NegocioException("No puede inactivar su propio usuario");
        }
        Usuario u = em.find(Usuario.class, id);
        if (u == null) {
            throw new NegocioException("El usuario no existe");
        }
        if (!superadmin(actorTenant) && !actorTenant.equals(u.getTenant())) {
            throw new NegocioException("El usuario pertenece a otra empresa");
        }
        u.setEstado(estadoNuevo);
        if ("ACTIVO".equals(estadoNuevo)) {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
    }

    /** Desbloqueo manual sin esperar el vencimiento del bloqueo. */
    @Transactional
    public void desbloquear(Long id, Long actorTenant) {
        autorizacion.exigir("usuarios", "EDITAR");
        Usuario u = em.find(Usuario.class, id);
        if (u != null) {
            if (!superadmin(actorTenant) && !actorTenant.equals(u.getTenant())) {
                throw new NegocioException("El usuario pertenece a otra empresa");
            }
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
    }

    // ── Permisos por accion ──

    public List<PermisoUsuario> listarPermisos(Long usuarioId, Long actorTenant) {
        exigirUsuarioDelTenant(usuarioId, actorTenant);   // obs 257
        return em.createQuery(
                "SELECT p FROM PermisoUsuario p WHERE p.usuario = :u ORDER BY p.pantalla, p.accion",
                PermisoUsuario.class)
            .setParameter("u", usuarioId)
            .getResultList();
    }

    @Transactional
    public void agregarPermiso(Long usuarioId, String pantalla, String accion, Long actorTenant) {
        autorizacion.exigir("usuarios", "EDITAR");
        if (pantalla == null || pantalla.isBlank() || accion == null || accion.isBlank()) {
            throw new NegocioException("Elija pantalla y acción");
        }
        if (!superadmin(actorTenant)) {
            Usuario u = em.find(Usuario.class, usuarioId);
            if (u == null) throw new NegocioException("El usuario no existe");
            if (!actorTenant.equals(u.getTenant())) {
                throw new NegocioException("El usuario pertenece a otra empresa");
            }
        }
        Long repetidos = em.createQuery(
                "SELECT COUNT(p) FROM PermisoUsuario p WHERE p.usuario = :u AND p.pantalla = :p AND p.accion = :a",
                Long.class)
            .setParameter("u", usuarioId).setParameter("p", pantalla).setParameter("a", accion)
            .getSingleResult();
        if (repetidos > 0) {
            throw new NegocioException("El usuario ya tiene ese permiso");
        }
        var permiso = new PermisoUsuario();
        permiso.setUsuario(usuarioId);
        permiso.setPantalla(pantalla);
        permiso.setAccion(accion);
        em.persist(permiso);
    }

    @Transactional
    public void eliminarPermiso(Long permisoId, Long actorTenant) {
        autorizacion.exigir("usuarios", "EDITAR");
        var p = em.find(PermisoUsuario.class, permisoId);
        if (p != null) {
            exigirUsuarioDelTenant(p.getUsuario(), actorTenant);   // obs 257
            em.remove(p);
        }
    }
}
