package py.com.pysistemas.sginmo.dominio.seguridad;

/**
 * Acciones autorizables de un ABM (enum cerrado, REQ-0004).
 * VER_AUDITORIA: los datos de auditoria (usuario/fecha de modificacion, historial)
 * solo se muestran al ADMINISTRADOR o con este permiso explicito.
 */
public enum Accion {
    VER,
    CREAR,
    EDITAR,
    INACTIVAR,
    REACTIVAR,
    EXPORTAR,
    VER_AUDITORIA
}
