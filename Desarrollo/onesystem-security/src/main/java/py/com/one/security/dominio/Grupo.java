package py.com.one.security.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Grupo de seguridad (REQ-0004): funciona como perfil funcional via permiso_grupo. */
@jakarta.persistence.Entity
@Table(name = "grupo")
public class Grupo extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grupo")
    private Long id;

    /** Discriminador multiempresa (V26); -1 = grupo plantilla global. UNIQUE(tenant, codigo). */
    @Column(name = "tenant")
    private Long tenant;

    @Column(name = "codigo", length = 30, nullable = false)
    private String codigo;

    @Column(name = "descripcion", length = 120, nullable = false)
    private String descripcion;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Grupo)) return false;
        Grupo otro = (Grupo) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
