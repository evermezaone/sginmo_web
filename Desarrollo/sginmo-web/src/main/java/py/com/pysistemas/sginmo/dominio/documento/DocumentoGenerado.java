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

    // ── REQ-0054: estado documental operativo (independiente del archivo fisico) ──
    @Column(name = "estado_documental", length = 12)
    private String estadoDocumental = "GENERADO";   // GENERADO|IMPRESO|ENVIADO|FIRMADO|OBSERVADO|ANULADO|ARCHIVADO

    @Column(name = "fecha_impresion")
    private java.time.LocalDateTime fechaImpresion;
    @Column(name = "fecha_envio")
    private java.time.LocalDateTime fechaEnvio;
    @Column(name = "fecha_firma")
    private java.time.LocalDateTime fechaFirma;
    @Column(name = "fecha_archivo")
    private java.time.LocalDateTime fechaArchivo;

    @Column(name = "adjunto_firmado")
    private Long adjuntoFirmado;                     // FK a documento_adjunto (version firmada escaneada)

    @Column(name = "motivo_anulacion", length = 250)
    private String motivoAnulacion;
    @Column(name = "usuario_anulacion", length = 20)
    private String usuarioAnulacion;
    @Column(name = "fecha_anulacion")
    private java.time.LocalDateTime fechaAnulacion;

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

    public String getEstadoDocumental() { return estadoDocumental; }
    public void setEstadoDocumental(String v) { this.estadoDocumental = v; }
    public java.time.LocalDateTime getFechaImpresion() { return fechaImpresion; }
    public void setFechaImpresion(java.time.LocalDateTime v) { this.fechaImpresion = v; }
    public java.time.LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(java.time.LocalDateTime v) { this.fechaEnvio = v; }
    public java.time.LocalDateTime getFechaFirma() { return fechaFirma; }
    public void setFechaFirma(java.time.LocalDateTime v) { this.fechaFirma = v; }
    public java.time.LocalDateTime getFechaArchivo() { return fechaArchivo; }
    public void setFechaArchivo(java.time.LocalDateTime v) { this.fechaArchivo = v; }
    public Long getAdjuntoFirmado() { return adjuntoFirmado; }
    public void setAdjuntoFirmado(Long v) { this.adjuntoFirmado = v; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String v) { this.motivoAnulacion = v; }
    public String getUsuarioAnulacion() { return usuarioAnulacion; }
    public void setUsuarioAnulacion(String v) { this.usuarioAnulacion = v; }
    public java.time.LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(java.time.LocalDateTime v) { this.fechaAnulacion = v; }
}
