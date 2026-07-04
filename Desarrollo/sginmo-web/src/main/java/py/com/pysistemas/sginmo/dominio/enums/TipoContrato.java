package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_CONTRATOS (doc 07 §3). */
public enum TipoContrato {
    PRIVADO("Contrato privado"),
    PUBLICO("Escritura pública");

    private final String etiqueta;

    TipoContrato(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
