package py.com.pysistemas.sginmo.dominio.documento;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Plantilla configurable para contratos y pagares (REQ-0041). */
@jakarta.persistence.Entity
@Table(name = "plantilla_documento")
public class PlantillaDocumento extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plantilla_documento")
    private Long id;

    @Column(name = "tenant", nullable = false)
    private Long tenant;

    @Column(name = "codigo", length = 60, nullable = false)
    private String codigo;

    @Column(name = "descripcion", length = 180, nullable = false)
    private String descripcion;

    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo = "CONTRATO";

    @Column(name = "tipo_operacion", length = 20)
    private String tipoOperacion;

    @Column(name = "tipo_contrato")
    private Long tipoContrato;

    @Column(name = "version_plantilla", nullable = false)
    private Integer versionPlantilla = 1;

    @Column(name = "formato_cuerpo", length = 20, nullable = false)
    private String formatoCuerpo = "TEXTO";

    @Column(name = "cuerpo", nullable = false)
    private String cuerpo;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public Long getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(Long tipoContrato) { this.tipoContrato = tipoContrato; }
    public Integer getVersionPlantilla() { return versionPlantilla; }
    public void setVersionPlantilla(Integer versionPlantilla) { this.versionPlantilla = versionPlantilla; }
    public String getFormatoCuerpo() { return formatoCuerpo; }
    public void setFormatoCuerpo(String formatoCuerpo) { this.formatoCuerpo = formatoCuerpo; }
    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlantillaDocumento)) return false;
        PlantillaDocumento otro = (PlantillaDocumento) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
