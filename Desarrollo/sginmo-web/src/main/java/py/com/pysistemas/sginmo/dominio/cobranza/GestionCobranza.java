package py.com.pysistemas.sginmo.dominio.cobranza;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import py.com.one.core.Auditable;

import java.time.LocalDate;

/** REQ-0057 - Gestion de cobranza sobre una cuota/operacion vencida. No modifica la cuota. */
@jakarta.persistence.Entity
@Table(name = "gestion_cobranza")
public class GestionCobranza extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gestion_cobranza")
    private Long id;

    @Column(name = "tenant")
    private Long tenant;
    @Column(name = "operacion")
    private Long operacion;
    @Column(name = "cronograma_cuota")
    private Long cronogramaCuota;
    @Column(name = "cliente")
    private Long cliente;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;
    @Column(name = "responsable", length = 20)
    private String responsable;

    @NotBlank(message = "El resultado es obligatorio")
    @Column(name = "resultado", length = 20, nullable = false)
    private String resultado;   // CONTACTADO|NO_CONTACTADO|COMPROMISO|RECHAZO|ILOCALIZABLE|OTRO

    @Column(name = "comentario", length = 500)
    private String comentario;
    @Column(name = "proxima_accion", length = 250)
    private String proximaAccion;
    @Column(name = "proxima_fecha")
    private LocalDate proximaFecha;

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
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public String getProximaAccion() { return proximaAccion; }
    public void setProximaAccion(String v) { this.proximaAccion = v; }
    public LocalDate getProximaFecha() { return proximaFecha; }
    public void setProximaFecha(LocalDate v) { this.proximaFecha = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GestionCobranza g)) return false;
        return id != null && id.equals(g.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
