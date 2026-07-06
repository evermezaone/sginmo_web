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
import java.time.LocalDate;

/** Persona fisica (V1, PK compartida con persona via @MapsId). */
@jakarta.persistence.Entity
@Table(name = "persona_fisica")
public class PersonaFisica extends Auditable implements Serializable {

    @Id
    @Column(name = "persona")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "persona")
    private Persona persona;

    @Column(name = "nombres", length = 120, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 120, nullable = false)
    private String apellidos;

    @Column(name = "sexo", length = 10)
    private String sexo;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "estado_civil_codigo", length = 40)
    private String estadoCivilCodigo;

    @Column(name = "nacionalidad", length = 80)
    private String nacionalidad;

    public Long getId() { return id; }
    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getEstadoCivilCodigo() { return estadoCivilCodigo; }
    public void setEstadoCivilCodigo(String estadoCivilCodigo) { this.estadoCivilCodigo = estadoCivilCodigo; }
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonaFisica)) return false;
        PersonaFisica otro = (PersonaFisica) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
