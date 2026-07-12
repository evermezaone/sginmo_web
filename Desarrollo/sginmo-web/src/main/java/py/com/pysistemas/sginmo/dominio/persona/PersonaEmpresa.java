package py.com.pysistemas.sginmo.dominio.persona;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/**
 * Datos comerciales/contextuales de una persona POR TENANT (V26, doc 14 §2).
 * La identidad (nombre, documento, sexo, fecha nac., razon social) vive en
 * persona/persona_fisica/persona_juridica y se comparte entre empresas; lo que
 * varia por empresa (direccion, telefono, email, estado civil, actividad, etc.)
 * vive aca, con UNIQUE(persona, tenant). Las referencias a catalogo son id de entidad.
 */
@jakarta.persistence.Entity
@Table(name = "persona_empresa")
public class PersonaEmpresa extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "persona_empresa")
    private Long id;

    @Column(name = "persona", nullable = false)
    private Long persona;

    @Column(name = "tenant", nullable = false)
    private Long tenant;

    @Column(name = "es_contribuyente", nullable = false)
    private boolean esContribuyente = false;

    @Column(name = "clasificacion_fiscal", length = 20)
    private String clasificacionFiscal;

    @Column(name = "direccion", length = 180)
    private String direccion;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 180)
    private String email;

    /** FK a ubicacion_geografica. */
    @Column(name = "ubicacion")
    private Long ubicacion;

    @Column(name = "ubicacion_url", length = 250)
    private String ubicacionUrl;

    @Column(name = "observacion")
    private String observacion;

    /** FK a entidad (lista ESTADOS_CIVILES). */
    @Column(name = "estado_civil")
    private Long estadoCivil;

    @Column(name = "nacionalidad")
    private Long nacionalidad;   // REQ-0043: id de entidad (lista NACIONALIDADES), antes varchar libre

    @Column(name = "nombre_fantasia", length = 120)
    private String nombreFantasia;

    /** FK a persona (fisica que representa a la empresa). */
    @Column(name = "representante_legal")
    private Long representanteLegal;

    /** FK a entidad (lista ACTIVIDADES_ECONOMICAS). */
    @Column(name = "actividad")
    private Long actividad;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPersona() { return persona; }
    public void setPersona(Long persona) { this.persona = persona; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public boolean isEsContribuyente() { return esContribuyente; }
    public void setEsContribuyente(boolean v) { this.esContribuyente = v; }
    public String getClasificacionFiscal() { return clasificacionFiscal; }
    public void setClasificacionFiscal(String v) { this.clasificacionFiscal = v; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { this.direccion = v; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { this.telefono = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public Long getUbicacion() { return ubicacion; }
    public void setUbicacion(Long v) { this.ubicacion = v; }
    public String getUbicacionUrl() { return ubicacionUrl; }
    public void setUbicacionUrl(String v) { this.ubicacionUrl = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { this.observacion = v; }
    public Long getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(Long v) { this.estadoCivil = v; }
    public Long getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(Long v) { this.nacionalidad = v; }
    public String getNombreFantasia() { return nombreFantasia; }
    public void setNombreFantasia(String v) { this.nombreFantasia = v; }
    public Long getRepresentanteLegal() { return representanteLegal; }
    public void setRepresentanteLegal(Long v) { this.representanteLegal = v; }
    public Long getActividad() { return actividad; }
    public void setActividad(Long v) { this.actividad = v; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonaEmpresa)) return false;
        PersonaEmpresa otro = (PersonaEmpresa) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
