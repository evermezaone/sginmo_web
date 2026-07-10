package py.com.pysistemas.sginmo.dominio.documento;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Historial de documentos generados desde una plantilla versionada (REQ-0041). */
@jakarta.persistence.Entity
@Table(name = "documento_generado")
public class DocumentoGenerado extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_generado")
    private Long id;

    @Column(name = "tenant", nullable = false)
    private Long tenant;

    @Column(name = "operacion", nullable = false)
    private Long operacion;

    @Column(name = "cronograma_cuota")
    private Long cronogramaCuota;

    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo;

    @Column(name = "plantilla_documento", nullable = false)
    private Long plantillaDocumento;

    @Column(name = "version_plantilla", nullable = false)
    private Integer versionPlantilla;

    @Column(name = "nombre_archivo", length = 180, nullable = false)
    private String nombreArchivo;

    @Column(name = "hash_contenido", length = 64, nullable = false)
    private String hashContenido;

    public Long getId() { return id; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public Long getOperacion() { return operacion; }
    public void setOperacion(Long operacion) { this.operacion = operacion; }
    public Long getCronogramaCuota() { return cronogramaCuota; }
    public void setCronogramaCuota(Long cronogramaCuota) { this.cronogramaCuota = cronogramaCuota; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Long getPlantillaDocumento() { return plantillaDocumento; }
    public void setPlantillaDocumento(Long plantillaDocumento) { this.plantillaDocumento = plantillaDocumento; }
    public Integer getVersionPlantilla() { return versionPlantilla; }
    public void setVersionPlantilla(Integer versionPlantilla) { this.versionPlantilla = versionPlantilla; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getHashContenido() { return hashContenido; }
    public void setHashContenido(String hashContenido) { this.hashContenido = hashContenido; }
}
