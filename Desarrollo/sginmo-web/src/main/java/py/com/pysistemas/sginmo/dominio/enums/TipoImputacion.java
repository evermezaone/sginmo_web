package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPO_IMPUTACION (doc 07 §3, etiquetas reales de la BD). */
public enum TipoImputacion {
    ADMINISTRADOR("Administrador"),
    ENTIDAD_INMOBILIARIA("Edificio/Loteamiento"),
    INQUILINO("Cliente"),
    PROPIEDAD("Departamento/Casa/Dúplex"),
    PROPIETARIO("Propietario"),
    VENDEDOR("Vendedor");

    private final String etiqueta;

    TipoImputacion(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
