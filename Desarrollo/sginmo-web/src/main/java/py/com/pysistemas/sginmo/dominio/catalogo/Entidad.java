package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import py.com.pysistemas.sginmo.dominio.base.Auditable;

/**
 * Listas abiertas configurables (decision de diseño 3 del usuario):
 * cada lista es un valor de `entidad` y sus opciones son los `codigo`.
 * En la UI el campo lista va fijo y el combo muestra los codigos ACTIVO.
 */
@jakarta.persistence.Entity
@Table(name = "entidad")
@IdClass(EntidadId.class)
public class Entidad extends Auditable {

    @Id
    @Column(name = "entidad", length = 40)
    private String entidad;

    @Id
    @Column(name = "codigo", length = 40)
    private String codigo;

    @Column(name = "descripcion", length = 180, nullable = false)
    private String descripcion;

    @Column(name = "valor_texto", length = 250)
    private String valorTexto;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getValorTexto() { return valorTexto; }
    public void setValorTexto(String valorTexto) { this.valorTexto = valorTexto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
