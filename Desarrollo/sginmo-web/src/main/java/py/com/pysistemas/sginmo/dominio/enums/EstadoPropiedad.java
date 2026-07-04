package py.com.pysistemas.sginmo.dominio.enums;

/**
 * Estado de PROPIEDADES. Fuente: doc 04 §3 (OperacionesService setea OCUPADA/VENDIDA)
 * y doc 07 §1 (datos reales: LIBRE/OCUPADA). Invariante del proyecto: se deriva de la
 * operación dentro de la misma transacción (bug 7 del legado).
 */
public enum EstadoPropiedad {
    LIBRE("Libre"),
    OCUPADA("Ocupada"),
    VENDIDA("Vendida");

    private final String etiqueta;

    EstadoPropiedad(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
