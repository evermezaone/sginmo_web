package py.com.pysistemas.sginmo.dominio.documento;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import py.com.one.core.Auditable;

import java.time.LocalDate;

/**
 * REQ-0053 - Adjunto documental por entidad de negocio. Tabla por-tenant (RLS V34).
 * El archivo fisico vive fuera del WAR (ruta configurable); aqui van los metadatos y
 * el nombre_fisico (UUID, sin colisiones; no se confia en el nombre original del usuario).
 */
@jakarta.persistence.Entity
@Table(name = "documento_adjunto")
public class DocumentoAdjunto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_adjunto")
    private Long id;

    @Column(name = "tenant")
    private Long tenant;

    @NotBlank(message = "El tipo es obligatorio")
    @Column(name = "tipo", length = 40, nullable = false)
    private String tipo;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @NotBlank(message = "La entidad vinculada es obligatoria")
    @Column(name = "entidad_tipo", length = 20, nullable = false)
    private String entidadTipo;      // PERSONA | ACTIVO | OPERACION | COBRO | LIQUIDACION | PLANTILLA | GENERAL

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "nombre_original", length = 255, nullable = false)
    private String nombreOriginal;

    @Column(name = "nombre_fisico", length = 120, nullable = false)
    private String nombreFisico;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "tamano", nullable = false)
    private long tamano;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getEntidadTipo() { return entidadTipo; }
    public void setEntidadTipo(String entidadTipo) { this.entidadTipo = entidadTipo; }
    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }
    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }
    public String getNombreFisico() { return nombreFisico; }
    public void setNombreFisico(String nombreFisico) { this.nombreFisico = nombreFisico; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getTamano() { return tamano; }
    public void setTamano(long tamano) { this.tamano = tamano; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentoAdjunto)) return false;
        DocumentoAdjunto otro = (DocumentoAdjunto) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
