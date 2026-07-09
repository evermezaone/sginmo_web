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

/** Liquidacion de una operacion (V1): garantia menos gastos = saldo a devolver/cobrar. */
@jakarta.persistence.Entity
@Table(name = "liquidacion")
public class Liquidacion extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "liquidacion")
    private Long id;

    @Column(name = "operacion", nullable = false)
    private Long operacion;

    @Column(name = "documento")
    private Long documento;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha = LocalDate.now();

    @Column(name = "fecha_fiscalizacion", nullable = false)
    private LocalDate fechaFiscalizacion = LocalDate.now();

    @Column(name = "entrego_llaves", nullable = false)
    private boolean entregoLlaves;

    @Column(name = "total_garantia", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGarantia = BigDecimal.ZERO;

    @Column(name = "total_gastos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGastos = BigDecimal.ZERO;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "motivo")
    private Long motivo;

    @Column(name = "observacion", length = 180)
    private String observacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperacion() { return operacion; }
    public void setOperacion(Long v) { this.operacion = v; }
    public Long getDocumento() { return documento; }
    public void setDocumento(Long v) { this.documento = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public LocalDate getFechaFiscalizacion() { return fechaFiscalizacion; }
    public void setFechaFiscalizacion(LocalDate v) { this.fechaFiscalizacion = v; }
    public boolean isEntregoLlaves() { return entregoLlaves; }
    public void setEntregoLlaves(boolean v) { this.entregoLlaves = v; }
    public BigDecimal getTotalGarantia() { return totalGarantia; }
    public void setTotalGarantia(BigDecimal v) { this.totalGarantia = v; }
    public BigDecimal getTotalGastos() { return totalGastos; }
    public void setTotalGastos(BigDecimal v) { this.totalGastos = v; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal v) { this.saldo = v; }
    public Long getMotivo() { return motivo; }
    public void setMotivo(Long v) { this.motivo = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { this.observacion = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Liquidacion)) return false;
        Liquidacion otro = (Liquidacion) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
