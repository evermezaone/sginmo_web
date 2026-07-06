package py.com.one.security.servicio;

import java.io.Serializable;

/** Pantalla autorizable del sistema anfitrion (para los combos de permisos). */
public class OpcionPantalla implements Serializable {

    private final String codigo;
    private final String descripcion;

    public OpcionPantalla(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
}
