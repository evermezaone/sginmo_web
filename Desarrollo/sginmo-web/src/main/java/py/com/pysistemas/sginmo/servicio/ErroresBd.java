package py.com.pysistemas.sginmo.servicio;

/**
 * Traduccion de errores tecnicos de la BD a mensajes de negocio (regla 8 del estandar):
 * aunque el Service valide, una constraint puede saltar igual por concurrencia.
 * Nunca se muestra un mensaje crudo de PostgreSQL al usuario.
 */
public final class ErroresBd {

    private ErroresBd() { }

    /**
     * Si la causa es una violacion de constraint devuelve la NegocioException traducida;
     * si no, relanza la excepcion original tal cual (error real de programacion).
     */
    public static NegocioException traducir(RuntimeException excepcion) {
        Throwable causa = excepcion;
        while (causa != null) {
            if (causa.getClass().getName().contains("ConstraintViolation")) {
                String mensaje = causa.getMessage() == null ? "" : causa.getMessage().toLowerCase();
                if (mensaje.contains("foreign key") || mensaje.contains("violates foreign")) {
                    return new NegocioException(
                        "No se puede completar la operación: el registro está en uso por otros datos");
                }
                if (mensaje.contains("unique") || mensaje.contains("duplicate")) {
                    return new NegocioException(
                        "Ya existe un registro con esos datos (clave única duplicada)");
                }
                return new NegocioException(
                    "La base de datos rechazó la operación por una restricción de integridad");
            }
            causa = causa.getCause();
        }
        throw excepcion;
    }
}
