package py.com.pysistemas.sginmo.dominio.activo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Activo inmobiliario (V1, recursivo): reemplaza entidades_inmobiliarias + propiedades del
 * legado. El tipo (lista TIPOS_ACTIVO: EDIFICIO, DEPARTAMENTO, TERRENO, LOTE, ...) lo define
 * y sus atributos por tipo lo complementan (decision de diseno del usuario). padre permite
 * contenedor (edificio > departamento; loteamiento > lote).
 */
@jakarta.persistence.Entity
@Table(name = "activo")
public class Activo extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activo")
    private Long id;

    @Column(name = "padre")
    private Long padre;

    @Column(name = "nombre", length = 180, nullable = false)
    private String nombre;

    @Column(name = "tipo", nullable = false)
    private Long tipo;                   // FK bigint a entidad (lista TIPOS_ACTIVO)

    @Column(name = "tenant", nullable = false)
    private Long tenant;

    @Column(name = "ubicacion")
    private Long ubicacion;

    @Column(name = "direccion", length = 250)
    private String direccion;

    @Column(name = "precio_venta", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioVenta = BigDecimal.ZERO;

    @Column(name = "comision_venta", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionVenta = BigDecimal.ZERO;

    @Column(name = "precio_alquiler", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioAlquiler = BigDecimal.ZERO;

    @Column(name = "comision_alquiler", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionAlquiler = BigDecimal.ZERO;

    /** LIBRE | OCUPADA | VENDIDA (lo mueven las operaciones; en el ABM solo se muestra). */
    @Column(name = "estado", length = 20, nullable = false)
    private String estado = "LIBRE";

    @Column(name = "cuenta_catastral", length = 120)
    private String cuentaCatastral;

    @Column(name = "numero_finca", length = 20)
    private String numeroFinca;

    @Column(name = "numero_lote", length = 20)
    private String numeroLote;

    @Column(name = "numero_manzana", length = 20)
    private String numeroManzana;

    @Column(name = "observacion", length = 250)
    private String observacion;

    // REQ-0088: operacion/tipo de contrato, medidas, anio y cantidad de unidades.
    @Column(name = "tipo_operacion", length = 10)
    private String tipoOperacion;   // ALQUILER | VENTA

    @Column(name = "medidas", length = 120)
    private String medidas;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "cantidad_unidades")
    private Integer cantidadUnidades;

    // REQ-0087: campos del formulario detallado de LOTES y CASAS/DPTOS.
    @Column(name = "superficie", precision = 15, scale = 2)
    private java.math.BigDecimal superficie;

    @Column(name = "dimensiones_linderos")
    private String dimensionesLinderos;

    @Column(name = "cochera")
    private Integer cochera;

    @Column(name = "m2_construccion", precision = 15, scale = 2)
    private java.math.BigDecimal m2Construccion;

    @Column(name = "medida", length = 120)
    private String medida;

    @Column(name = "ande_medidor", length = 40)
    private String andeMedidor;

    @Column(name = "ande_nis", length = 40)
    private String andeNis;

    @Column(name = "essap_medidor", length = 40)
    private String essapMedidor;

    @Column(name = "essap_cta_cte", length = 40)
    private String essapCtaCte;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPadre() { return padre; }
    public void setPadre(Long padre) { this.padre = padre; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getTipo() { return tipo; }
    public void setTipo(Long tipo) { this.tipo = tipo; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public Long getUbicacion() { return ubicacion; }
    public void setUbicacion(Long ubicacion) { this.ubicacion = ubicacion; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }
    public BigDecimal getComisionVenta() { return comisionVenta; }
    public void setComisionVenta(BigDecimal comisionVenta) { this.comisionVenta = comisionVenta; }
    public BigDecimal getPrecioAlquiler() { return precioAlquiler; }
    public void setPrecioAlquiler(BigDecimal precioAlquiler) { this.precioAlquiler = precioAlquiler; }
    public BigDecimal getComisionAlquiler() { return comisionAlquiler; }
    public void setComisionAlquiler(BigDecimal comisionAlquiler) { this.comisionAlquiler = comisionAlquiler; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCuentaCatastral() { return cuentaCatastral; }
    public void setCuentaCatastral(String cuentaCatastral) { this.cuentaCatastral = cuentaCatastral; }
    public String getNumeroFinca() { return numeroFinca; }
    public void setNumeroFinca(String numeroFinca) { this.numeroFinca = numeroFinca; }
    public String getNumeroLote() { return numeroLote; }
    public void setNumeroLote(String numeroLote) { this.numeroLote = numeroLote; }
    public String getNumeroManzana() { return numeroManzana; }
    public void setNumeroManzana(String numeroManzana) { this.numeroManzana = numeroManzana; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getMedidas() { return medidas; }
    public void setMedidas(String medidas) { this.medidas = medidas; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getCantidadUnidades() { return cantidadUnidades; }
    public void setCantidadUnidades(Integer cantidadUnidades) { this.cantidadUnidades = cantidadUnidades; }
    public java.math.BigDecimal getSuperficie() { return superficie; }
    public void setSuperficie(java.math.BigDecimal superficie) { this.superficie = superficie; }
    public String getDimensionesLinderos() { return dimensionesLinderos; }
    public void setDimensionesLinderos(String dimensionesLinderos) { this.dimensionesLinderos = dimensionesLinderos; }
    public Integer getCochera() { return cochera; }
    public void setCochera(Integer cochera) { this.cochera = cochera; }
    public java.math.BigDecimal getM2Construccion() { return m2Construccion; }
    public void setM2Construccion(java.math.BigDecimal m2Construccion) { this.m2Construccion = m2Construccion; }
    public String getMedida() { return medida; }
    public void setMedida(String medida) { this.medida = medida; }
    public String getAndeMedidor() { return andeMedidor; }
    public void setAndeMedidor(String andeMedidor) { this.andeMedidor = andeMedidor; }
    public String getAndeNis() { return andeNis; }
    public void setAndeNis(String andeNis) { this.andeNis = andeNis; }
    public String getEssapMedidor() { return essapMedidor; }
    public void setEssapMedidor(String essapMedidor) { this.essapMedidor = essapMedidor; }
    public String getEssapCtaCte() { return essapCtaCte; }
    public void setEssapCtaCte(String essapCtaCte) { this.essapCtaCte = essapCtaCte; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Activo)) return false;
        Activo otro = (Activo) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
