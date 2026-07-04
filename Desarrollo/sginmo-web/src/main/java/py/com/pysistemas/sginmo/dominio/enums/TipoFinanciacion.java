package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_FINANCIACIONES (doc 07 §3). Default del legado: FINANCIACION_PROPIA. */
public enum TipoFinanciacion {
    FINANCIACION_PROPIA("Financiación propia"),
    FINANCIACION_BANCARIA("Financiación bancaria");

    private final String etiqueta;

    TipoFinanciacion(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
