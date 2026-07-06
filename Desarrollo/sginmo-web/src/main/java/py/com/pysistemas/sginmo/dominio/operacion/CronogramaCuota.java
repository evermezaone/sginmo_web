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

/** Cuota del cronograma (V1). La generan y cancelan los SPs del motor (V16/V17). */
@jakarta.persistence.Entity
@Table(name = "cronograma_cuota")
public class CronogramaCuota extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cronograma_cuota")
    private Long id;

    @Column(name = "operacion", nullable = false)
    private Long operacion;

    @Column(name = "documento")
    private Long documento;

    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;

    @Column(name = "fecha_cancelacion")
    private LocalDate fechaCancelacion;

    public Long getId() { return id; }
    public Long getOperacion() { return operacion; }
    public Long getDocumento() { return documento; }
    public Integer getNumeroCuota() { return numeroCuota; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getSaldo() { return saldo; }
    public String getEstado() { return estado; }
    public LocalDate getFechaCancelacion() { return fechaCancelacion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CronogramaCuota)) return false;
        CronogramaCuota otro = (CronogramaCuota) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
