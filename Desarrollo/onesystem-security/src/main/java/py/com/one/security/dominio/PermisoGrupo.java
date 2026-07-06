package py.com.one.security.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Permiso de un grupo (REQ-0004): el grupo actua como perfil funcional. */
@jakarta.persistence.Entity
@Table(name = "permiso_grupo")
public class PermisoGrupo extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permiso_grupo")
    private Long id;

    @Column(name = "grupo", nullable = false)
    private Long grupo;

    @Column(name = "pantalla", length = 60, nullable = false)
    private String pantalla;

    @Column(name = "accion", length = 30, nullable = false)
    private String accion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGrupo() { return grupo; }
    public void setGrupo(Long grupo) { this.grupo = grupo; }

    public String getPantalla() { return pantalla; }
    public void setPantalla(String pantalla) { this.pantalla = pantalla; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermisoGrupo otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
