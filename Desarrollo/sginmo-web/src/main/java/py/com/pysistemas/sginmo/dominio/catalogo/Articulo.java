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
import py.com.one.core.Auditable;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /** Discriminador multiempresa (V26); -1 = catalogo GLOBAL. */
    @Column(name = "tenant")
    private Long tenant;
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }

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

    // ── Referencias de catalogo por id (V26): FK bigint a entidad ──
    @Column(name = "categoria")
    private Long categoria;              // lista TIPOS_ARTICULO

    @Column(name = "unidad_medida")
    private Long unidadMedida;           // lista UNIDADES_MEDIDA

    @Column(name = "presentacion")
    private Long presentacion;           // lista PRESENTACIONES

    @Column(name = "clasificacion")
    private Long clasificacion;   // REQ-0048: id de entidad (lista CLASIFICACION_ARTICULO), antes varchar libre

    @Column(name = "marca")
    private Long marca;                  // lista MARCAS

    @Column(name = "modelo")
    private Long modelo;                 // lista MODELOS

    @Column(name = "familia")
    private Long familia;                // lista FAMILIAS_ARTICULO

    @Column(name = "grupo")
    private Long grupo;                  // lista GRUPOS_ARTICULO

    @Column(name = "subgrupo")
    private Long subgrupo;               // lista SUBGRUPOS_ARTICULO

    @Column(name = "procedencia")
    private Long procedencia;            // lista PROCEDENCIAS

    @Column(name = "codigo_barra", length = 50)
    private String codigoBarra;

    @Column(name = "codigo_interno", length = 25)
    private String codigoInterno;

    @Column(name = "cuenta", length = 50)
    private String cuenta;

    @Column(name = "proveedor")
    private Long proveedor;                  // FK a persona (rol proveedor)

    @Column(name = "tipo_movimiento", length = 10)
    private String tipoMovimiento;           // INGRESO | EGRESO | DESCUENTO (nullable)

    @Column(name = "aplicacion", length = 30)
    private String aplicacion;               // clave funcional que usa la logica de negocio

    @Column(name = "precio_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "precio_unitario_ext", precision = 15, scale = 2)
    private BigDecimal precioUnitarioExt;

    @Column(name = "ultimo_costo", precision = 15, scale = 2)
    private BigDecimal ultimoCosto;

    @Column(name = "costo_moneda_ext", precision = 15, scale = 2)
    private BigDecimal costoMonedaExt;

    @Column(name = "stock_minimo", precision = 15, scale = 3)
    private BigDecimal stockMinimo;

    @Column(name = "stock_maximo", precision = 15, scale = 3)
    private BigDecimal stockMaximo;

    @Column(name = "porcentaje_stock_minimo", precision = 5, scale = 2)
    private BigDecimal porcentajeStockMinimo;

    @Column(name = "proximo_vencimiento")
    private LocalDate proximoVencimiento;

    @Column(name = "modifica_estado", nullable = false)
    private boolean modificaEstado;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    /** Disponible para operaciones NUEVAS (V8, regla 1 del estandar): independiente de estado. */
    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = Boolean.TRUE;

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

    public Long getCategoria() { return categoria; }
    public void setCategoria(Long v) { this.categoria = v; }
    public Long getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(Long v) { this.unidadMedida = v; }
    public Long getPresentacion() { return presentacion; }
    public void setPresentacion(Long v) { this.presentacion = v; }
    public Long getClasificacion() { return clasificacion; }
    public void setClasificacion(Long v) { this.clasificacion = v; }
    public Long getMarca() { return marca; }
    public void setMarca(Long v) { this.marca = v; }
    public Long getModelo() { return modelo; }
    public void setModelo(Long v) { this.modelo = v; }
    public Long getFamilia() { return familia; }
    public void setFamilia(Long v) { this.familia = v; }
    public Long getGrupo() { return grupo; }
    public void setGrupo(Long v) { this.grupo = v; }
    public Long getSubgrupo() { return subgrupo; }
    public void setSubgrupo(Long v) { this.subgrupo = v; }
    public Long getProcedencia() { return procedencia; }
    public void setProcedencia(Long v) { this.procedencia = v; }
    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String v) { this.codigoBarra = v; }
    public String getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(String v) { this.codigoInterno = v; }
    public String getCuenta() { return cuenta; }
    public void setCuenta(String v) { this.cuenta = v; }
    public Long getProveedor() { return proveedor; }
    public void setProveedor(Long v) { this.proveedor = v; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public String getAplicacion() { return aplicacion; }
    public void setAplicacion(String aplicacion) { this.aplicacion = aplicacion; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getPrecioUnitarioExt() { return precioUnitarioExt; }
    public void setPrecioUnitarioExt(BigDecimal v) { this.precioUnitarioExt = v; }
    public BigDecimal getUltimoCosto() { return ultimoCosto; }
    public void setUltimoCosto(BigDecimal v) { this.ultimoCosto = v; }
    public BigDecimal getCostoMonedaExt() { return costoMonedaExt; }
    public void setCostoMonedaExt(BigDecimal v) { this.costoMonedaExt = v; }

    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }

    public BigDecimal getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(BigDecimal stockMaximo) { this.stockMaximo = stockMaximo; }

    public BigDecimal getPorcentajeStockMinimo() { return porcentajeStockMinimo; }
    public void setPorcentajeStockMinimo(BigDecimal v) { this.porcentajeStockMinimo = v; }
    public java.time.LocalDate getProximoVencimiento() { return proximoVencimiento; }
    public void setProximoVencimiento(java.time.LocalDate v) { this.proximoVencimiento = v; }

    public boolean isModificaEstado() { return modificaEstado; }
    public void setModificaEstado(boolean modificaEstado) { this.modificaEstado = modificaEstado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Boolean getHabilitado() { return habilitado; }
    public void setHabilitado(Boolean habilitado) { this.habilitado = habilitado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    /** Igualdad por id (regla del estandar). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Articulo)) return false;
        Articulo otro = (Articulo) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
