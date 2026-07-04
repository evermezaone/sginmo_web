package py.com.pysistemas.sginmo.dominio.enums;

/** Estado de INGRESOS_EGRESOS. Fuente: dominio ESTADO_INGRESO_EGRESO real (doc 07 §3). */
public enum EstadoIngresoEgreso {
    PENDIENTE("Pendiente"),
    CANCELADO("Cancelado"),
    ANULADO("Anulado"),
    VENCIDO("Vencido");

    private final String etiqueta;

    EstadoIngresoEgreso(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
