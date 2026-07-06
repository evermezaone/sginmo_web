package py.com.pysistemas.sginmo.dominio.persona;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Persona juridica (V1, PK compartida con persona). Una empresa = persona juridica con rol EMPRESA. */
@jakarta.persistence.Entity
@Table(name = "persona_juridica")
public class PersonaJuridica extends Auditable implements Serializable {

    @Id
    @Column(name = "persona")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "persona")
    private Persona persona;

    @Column(name = "razon_social", length = 180, nullable = false)
    private String razonSocial;

    @Column(name = "nombre_fantasia", length = 120)
    private String nombreFantasia;

    public Long getId() { return id; }
    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNombreFantasia() { return nombreFantasia; }
    public void setNombreFantasia(String nombreFantasia) { this.nombreFantasia = nombreFantasia; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonaJuridica)) return false;
        PersonaJuridica otro = (PersonaJuridica) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
