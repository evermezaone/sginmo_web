package py.com.pysistemas.sginmo.dominio.enums;

/** Estado de OPERACIONES_PROPIEDADES. Fuente: doc 07 §3 y máquina de estados doc 00. */
public enum EstadoOperacion {
    VIGENTE("Vigente"),
    FINALIZADO("Finalizado");

    private final String etiqueta;

    EstadoOperacion(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
