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

/** Ingreso/egreso de caja (V1): gastos y otros ingresos con articulo como concepto. */
@jakarta.persistence.Entity
@Table(name = "ingreso_egreso")
public class IngresoEgreso extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingreso_egreso")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha = LocalDate.now();

    @Column(name = "tipo", length = 10, nullable = false)
    private String tipo = "EGRESO";     // INGRESO | EGRESO

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado = "CANCELADO";   // contado por defecto

    @Column(name = "articulo", nullable = false)
    private Long articulo;

    @Column(name = "persona")
    private Long persona;

    @Column(name = "activo")
    private Long activo;

    @Column(name = "operacion")
    private Long operacion;

    @Column(name = "tipo_imputacion_codigo", length = 40)
    private String tipoImputacionCodigo;

    @Column(name = "forma_pago")
    private Long formaPago;

    @Column(name = "tenant", nullable = false)
    private Long tenant;

    @Column(name = "observacion")
    private String observacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal v) { this.monto = v; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal v) { this.saldo = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = v; }
    public Long getArticulo() { return articulo; }
    public void setArticulo(Long v) { this.articulo = v; }
    public Long getPersona() { return persona; }
    public void setPersona(Long v) { this.persona = v; }
    public Long getActivo() { return activo; }
    public void setActivo(Long v) { this.activo = v; }
    public Long getOperacion() { return operacion; }
    public void setOperacion(Long v) { this.operacion = v; }
    public String getTipoImputacionCodigo() { return tipoImputacionCodigo; }
    public void setTipoImputacionCodigo(String v) { this.tipoImputacionCodigo = v; }
    public Long getFormaPago() { return formaPago; }
    public void setFormaPago(Long v) { this.formaPago = v; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long v) { this.tenant = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { this.observacion = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngresoEgreso)) return false;
        IngresoEgreso otro = (IngresoEgreso) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
