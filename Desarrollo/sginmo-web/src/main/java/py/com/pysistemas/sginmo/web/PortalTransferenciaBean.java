package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.file.UploadedFile;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.servicio.PortalTransferenciaService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * REQ-0083 (Fase 1) - Portal: el socio informa una transferencia y adjunta el comprobante, y ve el
 * estado de sus transferencias. La identidad viene de la sesion de portal (persona+tenant, REQ-0078).
 */
@Named
@ViewScoped
public class PortalTransferenciaBean implements Serializable {

    @Inject
    private transient PortalTransferenciaService servicio;
    @Inject
    private PortalSesion sesion;

    private BigDecimal importe;
    private java.time.LocalDate fecha;
    private String bancoOrigen;
    private String cuentaOrigen;
    private String numeroTransaccion;
    private String observacion;
    private transient UploadedFile comprobante;

    private List<PortalTransferenciaService.Fila> mias = List.of();

    @PostConstruct
    public void iniciar() {
        if (sesion.isAutenticado()) recargar();
    }

    public String verificarAcceso() {
        return sesion.isAutenticado() ? null : "/portal/login?faces-redirect=true";
    }

    private void recargar() {
        mias = servicio.mias(sesion.getPersona());
    }

    public void informar() {
        try {
            if (comprobante == null || comprobante.getContent() == null || comprobante.getContent().length == 0) {
                throw new NegocioException("Adjunte el comprobante de la transferencia");
            }
            PortalTransferenciaService.Datos d = new PortalTransferenciaService.Datos();
            d.importe = importe;
            d.fecha = fecha;
            d.bancoOrigen = bancoOrigen;
            d.cuentaOrigen = cuentaOrigen;
            d.numeroTransaccion = numeroTransaccion;
            d.observacion = observacion;
            servicio.informar(sesion.getPersona(), d, comprobante.getContent(),
                    comprobante.getFileName(), comprobante.getContentType());
            aviso(FacesMessage.SEVERITY_INFO, "Transferencia informada",
                    "Queda en revision. Le avisaremos cuando se aplique.");
            importe = null; fecha = null; bancoOrigen = null; cuentaOrigen = null;
            numeroTransaccion = null; observacion = null; comprobante = null;
            recargar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo informar", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
    public java.time.LocalDate getFecha() { return fecha; }
    public void setFecha(java.time.LocalDate fecha) { this.fecha = fecha; }
    public String getBancoOrigen() { return bancoOrigen; }
    public void setBancoOrigen(String bancoOrigen) { this.bancoOrigen = bancoOrigen; }
    public String getCuentaOrigen() { return cuentaOrigen; }
    public void setCuentaOrigen(String cuentaOrigen) { this.cuentaOrigen = cuentaOrigen; }
    public String getNumeroTransaccion() { return numeroTransaccion; }
    public void setNumeroTransaccion(String numeroTransaccion) { this.numeroTransaccion = numeroTransaccion; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public UploadedFile getComprobante() { return comprobante; }
    public void setComprobante(UploadedFile comprobante) { this.comprobante = comprobante; }
    public List<PortalTransferenciaService.Fila> getMias() { return mias; }
    public String getNombre() { return sesion.getNombre(); }
}
