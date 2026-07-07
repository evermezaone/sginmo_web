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

    // ── Clasificacion extendida (listas configurables de `entidad`) ──
    @Column(name = "presentacion_lista", length = 40)
    private String presentacionLista = "PRESENTACIONES";
    @Column(name = "presentacion_codigo", length = 40)
    private String presentacionCodigo;

    @Column(name = "clasificacion", length = 20)
    private String clasificacion;

    @Column(name = "marca_lista", length = 40)
    private String marcaLista = "MARCAS";
    @Column(name = "marca_codigo", length = 40)
    private String marcaCodigo;

    @Column(name = "modelo_lista", length = 40)
    private String modeloLista = "MODELOS";
    @Column(name = "modelo_codigo", length = 40)
    private String modeloCodigo;

    @Column(name = "familia_lista", length = 40)
    private String familiaLista = "FAMILIAS_ARTICULO";
    @Column(name = "familia_codigo", length = 40)
    private String familiaCodigo;

    @Column(name = "grupo_lista", length = 40)
    private String grupoLista = "GRUPOS_ARTICULO";
    @Column(name = "grupo_codigo", length = 40)
    private String grupoCodigo;

    @Column(name = "subgrupo_lista", length = 40)
    private String subgrupoLista = "SUBGRUPOS_ARTICULO";
    @Column(name = "subgrupo_codigo", length = 40)
    private String subgrupoCodigo;

    @Column(name = "procedencia_lista", length = 40)
    private String procedenciaLista = "PROCEDENCIAS";
    @Column(name = "procedencia_codigo", length = 40)
    private String procedenciaCodigo;

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

    public String getCategoriaLista() { return categoriaLista; }
    public void setCategoriaLista(String categoriaLista) { this.categoriaLista = categoriaLista; }

    public String getCategoriaCodigo() { return categoriaCodigo; }
    public void setCategoriaCodigo(String categoriaCodigo) { this.categoriaCodigo = categoriaCodigo; }

    public String getUnidadMedidaLista() { return unidadMedidaLista; }
    public void setUnidadMedidaLista(String unidadMedidaLista) { this.unidadMedidaLista = unidadMedidaLista; }

    public String getUnidadMedidaCodigo() { return unidadMedidaCodigo; }
    public void setUnidadMedidaCodigo(String unidadMedidaCodigo) { this.unidadMedidaCodigo = unidadMedidaCodigo; }

    public String getPresentacionLista() { return presentacionLista; }
    public void setPresentacionLista(String v) { this.presentacionLista = v; }
    public String getPresentacionCodigo() { return presentacionCodigo; }
    public void setPresentacionCodigo(String v) { this.presentacionCodigo = v; }
    public String getClasificacion() { return clasificacion; }
    public void setClasificacion(String v) { this.clasificacion = v; }
    public String getMarcaLista() { return marcaLista; }
    public void setMarcaLista(String v) { this.marcaLista = v; }
    public String getMarcaCodigo() { return marcaCodigo; }
    public void setMarcaCodigo(String v) { this.marcaCodigo = v; }
    public String getModeloLista() { return modeloLista; }
    public void setModeloLista(String v) { this.modeloLista = v; }
    public String getModeloCodigo() { return modeloCodigo; }
    public void setModeloCodigo(String v) { this.modeloCodigo = v; }
    public String getFamiliaLista() { return familiaLista; }
    public void setFamiliaLista(String v) { this.familiaLista = v; }
    public String getFamiliaCodigo() { return familiaCodigo; }
    public void setFamiliaCodigo(String v) { this.familiaCodigo = v; }
    public String getGrupoLista() { return grupoLista; }
    public void setGrupoLista(String v) { this.grupoLista = v; }
    public String getGrupoCodigo() { return grupoCodigo; }
    public void setGrupoCodigo(String v) { this.grupoCodigo = v; }
    public String getSubgrupoLista() { return subgrupoLista; }
    public void setSubgrupoLista(String v) { this.subgrupoLista = v; }
    public String getSubgrupoCodigo() { return subgrupoCodigo; }
    public void setSubgrupoCodigo(String v) { this.subgrupoCodigo = v; }
    public String getProcedenciaLista() { return procedenciaLista; }
    public void setProcedenciaLista(String v) { this.procedenciaLista = v; }
    public String getProcedenciaCodigo() { return procedenciaCodigo; }
    public void setProcedenciaCodigo(String v) { this.procedenciaCodigo = v; }
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
