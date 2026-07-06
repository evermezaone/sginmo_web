package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Parametros de configuracion (V1): clave-valor con descripcion; la clave es inmutable. */
@jakarta.persistence.Entity
@Table(name = "parametro_sistema")
public class ParametroSistema extends Auditable implements Serializable {

    @Id
    @Column(name = "clave", length = 60)
    private String clave;

    @Column(name = "valor", length = 120, nullable = false)
    private String valor;

    @Column(name = "descripcion", length = 180, nullable = false)
    private String descripcion;

    /** true si el valor es sensible (clave/contrasena): se enmascara en la grilla. */
    public boolean isSensible() {
        return clave != null && (clave.contains("CLAVE") || clave.contains("PASS"));
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParametroSistema otro)) return false;
        return clave != null && clave.equals(otro.clave);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
