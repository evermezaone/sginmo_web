package py.com.pysistemas.sginmo.dominio.activo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Propietario de un activo (V1): relacion muchos a muchos activo-persona. */
@jakarta.persistence.Entity
@Table(name = "activo_propietario")
public class ActivoPropietario extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activo_propietario")
    private Long id;

    @Column(name = "activo", nullable = false)
    private Long activo;

    @Column(name = "propietario", nullable = false)
    private Long propietario;

    /** Baja logica (V22): ACTIVO | INACTIVO. Preserva la trazabilidad historica. */
    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public Long getActivo() { return activo; }
    public void setActivo(Long activo) { this.activo = activo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Long getPropietario() { return propietario; }
    public void setPropietario(Long propietario) { this.propietario = propietario; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivoPropietario)) return false;
        ActivoPropietario otro = (ActivoPropietario) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
