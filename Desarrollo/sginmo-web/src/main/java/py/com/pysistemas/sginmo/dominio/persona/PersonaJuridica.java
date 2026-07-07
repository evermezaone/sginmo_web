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
    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "persona")
    private Persona persona;

    @Column(name = "razon_social", length = 180, nullable = false)
    private String razonSocial;

    @Column(name = "nombre_fantasia", length = 120)
    private String nombreFantasia;

    // ── Columnas de la tabla que faltaban en el ABM ──
    @Column(name = "fecha_constitucion")
    private java.time.LocalDate fechaConstitucion;

    /** FK a persona (persona fisica que representa a la empresa). */
    @Column(name = "representante_legal")
    private Long representanteLegal;

    @Column(name = "actividad_lista", length = 40)
    private String actividadLista = "ACTIVIDADES_ECONOMICAS";

    @Column(name = "actividad_codigo", length = 40)
    private String actividadCodigo;

    public Long getId() { return id; }
    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNombreFantasia() { return nombreFantasia; }
    public void setNombreFantasia(String nombreFantasia) { this.nombreFantasia = nombreFantasia; }
    public java.time.LocalDate getFechaConstitucion() { return fechaConstitucion; }
    public void setFechaConstitucion(java.time.LocalDate v) { this.fechaConstitucion = v; }
    public Long getRepresentanteLegal() { return representanteLegal; }
    public void setRepresentanteLegal(Long v) { this.representanteLegal = v; }
    public String getActividadLista() { return actividadLista; }
    public void setActividadLista(String v) { this.actividadLista = v; }
    public String getActividadCodigo() { return actividadCodigo; }
    public void setActividadCodigo(String v) { this.actividadCodigo = v; }

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
