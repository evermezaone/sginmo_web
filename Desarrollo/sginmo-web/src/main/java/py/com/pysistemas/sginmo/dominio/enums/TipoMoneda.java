package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_MONEDAS (doc 07 §3). LOCAL = Gs. (moneda por defecto del negocio). */
public enum TipoMoneda {
    LOCAL("Local"),
    EXTRANJERA("Extranjera");

    private final String etiqueta;

    TipoMoneda(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
