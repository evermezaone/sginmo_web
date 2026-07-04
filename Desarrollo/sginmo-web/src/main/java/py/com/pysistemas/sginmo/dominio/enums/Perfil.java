package py.com.pysistemas.sginmo.dominio.enums;

/** Dominio TIPOS_PERFILES (doc 07 §3). Base del control de acceso (doc 05 §2). */
public enum Perfil {
    ADMINISTRADOR("Administrador"),
    USUARIO("Usuario");

    private final String etiqueta;

    Perfil(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
