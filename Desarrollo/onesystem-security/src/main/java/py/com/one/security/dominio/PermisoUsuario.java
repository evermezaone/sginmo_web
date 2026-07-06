package py.com.one.security.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/**
 * Permiso explicito por pantalla+accion (REQ-0004). El perfil ADMINISTRADOR no necesita
 * filas aca: tiene todo implicito. pantalla '*' otorga la accion en todas las pantallas.
 */
@jakarta.persistence.Entity
@Table(name = "permiso_usuario")
public class PermisoUsuario extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permiso_usuario")
    private Long id;

    @Column(name = "usuario", nullable = false)
    private Long usuario;

    @Column(name = "pantalla", length = 60, nullable = false)
    private String pantalla;

    @Column(name = "accion", length = 30, nullable = false)
    private String accion;   // enum cerrado Accion

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuario() { return usuario; }
    public void setUsuario(Long usuario) { this.usuario = usuario; }

    public String getPantalla() { return pantalla; }
    public void setPantalla(String pantalla) { this.pantalla = pantalla; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermisoUsuario)) return false;
        PermisoUsuario otro = (PermisoUsuario) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
