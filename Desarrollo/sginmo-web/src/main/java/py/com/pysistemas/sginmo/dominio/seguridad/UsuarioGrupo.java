package py.com.pysistemas.sginmo.dominio.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.pysistemas.sginmo.dominio.base.Auditable;

import java.io.Serializable;

/** Integrante de un grupo de seguridad (REQ-0004). */
@jakarta.persistence.Entity
@Table(name = "usuario_grupo")
public class UsuarioGrupo extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_grupo")
    private Long id;

    @Column(name = "usuario", nullable = false)
    private Long usuario;

    @Column(name = "grupo", nullable = false)
    private Long grupo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuario() { return usuario; }
    public void setUsuario(Long usuario) { this.usuario = usuario; }

    public Long getGrupo() { return grupo; }
    public void setGrupo(Long grupo) { this.grupo = grupo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioGrupo otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
