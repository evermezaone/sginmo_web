package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_PERSONERIAS (doc 07 §3 — códigos reales PERFIS/PERJUR del legado). */
public enum TipoPersoneria {
    PERFIS("Persona Física"),
    PERJUR("Persona Jurídica");

    private final String etiqueta;

    TipoPersoneria(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
