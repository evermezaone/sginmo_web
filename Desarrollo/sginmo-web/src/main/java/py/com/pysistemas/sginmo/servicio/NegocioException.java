package py.com.pysistemas.sginmo.servicio;

/** Error de regla de negocio con mensaje apto para mostrar al usuario. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
