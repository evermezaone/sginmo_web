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

    // Datos del medio de pago (obs 225/226): la forma de pago define cuales son exigibles
    private String datoEmisor, datoProcesador, datoNumero, datoSerie, datoCuenta, datoReferencia;
    private java.time.LocalDate datoVencimiento;
    private Long datoCobrador, datoNotaCredito;
    private java.time.LocalDate datoFechaDeposito;
    private String datoNumeroDeposito, datoEstadoDeposito, datoMotivoRechazo;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> emisores = java.util.List.of();
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> procesadores = java.util.List.of();
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> motivosRechazo = java.util.List.of();
    private List<Persona> cobradores = java.util.List.of();
    private List<Object[]> notasCredito = java.util.List.of();

    // Anulacion de cobro con motivo obligatorio (obs 227, fiel a P_ANULARCOBRO)
    private String motivoAnulacion;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> motivosAnulacion = java.util.List.of();
    // REQ-0079: la anulacion queda escondida detras de un modo que el usuario habilita a proposito;
    // ademas solo se ofrece para el ultimo cobro ACTIVO y solo si es de hoy (el backend lo re-valida).
    private boolean modoAnulacion;

    @PostConstruct
    public void iniciar() {
        clientes = personaService.porRol("CLIENTE");
        cobradores = personaService.porRol("EMPLEADO");
        formasPago = catalogoService == null ? java.util.List.of() : formasHabilitadas();
        if (catalogoService != null) {
            emisores = catalogoService.opciones("EMISORES");
            procesadores = catalogoService.opciones("PROCESADORES");
            motivosRechazo = catalogoService.opciones("MOTIVOS_RECHAZO");
            motivosAnulacion = catalogoService.opciones("MOTIVOS_ANULACION");
        }
        refrescarPlanilla();
    }

    /** Forma de pago elegida (o null): la UI muestra/exige los datos segun sus flags. */
    public py.com.pysistemas.sginmo.dominio.catalogo.FormaPago getFormaPagoObj() {
        if (formaPagoSel == null || formasPago == null) return null;
        return formasPago.stream().filter(f -> formaPagoSel.equals(f.getId())).findFirst().orElse(null);
    }

    private void limpiarDatosCobro() {
        datoEmisor = null; datoProcesador = null; datoNumero = null; datoSerie = null;
        datoCuenta = null; datoReferencia = null; datoVencimiento = null;
        datoCobrador = null; datoNotaCredito = null; datoFechaDeposito = null;
        datoNumeroDeposito = null; datoEstadoDeposito = null; datoMotivoRechazo = null;
    }

    public void formaPagoCambiada() { limpiarDatosCobro(); }

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
        notasCredito = cajaService.notasCreditoDe(clienteSel);
    }

    public void documentoCambiado() {
        cuotas = documentoSel == null ? java.util.List.of() : cajaService.cuotasPendientesDeDocumento(documentoSel);
    }

    public void cobrar() {
        try {
            if (planilla == null) throw new NegocioException("Abra la caja primero");
            if (documentoSel == null) throw new NegocioException("Elija el documento a cobrar");
            long cobro = cajaService.cobrar(documentoSel, planilla.getId(), formaPagoSel, clienteSel,
                    montoCobro, contexto.getEmpresa() != null ? monedaLocal() : null, sesion.codigoUsuario(),
                    datoEmisor, datoProcesador, datoNumero, datoSerie, datoCuenta, datoVencimiento, datoReferencia,
                    datoCobrador, datoFechaDeposito, datoNumeroDeposito, datoEstadoDeposito, datoMotivoRechazo, datoNotaCredito);
            aviso(FacesMessage.SEVERITY_INFO, "Cobro registrado", "Recibo #" + cobro);
            montoCobro = BigDecimal.ZERO;
            limpiarDatosCobro();
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
            cajaService.anularCobro(cobroId, sesion.codigoUsuario(), motivoAnulacion);
            aviso(FacesMessage.SEVERITY_INFO, "Cobro anulado", "Se repuso el saldo y las cuotas");
            motivoAnulacion = null;
            modoAnulacion = false;   // REQ-0079: al anular se vuelve a esconder la opcion
            planilla = cajaService.planillaAbierta(contexto.getEmpresa().getId(), contexto.sucursal().getId());
            cobros = cajaService.cobrosDePlanilla(planilla.getId());
            if (clienteSel != null) documentos = cajaService.documentosPendientesDe(clienteSel);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo anular", e.getMessage());
        }
    }

    /** REQ-0079: habilita/deshabilita el modo anulacion (control escondido). */
    public void alternarModoAnulacion() { modoAnulacion = !modoAnulacion; }

    /**
     * REQ-0079: id del unico cobro anulable = el ultimo cobro ACTIVO de la planilla, y solo si es de
     * HOY. Como {@code cobros} viene ordenado por id DESC, el primer ACTIVO es el mas reciente. Null si
     * no hay ninguno anulable (el backend igual re-valida esta regla).
     */
    public Long getCobroAnulableId() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        for (Object[] cb : cobros) {
            if ("ACTIVO".equals(cb[5])) {
                java.time.LocalDate f = cb[1] instanceof java.sql.Date d ? d.toLocalDate()
                        : (cb[1] instanceof java.time.LocalDate ld ? ld : null);
                return (f != null && f.isEqual(hoy)) ? ((Number) cb[0]).longValue() : null;
            }
        }
        return null;
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
    public String getDatoEmisor() { return datoEmisor; }
    public void setDatoEmisor(String v) { this.datoEmisor = v; }
    public String getDatoProcesador() { return datoProcesador; }
    public void setDatoProcesador(String v) { this.datoProcesador = v; }
    public String getDatoNumero() { return datoNumero; }
    public void setDatoNumero(String v) { this.datoNumero = v; }
    public String getDatoSerie() { return datoSerie; }
    public void setDatoSerie(String v) { this.datoSerie = v; }
    public String getDatoCuenta() { return datoCuenta; }
    public void setDatoCuenta(String v) { this.datoCuenta = v; }
    public String getDatoReferencia() { return datoReferencia; }
    public void setDatoReferencia(String v) { this.datoReferencia = v; }
    public java.time.LocalDate getDatoVencimiento() { return datoVencimiento; }
    public void setDatoVencimiento(java.time.LocalDate v) { this.datoVencimiento = v; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> getEmisores() { return emisores; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> getProcesadores() { return procesadores; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> getMotivosRechazo() { return motivosRechazo; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> getMotivosAnulacion() { return motivosAnulacion; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String v) { this.motivoAnulacion = v; }
    public boolean isModoAnulacion() { return modoAnulacion; }
    public List<Persona> getCobradores() { return cobradores; }
    public List<Object[]> getNotasCredito() { return notasCredito; }
    public Long getDatoCobrador() { return datoCobrador; }
    public void setDatoCobrador(Long v) { this.datoCobrador = v; }
    public Long getDatoNotaCredito() { return datoNotaCredito; }
    public void setDatoNotaCredito(Long v) { this.datoNotaCredito = v; }
    public java.time.LocalDate getDatoFechaDeposito() { return datoFechaDeposito; }
    public void setDatoFechaDeposito(java.time.LocalDate v) { this.datoFechaDeposito = v; }
    public String getDatoNumeroDeposito() { return datoNumeroDeposito; }
    public void setDatoNumeroDeposito(String v) { this.datoNumeroDeposito = v; }
    public String getDatoEstadoDeposito() { return datoEstadoDeposito; }
    public void setDatoEstadoDeposito(String v) { this.datoEstadoDeposito = v; }
    public String getDatoMotivoRechazo() { return datoMotivoRechazo; }
    public void setDatoMotivoRechazo(String v) { this.datoMotivoRechazo = v; }
}
