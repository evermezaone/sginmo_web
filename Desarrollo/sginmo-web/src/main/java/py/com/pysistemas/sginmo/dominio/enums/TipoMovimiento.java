package py.com.pysistemas.sginmo.dominio.enums;

/** INGRESOS_EGRESOS.TIPO (doc 02 §1.12). */
public enum TipoMovimiento {
    INGRESO("Ingreso"),
    EGRESO("Egreso");

    private final String etiqueta;

    TipoMovimiento(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
