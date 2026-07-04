package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_ENTIDADES_INMOBILIARIAS (doc 07 §3, 6 valores reales). */
public enum TipoEntidadInmobiliaria {
    EDIFICIO("Edificio"),
    LOTEAMIENTO("Loteamiento"),
    COMPLEJO("Complejo"),
    BARRIO_CERRADO("Barrio cerrado"),
    SALONES_COMERCIALES("Salones comerciales"),
    NO_APLICA("No aplica");

    private final String etiqueta;

    TipoEntidadInmobiliaria(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
