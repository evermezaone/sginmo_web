package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import py.com.pysistemas.sginmo.dominio.base.Auditable;

import java.math.BigDecimal;

/**
 * Maestro de articulos (productos y servicios; conceptos de dinero del sistema).
 * Mapea el subconjunto de columnas que usa el ABM; el resto de las columnas de la
 * tabla conserva sus DEFAULT (el INSERT solo envia lo mapeado).
 */
@jakarta.persistence.Entity
@Table(name = "articulo")
public class Articulo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "articulo")
    private Long id;

    @NotBlank(message = "El código es obligatorio")
    @Column(name = "codigo", length = 25, nullable = false, unique = true)
    private String codigo;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(name = "descripcion", length = 150, nullable = false)
    private String descripcion;

    @NotBlank(message = "El tipo es obligatorio")
    @Column(name = "tipo", length = 10, nullable = false)
    private String tipo;                     // PRODUCTO | SERVICIO

    @NotNull(message = "El impuesto es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "impuesto", nullable = false)
    private Impuesto impuesto;

    @Column(name = "categoria_lista", length = 40)
    private String categoriaLista = "TIPOS_ARTICULO";

    @Column(name = "categoria_codigo", length = 40)
    private String categoriaCodigo;

    @Column(name = "unidad_medida_lista", length = 40)
    private String unidadMedidaLista = "UNIDADES_MEDIDA";

    @Column(name = "unidad_medida_codigo", length = 40)
    private String unidadMedidaCodigo;

    @Column(name = "tipo_movimiento", length = 10)
    private String tipoMovimiento;           // INGRESO | EGRESO | DESCUENTO (nullable)

    @Column(name = "aplicacion", length = 30)
    private String aplicacion;               // clave funcional que usa la logica de negocio

    @Column(name = "precio_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "stock_minimo", precision = 15, scale = 3)
    private BigDecimal stockMinimo;

    @Column(name = "stock_maximo", precision = 15, scale = 3)
    private BigDecimal stockMaximo;

    @Column(name = "modifica_estado", nullable = false)
    private boolean modificaEstado;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    @Column(name = "observacion", length = 250)
    private String observacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Impuesto getImpuesto() { return impuesto; }
    public void setImpuesto(Impuesto impuesto) { this.impuesto = impuesto; }

    public String getCategoriaLista() { return categoriaLista; }
    public void setCategoriaLista(String categoriaLista) { this.categoriaLista = categoriaLista; }

    public String getCategoriaCodigo() { return categoriaCodigo; }
    public void setCategoriaCodigo(String categoriaCodigo) { this.categoriaCodigo = categoriaCodigo; }

    public String getUnidadMedidaLista() { return unidadMedidaLista; }
    public void setUnidadMedidaLista(String unidadMedidaLista) { this.unidadMedidaLista = unidadMedidaLista; }

    public String getUnidadMedidaCodigo() { return unidadMedidaCodigo; }
    public void setUnidadMedidaCodigo(String unidadMedidaCodigo) { this.unidadMedidaCodigo = unidadMedidaCodigo; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public String getAplicacion() { return aplicacion; }
    public void setAplicacion(String aplicacion) { this.aplicacion = aplicacion; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }

    public BigDecimal getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(BigDecimal stockMaximo) { this.stockMaximo = stockMaximo; }

    public boolean isModificaEstado() { return modificaEstado; }
    public void setModificaEstado(boolean modificaEstado) { this.modificaEstado = modificaEstado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
