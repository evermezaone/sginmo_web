package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_DOCUMENTOS_IDENTIDAD (doc 07 §3). */
public enum TipoDocumentoIdentidad {
    CI("Cédula de identidad"),
    RUC("Registro único del contribuyente"),
    DOCEX("Documento extranjero"),
    OTROS("Otros");

    private final String etiqueta;

    TipoDocumentoIdentidad(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
