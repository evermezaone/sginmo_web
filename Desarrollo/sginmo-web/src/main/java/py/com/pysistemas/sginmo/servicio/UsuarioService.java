package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.pysistemas.sginmo.dominio.seguridad.PermisoUsuario;
import py.com.pysistemas.sginmo.dominio.seguridad.Usuario;

import java.util.List;

/**
 * ABM de usuarios y sus permisos por accion (REQ-0004).
 * Reglas: codigo unico; nunca DELETE (baja logica); un usuario no puede inactivarse
 * a si mismo; alta y reseteo de contrasena fuerzan cambio al proximo ingreso.
 */
@ApplicationScoped
public class UsuarioService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @Inject
    private SeguridadService seguridadService;

    // ── Consultas ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(u) FROM Usuario u WHERE (:f = '' OR lower(u.codigoUsuario) LIKE :like OR lower(u.perfil) LIKE :like)",
            Long.class);
        aplicarFiltro(q, filtro);
        return q.getSingleResult();
    }

    public List<Usuario> listar(int primero, int cantidad, String filtro) {
        var q = em.createQuery(
            "SELECT u FROM Usuario u WHERE (:f = '' OR lower(u.codigoUsuario) LIKE :like OR lower(u.perfil) LIKE :like) ORDER BY u.codigoUsuario",
            Usuario.class);
        aplicarFiltro(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void aplicarFiltro(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f);
        q.setParameter("like", "%" + f + "%");
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

    /** Alta/edicion. passwordPlano: obligatorio al crear; en edicion, si viene, resetea (y fuerza cambio). */
    @Transactional
    public Usuario guardar(Usuario usuario, String passwordPlano, Long empresaDefecto) {
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
            if (usuario.getEmpresa() == null) {
                usuario.setEmpresa(empresaDefecto);
            }
            em.persist(usuario);
            return usuario;
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

    public List<py.com.pysistemas.sginmo.dominio.seguridad.UsuarioGrupo> listarGruposDe(Long usuarioId) {
        return em.createQuery(
                "SELECT ug FROM UsuarioGrupo ug WHERE ug.usuario = :u ORDER BY ug.id",
                py.com.pysistemas.sginmo.dominio.seguridad.UsuarioGrupo.class)
            .setParameter("u", usuarioId)
            .getResultList();
    }

    public java.util.Map<Long, py.com.pysistemas.sginmo.dominio.seguridad.Grupo> gruposPorId() {
        var mapa = new java.util.LinkedHashMap<Long, py.com.pysistemas.sginmo.dominio.seguridad.Grupo>();
        em.createQuery("SELECT g FROM Grupo g ORDER BY g.descripcion",
                py.com.pysistemas.sginmo.dominio.seguridad.Grupo.class)
            .getResultList()
            .forEach(g -> mapa.put(g.getId(), g));
        return mapa;
    }

    @Transactional
    public void agregarAGrupo(Long usuarioId, Long grupoId) {
        if (usuarioId == null || grupoId == null) {
            throw new NegocioException("Elija el grupo");
        }
        Long repetidos = em.createQuery(
                "SELECT COUNT(ug) FROM UsuarioGrupo ug WHERE ug.usuario = :u AND ug.grupo = :g", Long.class)
            .setParameter("u", usuarioId).setParameter("g", grupoId)
            .getSingleResult();
        if (repetidos > 0) {
            throw new NegocioException("El usuario ya integra ese grupo");
        }
        var ug = new py.com.pysistemas.sginmo.dominio.seguridad.UsuarioGrupo();
        ug.setUsuario(usuarioId);
        ug.setGrupo(grupoId);
        em.persist(ug);
    }

    @Transactional
    public void quitarDeGrupo(Long usuarioGrupoId) {
        var ug = em.find(py.com.pysistemas.sginmo.dominio.seguridad.UsuarioGrupo.class, usuarioGrupoId);
        if (ug != null) {
            em.remove(ug);
        }
    }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo, Long usuarioActualId) {
        if (id != null && id.equals(usuarioActualId) && "INACTIVO".equals(estadoNuevo)) {
            throw new NegocioException("No puede inactivar su propio usuario");
        }
        Usuario u = em.find(Usuario.class, id);
        if (u == null) {
            throw new NegocioException("El usuario no existe");
        }
        u.setEstado(estadoNuevo);
        if ("ACTIVO".equals(estadoNuevo)) {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
    }

    /** Desbloqueo manual sin esperar el vencimiento del bloqueo. */
    @Transactional
    public void desbloquear(Long id) {
        Usuario u = em.find(Usuario.class, id);
        if (u != null) {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
    }

    // ── Permisos por accion ──

    public List<PermisoUsuario> listarPermisos(Long usuarioId) {
        return em.createQuery(
                "SELECT p FROM PermisoUsuario p WHERE p.usuario = :u ORDER BY p.pantalla, p.accion",
                PermisoUsuario.class)
            .setParameter("u", usuarioId)
            .getResultList();
    }

    @Transactional
    public void agregarPermiso(Long usuarioId, String pantalla, String accion) {
        if (pantalla == null || pantalla.isBlank() || accion == null || accion.isBlank()) {
            throw new NegocioException("Elija pantalla y acción");
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
    public void eliminarPermiso(Long permisoId) {
        var p = em.find(PermisoUsuario.class, permisoId);
        if (p != null) {
            em.remove(p);
        }
    }
}
