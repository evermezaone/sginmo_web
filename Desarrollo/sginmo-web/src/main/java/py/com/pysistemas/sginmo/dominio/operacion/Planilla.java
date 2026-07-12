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
import java.time.LocalDateTime;

/** Planilla de caja diaria (V1, patron Gestion): una ABIERTA por sucursal. */
@jakarta.persistence.Entity
@Table(name = "planilla")
public class Planilla extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planilla")
    private Long id;

    @Column(name = "tenant", nullable = false)
    private Long tenant;

    @Column(name = "sucursal", nullable = false)
    private Long sucursal;

    @Column(name = "usuario_apertura", length = 20, nullable = false)
    private String usuarioApertura;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura;

    @Column(name = "hora_apertura", nullable = false)
    private LocalDateTime horaApertura;

    @Column(name = "monto_apertura", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoApertura = BigDecimal.ZERO;

    @Column(name = "usuario_cierre", length = 20)
    private String usuarioCierre;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Column(name = "hora_cierre")
    private LocalDateTime horaCierre;

    @Column(name = "monto_cobro", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoCobro = BigDecimal.ZERO;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ABIERTA";

    // ── REQ-0059: arqueo y cierre controlado ──
    @Column(name = "efectivo_esperado", precision = 15, scale = 2)
    private BigDecimal efectivoEsperado;
    @Column(name = "efectivo_contado", precision = 15, scale = 2)
    private BigDecimal efectivoContado;
    @Column(name = "diferencia", precision = 15, scale = 2)
    private BigDecimal diferencia;
    @Column(name = "observacion_cierre", length = 300)
    private String observacionCierre;
    @Column(name = "reabierta")
    private Boolean reabierta = Boolean.FALSE;
    @Column(name = "usuario_reapertura", length = 20)
    private String usuarioReapertura;
    @Column(name = "fecha_reapertura")
    private LocalDateTime fechaReapertura;
    @Column(name = "motivo_reapertura", length = 250)
    private String motivoReapertura;

    public Long getId() { return id; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long v) { this.tenant = v; }
    public Long getSucursal() { return sucursal; }
    public void setSucursal(Long v) { this.sucursal = v; }
    public String getUsuarioApertura() { return usuarioApertura; }
    public void setUsuarioApertura(String v) { this.usuarioApertura = v; }
    public LocalDate getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDate v) { this.fechaApertura = v; }
    public LocalDateTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalDateTime v) { this.horaApertura = v; }
    public BigDecimal getMontoApertura() { return montoApertura; }
    public void setMontoApertura(BigDecimal v) { this.montoApertura = v; }
    public String getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(String v) { this.usuarioCierre = v; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate v) { this.fechaCierre = v; }
    public LocalDateTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalDateTime v) { this.horaCierre = v; }
    public BigDecimal getMontoCobro() { return montoCobro; }
    public void setMontoCobro(BigDecimal v) { this.montoCobro = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = v; }

    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal v) { this.efectivoEsperado = v; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal v) { this.efectivoContado = v; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal v) { this.diferencia = v; }
    public String getObservacionCierre() { return observacionCierre; }
    public void setObservacionCierre(String v) { this.observacionCierre = v; }
    public Boolean getReabierta() { return reabierta; }
    public void setReabierta(Boolean v) { this.reabierta = v; }
    public String getUsuarioReapertura() { return usuarioReapertura; }
    public void setUsuarioReapertura(String v) { this.usuarioReapertura = v; }
    public LocalDateTime getFechaReapertura() { return fechaReapertura; }
    public void setFechaReapertura(LocalDateTime v) { this.fechaReapertura = v; }
    public String getMotivoReapertura() { return motivoReapertura; }
    public void setMotivoReapertura(String v) { this.motivoReapertura = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Planilla)) return false;
        Planilla otro = (Planilla) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
