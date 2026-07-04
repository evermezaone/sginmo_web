package py.com.pysistemas.sginmo.dominio.enums;

/**
 * Estado de CRONOGRAMAS_CUOTAS. Fuente: doc 07 §3 (valores reales en datos:
 * PENDIENTE/CANCELADO; CANCELADO = cuota pagada, semántica heredada del legado).
 */
public enum EstadoCuota {
    PENDIENTE("Pendiente"),
    CANCELADO("Pagada");

    private final String etiqueta;

    EstadoCuota(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
