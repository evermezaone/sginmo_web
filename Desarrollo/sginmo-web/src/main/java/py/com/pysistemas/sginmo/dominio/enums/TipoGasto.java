package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_GASTOS (doc 07 §3). */
public enum TipoGasto {
    FIJO("Fijo"),
    VARIABLE("Variable");

    private final String etiqueta;

    TipoGasto(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
