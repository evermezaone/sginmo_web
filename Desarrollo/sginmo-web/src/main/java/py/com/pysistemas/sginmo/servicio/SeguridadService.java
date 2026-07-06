package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;
import py.com.pysistemas.sginmo.dominio.seguridad.Usuario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Autenticacion (REQ-0004): bcrypt + bloqueo por intentos fallidos.
 * Politica configurable en parametro_sistema: LOGIN_MAX_INTENTOS / LOGIN_BLOQUEO_MINUTOS.
 * El mensaje de credenciales invalidas es el mismo para usuario inexistente y password
 * incorrecta (no revelar que usuarios existen).
 */
@ApplicationScoped
public class SeguridadService {

    private static final int MAX_INTENTOS_DEFECTO = 5;
    private static final int BLOQUEO_MINUTOS_DEFECTO = 15;
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

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
                throw new NegocioException("Usuario bloqueado por " + minutos + " minutos por intentos fallidos");
            }
            u.setIntentosFallidos(intentos);
            throw new NegocioException(CREDENCIALES_INVALIDAS);
        }
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        return u;
    }

    /** Hash bcrypt para alta/cambio de contrasena (ABM de usuarios). */
    public String hash(String passwordPlano) {
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(10));
    }

    private int parametroEntero(String clave, int defecto) {
        try {
            var filas = em.createNativeQuery("SELECT valor FROM parametro_sistema WHERE clave = :clave")
                .setParameter("clave", clave)
                .getResultList();
            return filas.isEmpty() ? defecto : Integer.parseInt(filas.get(0).toString().trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }
}
