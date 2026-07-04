package py.com.pysistemas.sginmo.dominio.enums;

/** Estado de COBROS. Fuente: doc 03 §1 (RN-COBR-007) y doc 07 §3. */
public enum EstadoCobro {
    CANCELADO("Cobrado"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoCobro(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
