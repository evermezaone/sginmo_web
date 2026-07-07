package py.com.pysistemas.sginmo.dominio.activo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Valor de un atributo parametrizable de un activo (REQ-0013). Escrito por JPA
 *  (extiende Auditable) para preservar el usuario real de creacion/modificacion. */
@jakarta.persistence.Entity
@Table(name = "activo_atributo")
public class ActivoAtributo extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activo_atributo")
    private Long id;

    @Column(name = "activo", nullable = false)
    private Long activo;

    @Column(name = "atributo", nullable = false)
    private Long atributo;

    @Column(name = "valor", length = 250)
    private String valor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivo() { return activo; }
    public void setActivo(Long activo) { this.activo = activo; }
    public Long getAtributo() { return atributo; }
    public void setAtributo(Long atributo) { this.atributo = atributo; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivoAtributo)) return false;
        ActivoAtributo otro = (ActivoAtributo) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
