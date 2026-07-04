package py.com.pysistemas.sginmo.dominio.enums;

/** COBROS_DETALLES.TIPO_DOCUMENTO (doc 03 §1: RN-COBR-002/006). */
public enum TipoDocumentoCobro {
    CUOTA("Cuota"),
    MORA("Mora"),
    DESCUENTO("Descuento");

    private final String etiqueta;

    TipoDocumentoCobro(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
