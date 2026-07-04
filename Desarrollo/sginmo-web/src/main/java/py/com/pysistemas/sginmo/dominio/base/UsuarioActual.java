package py.com.pysistemas.sginmo.dominio.base;

/**
 * Proveedor del usuario autenticado para la auditoria de entidades.
 * La implementacion real (sesion web) llega con REQ-0004/0005; mientras tanto
 * el listener usa el fallback "sistema" (jobs, ETL, tests).
 */
public interface UsuarioActual {

    String SISTEMA = "sistema";

    /** Codigo del usuario autenticado (equivalente a USUARIOS.CODIGO_USUARIO del legado). */
    String codigoUsuario();
}
