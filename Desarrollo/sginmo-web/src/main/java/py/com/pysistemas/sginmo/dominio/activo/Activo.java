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

    @Column(name = "tipo_codigo", length = 40, nullable = false)
    private String tipoCodigo;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPadre() { return padre; }
    public void setPadre(Long padre) { this.padre = padre; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipoCodigo() { return tipoCodigo; }
    public void setTipoCodigo(String tipoCodigo) { this.tipoCodigo = tipoCodigo; }
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
