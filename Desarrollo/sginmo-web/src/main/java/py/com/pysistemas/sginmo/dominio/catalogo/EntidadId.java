package py.com.pysistemas.sginmo.dominio.catalogo;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de la tabla generica de listas: (entidad, codigo). */
public class EntidadId implements Serializable {

    private String entidad;
    private String codigo;

    public EntidadId() { }

    public EntidadId(String entidad, String codigo) {
        this.entidad = entidad;
        this.codigo = codigo;
    }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntidadId otra)) return false;
        return Objects.equals(entidad, otra.entidad) && Objects.equals(codigo, otra.codigo);
    }

    @Override
    public int hashCode() { return Objects.hash(entidad, codigo); }
}
