package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_OPERACIONES (doc 07 §3). */
public enum TipoOperacion {
    ALQUILER("Alquiler"),
    VENTA("Venta");

    private final String etiqueta;

    TipoOperacion(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
