package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPO_ITEM_INGR_EGR (doc 07 §3). */
public enum TipoItemIngresoEgreso {
    INGRESO("Ingreso"),
    EGRESO("Egreso"),
    DESCUENTO("Descuento");

    private final String etiqueta;

    TipoItemIngresoEgreso(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
