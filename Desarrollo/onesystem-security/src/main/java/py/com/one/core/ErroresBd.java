package py.com.one.core;

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
            // REQ-0076: los RAISE EXCEPTION del motor (PL/pgSQL, SQLState P0001) son mensajes de NEGOCIO
            // deliberados (ej. "No hay timbrado ACTIVO..."); se muestran tal cual en vez de fallar en
            // silencio. Solo P0001 (raise_exception), nunca un error tecnico crudo.
            if (causa instanceof java.sql.SQLException sql && "P0001".equals(sql.getSQLState())) {
                String m = sql.getMessage();
                if (m != null && !m.isBlank()) {
                    // psql suele anteponer "ERROR: "; se limpia y se corta a una linea.
                    m = m.replaceFirst("(?i)^error:\\s*", "").trim();
                    int nl = m.indexOf('\n');
                    if (nl > 0) m = m.substring(0, nl).trim();
                    return new NegocioException(m);
                }
            }
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
