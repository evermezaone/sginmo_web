package py.com.one.security.servicio;

/**
 * Punto de integracion del modulo: parametros configurables del proyecto anfitrion.
 * Claves que usa el modulo (todas opcionales, con defaults):
 *   LOGIN_MAX_INTENTOS, LOGIN_BLOQUEO_MINUTOS,
 *   ALERTA_LOGIN_FALLIDO (SI/NO),
 *   SMTP_HOST, SMTP_PUERTO, SMTP_USUARIO, SMTP_CLAVE, SMTP_DESDE, SMTP_TLS (SI/NO).
 * Si no hay implementacion, el modulo usa defaults y no envia correos.
 */
public interface ProveedorParametros {

    /** Valor del parametro o null si no existe. */
    String valor(String clave);
}
