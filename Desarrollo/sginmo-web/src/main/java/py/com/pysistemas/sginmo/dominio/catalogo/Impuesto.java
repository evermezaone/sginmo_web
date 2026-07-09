package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.math.BigDecimal;

/** Impuestos (IVA PY) con base imponible parcial (decision del usuario 2026-07-05). */
@jakarta.persistence.Entity
@Table(name = "impuesto")
public class Impuesto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "impuesto")
    private Long id;

    /** Discriminador multiempresa (V26); -1 = catalogo GLOBAL. */
    @Column(name = "tenant")
    private Long tenant;
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }

    @Column(name = "descripcion", length = 120, nullable = false)
    private String descripcion;

    @Column(name = "porcentaje_impuesto", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeImpuesto;

    @Column(name = "factor_discriminado", nullable = false, precision = 15, scale = 2)
    private BigDecimal factorDiscriminado;

    @Column(name = "factor_impuesto", nullable = false, precision = 15, scale = 2)
    private BigDecimal factorImpuesto;

    @Column(name = "porcentaje_base_gravada", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeBaseGravada = new BigDecimal("100");

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPorcentajeImpuesto() { return porcentajeImpuesto; }
    public void setPorcentajeImpuesto(BigDecimal porcentajeImpuesto) { this.porcentajeImpuesto = porcentajeImpuesto; }

    public BigDecimal getFactorDiscriminado() { return factorDiscriminado; }
    public void setFactorDiscriminado(BigDecimal factorDiscriminado) { this.factorDiscriminado = factorDiscriminado; }

    public BigDecimal getFactorImpuesto() { return factorImpuesto; }
    public void setFactorImpuesto(BigDecimal factorImpuesto) { this.factorImpuesto = factorImpuesto; }

    public BigDecimal getPorcentajeBaseGravada() { return porcentajeBaseGravada; }
    public void setPorcentajeBaseGravada(BigDecimal porcentajeBaseGravada) { this.porcentajeBaseGravada = porcentajeBaseGravada; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    /** Igualdad por id: imprescindible para combos JSF (el converter carga otra instancia). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Impuesto)) return false;
        Impuesto otro = (Impuesto) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
