package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Formas de pago (V1/V2) con flags de requisitos del cobro y habilitado (V12, regla 1). */
@jakarta.persistence.Entity
@Table(name = "forma_pago")
public class FormaPago extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forma_pago")
    private Long id;

    /** Discriminador multiempresa (V26); -1 = catalogo GLOBAL. */
    @Column(name = "tenant")
    private Long tenant;
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }

    @Column(name = "codigo", length = 20, nullable = false, unique = true)
    private String codigo;

    @Column(name = "descripcion", length = 120, nullable = false)
    private String descripcion;

    @Column(name = "dias")
    private Integer dias;

    @Column(name = "por_defecto", nullable = false)
    private boolean porDefecto;

    @Column(name = "requiere_emisor", nullable = false)          private boolean requiereEmisor;
    @Column(name = "requiere_procesador", nullable = false)      private boolean requiereProcesador;
    @Column(name = "requiere_numero", nullable = false)          private boolean requiereNumero;
    @Column(name = "requiere_serie", nullable = false)           private boolean requiereSerie;
    @Column(name = "requiere_vencimiento", nullable = false)     private boolean requiereVencimiento;
    @Column(name = "requiere_cuenta", nullable = false)          private boolean requiereCuenta;
    @Column(name = "requiere_referencia", nullable = false)      private boolean requiereReferencia;
    @Column(name = "requiere_cobrador", nullable = false)        private boolean requiereCobrador;
    @Column(name = "requiere_fecha_deposito", nullable = false)  private boolean requiereFechaDeposito;
    @Column(name = "requiere_numero_deposito", nullable = false) private boolean requiereNumeroDeposito;
    @Column(name = "requiere_estado_deposito", nullable = false) private boolean requiereEstadoDeposito;
    @Column(name = "requiere_motivo_rechazo", nullable = false)  private boolean requiereMotivoRechazo;
    @Column(name = "requiere_nota_credito", nullable = false)    private boolean requiereNotaCredito;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    /** Disponible para cobros NUEVOS (V12, regla 1): independiente del estado. */
    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = Boolean.TRUE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getDias() { return dias; }
    public void setDias(Integer dias) { this.dias = dias; }
    public boolean isPorDefecto() { return porDefecto; }
    public void setPorDefecto(boolean porDefecto) { this.porDefecto = porDefecto; }
    public boolean isRequiereEmisor() { return requiereEmisor; }
    public void setRequiereEmisor(boolean v) { this.requiereEmisor = v; }
    public boolean isRequiereProcesador() { return requiereProcesador; }
    public void setRequiereProcesador(boolean v) { this.requiereProcesador = v; }
    public boolean isRequiereNumero() { return requiereNumero; }
    public void setRequiereNumero(boolean v) { this.requiereNumero = v; }
    public boolean isRequiereSerie() { return requiereSerie; }
    public void setRequiereSerie(boolean v) { this.requiereSerie = v; }
    public boolean isRequiereVencimiento() { return requiereVencimiento; }
    public void setRequiereVencimiento(boolean v) { this.requiereVencimiento = v; }
    public boolean isRequiereCuenta() { return requiereCuenta; }
    public void setRequiereCuenta(boolean v) { this.requiereCuenta = v; }
    public boolean isRequiereReferencia() { return requiereReferencia; }
    public void setRequiereReferencia(boolean v) { this.requiereReferencia = v; }
    public boolean isRequiereCobrador() { return requiereCobrador; }
    public void setRequiereCobrador(boolean v) { this.requiereCobrador = v; }
    public boolean isRequiereFechaDeposito() { return requiereFechaDeposito; }
    public void setRequiereFechaDeposito(boolean v) { this.requiereFechaDeposito = v; }
    public boolean isRequiereNumeroDeposito() { return requiereNumeroDeposito; }
    public void setRequiereNumeroDeposito(boolean v) { this.requiereNumeroDeposito = v; }
    public boolean isRequiereEstadoDeposito() { return requiereEstadoDeposito; }
    public void setRequiereEstadoDeposito(boolean v) { this.requiereEstadoDeposito = v; }
    public boolean isRequiereMotivoRechazo() { return requiereMotivoRechazo; }
    public void setRequiereMotivoRechazo(boolean v) { this.requiereMotivoRechazo = v; }
    public boolean isRequiereNotaCredito() { return requiereNotaCredito; }
    public void setRequiereNotaCredito(boolean v) { this.requiereNotaCredito = v; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Boolean getHabilitado() { return habilitado; }
    public void setHabilitado(Boolean habilitado) { this.habilitado = habilitado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormaPago)) return false;
        FormaPago otro = (FormaPago) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
