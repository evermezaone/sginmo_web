package py.com.pysistemas.sginmo.dominio.cobranza;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import py.com.one.core.Auditable;

import java.math.BigDecimal;
import java.time.LocalDate;

/** REQ-0057 - Promesa de pago. No es un pago ni cambia el estado de la cuota. */
@jakarta.persistence.Entity
@Table(name = "promesa_pago")
public class PromesaPago extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promesa_pago")
    private Long id;

    @Column(name = "tenant")
    private Long tenant;
    @Column(name = "operacion")
    private Long operacion;
    @Column(name = "cronograma_cuota")
    private Long cronogramaCuota;
    @Column(name = "cliente")
    private Long cliente;

    @NotNull(message = "La fecha de promesa es obligatoria")
    @Column(name = "fecha_promesa", nullable = false)
    private LocalDate fechaPromesa;

    @NotNull(message = "El monto es obligatorio")
    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda")
    private Long moneda;

    @Column(name = "estado", length = 12, nullable = false)
    private String estado = "PENDIENTE";   // PENDIENTE|CUMPLIDA|INCUMPLIDA

    @Column(name = "comentario", length = 500)
    private String comentario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public Long getOperacion() { return operacion; }
    public void setOperacion(Long operacion) { this.operacion = operacion; }
    public Long getCronogramaCuota() { return cronogramaCuota; }
    public void setCronogramaCuota(Long v) { this.cronogramaCuota = v; }
    public Long getCliente() { return cliente; }
    public void setCliente(Long cliente) { this.cliente = cliente; }
    public LocalDate getFechaPromesa() { return fechaPromesa; }
    public void setFechaPromesa(LocalDate v) { this.fechaPromesa = v; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public Long getMoneda() { return moneda; }
    public void setMoneda(Long moneda) { this.moneda = moneda; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromesaPago p)) return false;
        return id != null && id.equals(p.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
