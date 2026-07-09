package py.com.pysistemas.sginmo.dominio.persona;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Sucursal de una empresa (V1; estado agregado en V13). */
@jakarta.persistence.Entity
@Table(name = "sucursal")
public class Sucursal extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sucursal")
    private Long id;

    /** Discriminador multiempresa (V26); = la empresa (persona_juridica) dueña. */
    @Column(name = "tenant")
    private Long tenant;

    @Column(name = "persona_juridica", nullable = false)
    private Long personaJuridica;

    @Column(name = "descripcion", length = 180, nullable = false)
    private String descripcion;

    @Column(name = "direccion", length = 180, nullable = false)
    private String direccion;

    @Column(name = "telefono", length = 20, nullable = false)
    private String telefono;

    @Column(name = "por_defecto", nullable = false)
    private boolean porDefecto;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public Long getPersonaJuridica() { return personaJuridica; }
    public void setPersonaJuridica(Long personaJuridica) { this.personaJuridica = personaJuridica; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isPorDefecto() { return porDefecto; }
    public void setPorDefecto(boolean porDefecto) { this.porDefecto = porDefecto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sucursal)) return false;
        Sucursal otro = (Sucursal) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
