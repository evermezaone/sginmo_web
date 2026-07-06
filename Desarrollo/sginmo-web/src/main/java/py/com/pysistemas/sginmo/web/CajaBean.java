package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.dominio.operacion.Planilla;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.servicio.CajaService;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.PersonaService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** Caja diaria y cobros (REQ-0022/0023). Todo el cuadre lo hacen los SPs del motor. */
@Named
@ViewScoped
public class CajaBean implements Serializable {

    public static final String PANTALLA = "caja";

    @Inject
    private transient CajaService cajaService;

    @Inject
    private transient PersonaService personaService;

    @Inject
    private transient CatalogoService catalogoService;

    @Inject
    private ContextoEmpresa contexto;

    @Inject
    private SesionUsuario sesion;

    private Planilla planilla;
    private BigDecimal montoApertura = BigDecimal.ZERO;

    private List<Persona> clientes;
    private Long clienteSel;
    private List<Object[]> documentos = java.util.List.of();
    private Long documentoSel;
    private List<Object[]> cuotas = java.util.List.of();
    private List<Object[]> cobros = java.util.List.of();

    private BigDecimal montoCobro = BigDecimal.ZERO;
    private Long formaPagoSel;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.FormaPago> formasPago;

    @PostConstruct
    public void iniciar() {
        clientes = personaService.porRol("CLIENTE");
        formasPago = catalogoService == null ? java.util.List.of() : formasHabilitadas();
        refrescarPlanilla();
    }

    private List<py.com.pysistemas.sginmo.dominio.catalogo.FormaPago> formasHabilitadas() {
        return catalogoService.formasHabilitadas();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    private void refrescarPlanilla() {
        if (contexto.getEmpresa() != null && contexto.sucursal() != null) {
            planilla = cajaService.planillaAbierta(contexto.getEmpresa().getId(), contexto.sucursal().getId());
            if (planilla != null) {
                cobros = cajaService.cobrosDePlanilla(planilla.getId());
            }
        }
    }

    public boolean isCajaAbierta() { return planilla != null; }

    public void abrirCaja() {
        try {
            if (contexto.getEmpresa() == null || contexto.sucursal() == null) {
                throw new NegocioException("Seleccione empresa/sucursal en la barra superior");
            }
            planilla = cajaService.abrirPlanilla(contexto.getEmpresa().getId(), contexto.sucursal().getId(),
                    montoApertura, sesion.codigoUsuario());
            cobros = java.util.List.of();
            aviso(FacesMessage.SEVERITY_INFO, "Caja abierta", "Planilla #" + planilla.getId());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo abrir la caja", e.getMessage());
        }
    }

    public void cerrarCaja() {
        try {
            cajaService.cerrarPlanilla(planilla.getId(), sesion.codigoUsuario());
            aviso(FacesMessage.SEVERITY_INFO, "Caja cerrada",
                    "Total cobrado: " + planilla.getMontoCobro());
            planilla = null; cobros = java.util.List.of();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cerrar", e.getMessage());
        }
    }

    public void clienteCambiado() {
        documentos = clienteSel == null ? java.util.List.of() : cajaService.documentosPendientesDe(clienteSel);
        documentoSel = null; cuotas = java.util.List.of();
    }

    public void documentoCambiado() {
        cuotas = documentoSel == null ? java.util.List.of() : cajaService.cuotasPendientesDeDocumento(documentoSel);
    }

    public void cobrar() {
        try {
            if (planilla == null) throw new NegocioException("Abra la caja primero");
            if (documentoSel == null) throw new NegocioException("Elija el documento a cobrar");
            long cobro = cajaService.cobrar(documentoSel, planilla.getId(), formaPagoSel, clienteSel,
                    montoCobro, contexto.getEmpresa() != null ? monedaLocal() : null, sesion.codigoUsuario());
            aviso(FacesMessage.SEVERITY_INFO, "Cobro registrado", "Recibo #" + cobro);
            montoCobro = BigDecimal.ZERO;
            planilla = cajaService.planillaAbierta(contexto.getEmpresa().getId(), contexto.sucursal().getId());
            documentos = cajaService.documentosPendientesDe(clienteSel);
            cuotas = documentoSel != null ? cajaService.cuotasPendientesDeDocumento(documentoSel) : java.util.List.of();
            cobros = cajaService.cobrosDePlanilla(planilla.getId());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cobrar", e.getMessage());
        }
    }

    public void anular(Long cobroId) {
        try {
            cajaService.anularCobro(cobroId, sesion.codigoUsuario());
            aviso(FacesMessage.SEVERITY_INFO, "Cobro anulado", "Se repuso el saldo y las cuotas");
            planilla = cajaService.planillaAbierta(contexto.getEmpresa().getId(), contexto.sucursal().getId());
            cobros = cajaService.cobrosDePlanilla(planilla.getId());
            if (clienteSel != null) documentos = cajaService.documentosPendientesDe(clienteSel);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo anular", e.getMessage());
        }
    }

    private Long monedaLocal() {
        return catalogoService.monedaLocalId();
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public Planilla getPlanilla() { return planilla; }
    public BigDecimal getMontoApertura() { return montoApertura; }
    public void setMontoApertura(BigDecimal v) { this.montoApertura = v; }
    public List<Persona> getClientes() { return clientes; }
    public Long getClienteSel() { return clienteSel; }
    public void setClienteSel(Long v) { this.clienteSel = v; }
    public List<Object[]> getDocumentos() { return documentos; }
    public Long getDocumentoSel() { return documentoSel; }
    public void setDocumentoSel(Long v) { this.documentoSel = v; }
    public List<Object[]> getCuotas() { return cuotas; }
    public List<Object[]> getCobros() { return cobros; }
    public BigDecimal getMontoCobro() { return montoCobro; }
    public void setMontoCobro(BigDecimal v) { this.montoCobro = v; }
    public Long getFormaPagoSel() { return formaPagoSel; }
    public void setFormaPagoSel(Long v) { this.formaPagoSel = v; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.FormaPago> getFormasPago() { return formasPago; }
}
