package py.com.pysistemas.sginmo.dominio.operacion;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Operacion de alquiler o venta (V1). El cuadre financiero vive en la BD (V16/V17). */
@jakarta.persistence.Entity
@Table(name = "operacion")
public class Operacion extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operacion")
    private Long id;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion = LocalDate.now();

    @Column(name = "tipo_operacion", length = 10, nullable = false)
    private String tipoOperacion = "ALQUILER";     // ALQUILER | VENTA

    @Column(name = "cliente", nullable = false)
    private Long cliente;

    @Column(name = "vendedor")
    private Long vendedor;

    @Column(name = "activo", nullable = false)
    private Long activo;

    @Column(name = "fecha_inicio_contrato", nullable = false)
    private LocalDate fechaInicioContrato = LocalDate.now();

    @Column(name = "fecha_fin_contrato")
    private LocalDate fechaFinContrato;

    @Column(name = "fecha_finalizacion")
    private LocalDate fechaFinalizacion;

    @Column(name = "fecha_renovacion")
    private LocalDate fechaRenovacion;

    @Column(name = "plazo")
    private Integer plazo;

    @Column(name = "precio", nullable = false, precision = 15, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "monto_total_operacion", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotalOperacion = BigDecimal.ZERO;

    @Column(name = "garantia", nullable = false, precision = 15, scale = 2)
    private BigDecimal garantia = BigDecimal.ZERO;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado = "VIGENTE";             // VIGENTE | FINALIZADO

    @Column(name = "empresa", nullable = false)
    private Long empresa;

    @Column(name = "sucursal", nullable = false)
    private Long sucursal;

    @Column(name = "moneda", nullable = false)
    private Long moneda;

    @Column(name = "condicion_operacion", length = 10, nullable = false)
    private String condicionOperacion = "CREDITO"; // CONTADO | CREDITO

    @Column(name = "dia_pago")
    private Integer diaPago;

    @Column(name = "monto_mora", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoMora = BigDecimal.ZERO;

    @Column(name = "dias_gracia")
    private Integer diasGracia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFechaOperacion() { return fechaOperacion; }
    public void setFechaOperacion(LocalDate v) { this.fechaOperacion = v; }
    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String v) { this.tipoOperacion = v; }
    public Long getCliente() { return cliente; }
    public void setCliente(Long v) { this.cliente = v; }
    public Long getVendedor() { return vendedor; }
    public void setVendedor(Long v) { this.vendedor = v; }
    public Long getActivo() { return activo; }
    public void setActivo(Long v) { this.activo = v; }
    public LocalDate getFechaInicioContrato() { return fechaInicioContrato; }
    public void setFechaInicioContrato(LocalDate v) { this.fechaInicioContrato = v; }
    public LocalDate getFechaFinContrato() { return fechaFinContrato; }
    public void setFechaFinContrato(LocalDate v) { this.fechaFinContrato = v; }
    public LocalDate getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(LocalDate v) { this.fechaFinalizacion = v; }
    public LocalDate getFechaRenovacion() { return fechaRenovacion; }
    public void setFechaRenovacion(LocalDate v) { this.fechaRenovacion = v; }
    public Integer getPlazo() { return plazo; }
    public void setPlazo(Integer v) { this.plazo = v; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal v) { this.precio = v; }
    public BigDecimal getMontoTotalOperacion() { return montoTotalOperacion; }
    public void setMontoTotalOperacion(BigDecimal v) { this.montoTotalOperacion = v; }
    public BigDecimal getGarantia() { return garantia; }
    public void setGarantia(BigDecimal v) { this.garantia = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = v; }
    public Long getEmpresa() { return empresa; }
    public void setEmpresa(Long v) { this.empresa = v; }
    public Long getSucursal() { return sucursal; }
    public void setSucursal(Long v) { this.sucursal = v; }
    public Long getMoneda() { return moneda; }
    public void setMoneda(Long v) { this.moneda = v; }
    public String getCondicionOperacion() { return condicionOperacion; }
    public void setCondicionOperacion(String v) { this.condicionOperacion = v; }
    public Integer getDiaPago() { return diaPago; }
    public void setDiaPago(Integer v) { this.diaPago = v; }
    public BigDecimal getMontoMora() { return montoMora; }
    public void setMontoMora(BigDecimal v) { this.montoMora = v; }
    public Integer getDiasGracia() { return diasGracia; }
    public void setDiasGracia(Integer v) { this.diasGracia = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operacion)) return false;
        Operacion otro = (Operacion) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
