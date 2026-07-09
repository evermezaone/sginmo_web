package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Atributos parametrizables del articulo (lista PROPIEDADES_ARTICULO). */
@jakarta.persistence.Entity
@Table(name = "articulo_propiedad")
public class ArticuloPropiedad extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "articulo_propiedad")
    private Long id;

    @Column(name = "articulo", nullable = false)
    private Long articulo;

    @Column(name = "propiedad", nullable = false)
    private Long propiedad;              // FK bigint a entidad (lista PROPIEDADES_ARTICULO)

    @Column(name = "valor", length = 100)
    private String valor;

    @Column(name = "numero", precision = 15, scale = 2)
    private BigDecimal numero;

    @Column(name = "fecha")
    private LocalDate fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getArticulo() { return articulo; }
    public void setArticulo(Long articulo) { this.articulo = articulo; }

    public Long getPropiedad() { return propiedad; }
    public void setPropiedad(Long propiedad) { this.propiedad = propiedad; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public BigDecimal getNumero() { return numero; }
    public void setNumero(BigDecimal numero) { this.numero = numero; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}
