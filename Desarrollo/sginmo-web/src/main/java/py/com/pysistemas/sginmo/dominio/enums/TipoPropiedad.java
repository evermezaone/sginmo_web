package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_PROPIEDADES (doc 07 §3, 9 valores reales). */
public enum TipoPropiedad {
    CASA("Casa"),
    DEPARTAMENTO("Departamento"),
    DUPLEX("Dúplex"),
    LOTE("Lote"),
    OFICINA("Oficina"),
    PIEZA("Pieza"),
    SALONES("Salón comercial"),
    ESTACIONAMIENTO("Estacionamiento"),
    AREA_COMUN("Área común");

    private final String etiqueta;

    TipoPropiedad(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
