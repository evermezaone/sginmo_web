package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio CONDICION_OPERACION (doc 07 §3). CONTADO fuerza plazo=1 (RN-OPE-007). */
public enum CondicionOperacion {
    CONTADO("Contado"),
    CREDITO("Crédito");

    private final String etiqueta;

    CondicionOperacion(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
