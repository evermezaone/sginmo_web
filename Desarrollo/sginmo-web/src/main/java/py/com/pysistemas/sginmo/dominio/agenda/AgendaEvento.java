package py.com.pysistemas.sginmo.dominio.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import py.com.one.core.Auditable;

import java.time.LocalDate;

/**
 * REQ-0052 - Evento de agenda: recordatorio, tarea, vencimiento o promesa.
 * Tabla de negocio por-tenant (RLS V33). Los eventos automaticos llevan
 * origen_tabla/origen_id (dedup por indice unico); las tareas manuales, no.
 * Estados como varchar+CHECK (el proyecto no usa @Enumerated).
 */
@jakarta.persistence.Entity
@Table(name = "agenda_evento")
public class AgendaEvento extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agenda_evento")
    private Long id;

    /** Discriminador multiempresa; nunca -1 (no hay eventos globales). */
    @Column(name = "tenant")
    private Long tenant;

    @NotBlank(message = "El tipo es obligatorio")
    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo;                 // RECORDATORIO | TAREA | VENCIMIENTO | PROMESA

    @NotBlank(message = "El titulo es obligatorio")
    @Column(name = "titulo", length = 180, nullable = false)
    private String titulo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @NotNull(message = "La fecha es obligatoria")
    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    @Column(name = "prioridad", length = 10, nullable = false)
    private String prioridad = "MEDIA";  // BAJA | MEDIA | ALTA

    @Column(name = "responsable", length = 20)
    private String responsable;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "PENDIENTE"; // PENDIENTE | EN_CURSO | RESUELTO | CERRADO

    @Column(name = "origen_tabla", length = 30)
    private String origenTabla;

    @Column(name = "origen_id")
    private Long origenId;

    @Column(name = "operacion")
    private Long operacion;

    @Column(name = "persona")
    private Long persona;

    @Column(name = "activo")
    private Long activo;

    @Column(name = "cobro")
    private Long cobro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDate fechaEvento) { this.fechaEvento = fechaEvento; }
    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getOrigenTabla() { return origenTabla; }
    public void setOrigenTabla(String origenTabla) { this.origenTabla = origenTabla; }
    public Long getOrigenId() { return origenId; }
    public void setOrigenId(Long origenId) { this.origenId = origenId; }
    public Long getOperacion() { return operacion; }
    public void setOperacion(Long operacion) { this.operacion = operacion; }
    public Long getPersona() { return persona; }
    public void setPersona(Long persona) { this.persona = persona; }
    public Long getActivo() { return activo; }
    public void setActivo(Long activo) { this.activo = activo; }
    public Long getCobro() { return cobro; }
    public void setCobro(Long cobro) { this.cobro = cobro; }

    /** Igualdad por id (estandar del proyecto). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgendaEvento)) return false;
        AgendaEvento otro = (AgendaEvento) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
