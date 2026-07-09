package py.com.pysistemas.sginmo.dominio.persona;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Rol de una persona (V1): una misma persona puede ser CLIENTE, PROVEEDOR, EMPRESA, etc. */
@jakarta.persistence.Entity
@Table(name = "persona_rol")
public class PersonaRol extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "persona_rol")
    private Long id;

    @Column(name = "persona", nullable = false)
    private Long persona;

    @Column(name = "rol", nullable = false)
    private Long rol;                    // FK bigint a entidad (lista ROLES_PERSONA)

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public Long getPersona() { return persona; }
    public void setPersona(Long persona) { this.persona = persona; }
    public Long getRol() { return rol; }
    public void setRol(Long rol) { this.rol = rol; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonaRol)) return false;
        PersonaRol otro = (PersonaRol) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
