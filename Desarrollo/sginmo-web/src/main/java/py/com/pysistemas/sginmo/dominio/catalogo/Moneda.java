package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Monedas del sistema (V1; estado agregado en V12). */
@jakarta.persistence.Entity
@Table(name = "moneda")
public class Moneda extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "moneda")
    private Long id;

    /** Discriminador multiempresa (V26); -1 = catalogo GLOBAL. */
    @Column(name = "tenant")
    private Long tenant;
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }

    @Column(name = "descripcion", length = 120, nullable = false)
    private String descripcion;

    @Column(name = "simbolo", length = 20, nullable = false)
    private String simbolo;

    @Column(name = "tipo_moneda", length = 20, nullable = false)
    private String tipoMoneda = "EXTRANJERA";     // LOCAL | EXTRANJERA

    @Column(name = "precision_decimales", nullable = false)
    private Integer precisionDecimales = 0;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }
    public String getTipoMoneda() { return tipoMoneda; }
    public void setTipoMoneda(String tipoMoneda) { this.tipoMoneda = tipoMoneda; }
    public Integer getPrecisionDecimales() { return precisionDecimales; }
    public void setPrecisionDecimales(Integer precisionDecimales) { this.precisionDecimales = precisionDecimales; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Moneda)) return false;
        Moneda otro = (Moneda) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
