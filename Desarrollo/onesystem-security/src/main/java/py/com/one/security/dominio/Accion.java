package py.com.one.security.dominio;

/**
 * Acciones autorizables de un ABM (enum cerrado, REQ-0004).
 * OPERAR: paquete de trabajo normal del ABM — incluye VER, CREAR, EDITAR, INACTIVAR,
 * REACTIVAR y EXPORTAR en un solo permiso (pedido del usuario: al encargado de una
 * pantalla se le da OPERAR y listo). NO incluye VER_AUDITORIA, que siempre es explicito.
 */
public enum Accion {
    OPERAR,
    VER,
    CREAR,
    EDITAR,
    INACTIVAR,
    REACTIVAR,
    EXPORTAR,
    GENERAR_CONTRATO,
    GENERAR_PAGARE,
    DESCARGAR_DOCUMENTO,
    VER_AUDITORIA
}
