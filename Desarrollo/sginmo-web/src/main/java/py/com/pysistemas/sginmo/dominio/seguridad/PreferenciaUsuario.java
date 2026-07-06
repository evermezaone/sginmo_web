package py.com.pysistemas.sginmo.dominio.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.pysistemas.sginmo.dominio.base.Auditable;

import java.io.Serializable;

/** Preferencia de pantalla por usuario (REQ-0004): "Mi vista" y futuras. Valor en JSON. */
@jakarta.persistence.Entity
@Table(name = "preferencia_usuario")
public class PreferenciaUsuario extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preferencia_usuario")
    private Long id;

    @Column(name = "usuario", nullable = false)
    private Long usuario;

    @Column(name = "pantalla", length = 60, nullable = false)
    private String pantalla;

    @Column(name = "clave", length = 80, nullable = false)
    private String clave;

    @Column(name = "valor", nullable = false)
    private String valor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuario() { return usuario; }
    public void setUsuario(Long usuario) { this.usuario = usuario; }

    public String getPantalla() { return pantalla; }
    public void setPantalla(String pantalla) { this.pantalla = pantalla; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreferenciaUsuario otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
