package py.com.one.security.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;
import py.com.one.core.NegocioException;
import py.com.one.security.dominio.Usuario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Autenticacion (ONEsystem-security): bcrypt + bloqueo por intentos fallidos +
 * alerta por correo ante acceso fallido (si el proyecto configura SMTP_*).
 * Politica configurable via ProveedorParametros: LOGIN_MAX_INTENTOS /
 * LOGIN_BLOQUEO_MINUTOS / ALERTA_LOGIN_FALLIDO. El mensaje de credenciales
 * invalidas es el mismo para usuario inexistente y password incorrecta.
 */
@ApplicationScoped
public class SeguridadService {

    private static final int MAX_INTENTOS_DEFECTO = 5;
    private static final int BLOQUEO_MINUTOS_DEFECTO = 15;
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";

    @PersistenceContext
    private EntityManager em;

    @Inject
    private Instance<ProveedorParametros> parametros;

    @Inject
    private CorreoService correo;

    /**
     * dontRollbackOn: el registro del intento fallido debe QUEDAR grabado aunque
     * la autenticacion termine en NegocioException.
     */
    @Transactional(dontRollbackOn = NegocioException.class)
    public Usuario autenticar(String codigoUsuario, String password) {
        if (codigoUsuario == null || codigoUsuario.isBlank() || password == null || password.isBlank()) {
            throw new NegocioException("Ingrese usuario y contraseña");
        }
        Usuario u = em.createQuery(
                "SELECT u FROM Usuario u WHERE lower(u.codigoUsuario) = :codigo", Usuario.class)
            .setParameter("codigo", codigoUsuario.trim().toLowerCase())
            .getResultStream().findFirst().orElse(null);
        if (u == null) {
            throw new NegocioException(CREDENCIALES_INVALIDAS);
        }
        if (!"ACTIVO".equals(u.getEstado())) {
            throw new NegocioException("El usuario está inactivo. Contacte al administrador");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(ahora)) {
            throw new NegocioException("Usuario bloqueado por intentos fallidos. Intente nuevamente a las "
                    + u.getBloqueadoHasta().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (!BCrypt.checkpw(password, u.getPasswordHash())) {
            int intentos = (u.getIntentosFallidos() == null ? 0 : u.getIntentosFallidos()) + 1;
            int maximo = parametroEntero("LOGIN_MAX_INTENTOS", MAX_INTENTOS_DEFECTO);
            if (intentos >= maximo) {
                int minutos = parametroEntero("LOGIN_BLOQUEO_MINUTOS", BLOQUEO_MINUTOS_DEFECTO);
                u.setBloqueadoHasta(ahora.plusMinutes(minutos));
                u.setIntentosFallidos(0);
                alertar(u, "Cuenta bloqueada por intentos fallidos",
                        "Su cuenta '" + u.getCodigoUsuario() + "' fue BLOQUEADA por " + minutos
                        + " minutos tras " + maximo + " intentos fallidos de acceso ("
                        + ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ").\n"
                        + "Si no fue usted, contacte al administrador de inmediato.");
                throw new NegocioException("Usuario bloqueado por " + minutos + " minutos por intentos fallidos");
            }
            u.setIntentosFallidos(intentos);
            alertar(u, "Intento de acceso fallido",
                    "Se registró un intento de acceso FALLIDO a su cuenta '" + u.getCodigoUsuario()
                    + "' (" + ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "), intento "
                    + intentos + " de " + maximo + ".\nSi no fue usted, contacte al administrador.");
            throw new NegocioException(CREDENCIALES_INVALIDAS);
        }
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        return u;
    }

    /**
     * Permisos EFECTIVOS del usuario como claves "pantalla:accion", cargados una vez al
     * iniciar sesion: permisos directos UNION permisos de sus grupos ACTIVOS.
     * El ADMINISTRADOR no necesita filas: tiene todo implicito.
     */
    public java.util.Set<String> permisosDe(Long usuarioId) {
        var directos = em.createQuery(
                "SELECT p.pantalla, p.accion FROM PermisoUsuario p WHERE p.usuario = :usuario", Object[].class)
            .setParameter("usuario", usuarioId)
            .getResultStream();
        var porGrupo = em.createQuery(
                "SELECT pg.pantalla, pg.accion FROM PermisoGrupo pg, UsuarioGrupo ug, Grupo g "
                + "WHERE ug.usuario = :usuario AND pg.grupo = ug.grupo AND g.id = ug.grupo AND g.estado = 'ACTIVO'",
                Object[].class)
            .setParameter("usuario", usuarioId)
            .getResultStream();
        return java.util.stream.Stream.concat(directos, porGrupo)
            .map(fila -> fila[0] + ":" + fila[1])
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Hash bcrypt para alta/cambio de contrasena (ABM de usuarios). */
    public String hash(String passwordPlano) {
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(10));
    }

    /** Cambio de contrasena del propio usuario (exige la actual). */
    @Transactional
    public Usuario cambiarPassword(Long usuarioId, String actual, String nueva, String repetida) {
        validarNueva(nueva, repetida);
        Usuario u = em.find(Usuario.class, usuarioId);
        if (u == null) {
            throw new NegocioException("El usuario no existe");
        }
        if (actual == null || !BCrypt.checkpw(actual, u.getPasswordHash())) {
            throw new NegocioException("La contraseña actual no es correcta");
        }
        if (BCrypt.checkpw(nueva, u.getPasswordHash())) {
            throw new NegocioException("La contraseña nueva debe ser distinta de la actual");
        }
        u.setPasswordHash(hash(nueva));
        u.setDebeCambiarPassword(false);
        return u;
    }

    /** Politica minima de contrasenas del sistema. */
    public void validarNueva(String nueva, String repetida) {
        if (nueva == null || nueva.length() < 8) {
            throw new NegocioException("La contraseña nueva debe tener al menos 8 caracteres");
        }
        if (!nueva.equals(repetida)) {
            throw new NegocioException("Las contraseñas no coinciden");
        }
    }

    /** Alerta al correo del usuario, si esta habilitada y hay email cargado; jamas rompe el login. */
    private void alertar(Usuario u, String asunto, String cuerpo) {
        if (!"SI".equalsIgnoreCase(valorODefecto(parametro("ALERTA_LOGIN_FALLIDO"), "SI"))) {
            return;
        }
        if (u.getEmail() == null || u.getEmail().isBlank()) {
            return;
        }
        correo.enviarAsync(u.getEmail(), asunto, cuerpo);
    }

    private String parametro(String clave) {
        return parametros.isUnsatisfied() ? null : parametros.get().valor(clave);
    }

    private int parametroEntero(String clave, int defecto) {
        try {
            String valor = parametro(clave);
            return valor == null || valor.isBlank() ? defecto : Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
