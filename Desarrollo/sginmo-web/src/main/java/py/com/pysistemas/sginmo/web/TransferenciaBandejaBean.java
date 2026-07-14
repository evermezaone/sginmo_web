package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.CajaService;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.PortalTransferenciaService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;

/**
 * REQ-0083 (Fase 1) - Bandeja operativa interna de transferencias informadas: revisar, ver comprobante,
 * observar/rechazar con motivo y APROBAR (aplica el cobro con el motor de caja contra la caja abierta).
 */
@Named
@ViewScoped
public class TransferenciaBandejaBean implements Serializable {

    public static final String PANTALLA = "transferencias";

    @Inject
    private transient PortalTransferenciaService servicio;
    @Inject
    private transient CajaService cajaService;
    @Inject
    private transient CatalogoService catalogoService;
    @Inject
    private ContextoEmpresa contexto;
    @Inject
    private SesionUsuario sesion;

    private String filtroEstado;
    private List<PortalTransferenciaService.Fila> lista = List.of();
    private PortalTransferenciaService.Fila seleccionada;
    private String motivo;

    // Aprobacion.
    private List<Object[]> documentos = List.of();
    private Long documentoSel;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> emisores = List.of();
    private String emisorSel;
    // REQ-0094: pagos por QR conciliados (listos para aplicar).
    private List<Object[]> pagosQr = List.of();
    // Conciliacion bancaria (REQ-0085).
    private List<PortalTransferenciaService.Mov> candidatos = List.of();
    private PortalTransferenciaService.Mov nuevoMov = new PortalTransferenciaService.Mov();
    private transient org.primefaces.model.file.UploadedFile csvMovimientos;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        emisores = catalogoService.opciones("EMISORES");
        cargar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void cargar() {
        lista = servicio.bandeja(filtroEstado);
        pagosQr = servicio.intentosQrConciliados();   // REQ-0094
    }

    public List<Object[]> getPagosQr() { return pagosQr; }

    public void seleccionar(PortalTransferenciaService.Fila f) {
        seleccionada = f;
        motivo = null;
        documentoSel = null;
        emisorSel = null;
        documentos = f != null && f.getPersona() != null ? cajaService.documentosPendientesDe(f.getPersona()) : List.of();
        candidatos = f != null ? servicio.candidatos(f.getId()) : List.of();
    }

    /** REQ-0085: concilia con un movimiento bancario confirmado y aplica el pago. */
    public void conciliar(PortalTransferenciaService.Mov m) {
        try {
            if (contexto.getEmpresa() == null || contexto.sucursal() == null)
                throw new NegocioException("Seleccione una empresa/sucursal");
            var planilla = cajaService.planillaAbierta(contexto.getEmpresa().getId(), contexto.sucursal().getId());
            if (planilla == null) throw new NegocioException("Abra la caja para aplicar el cobro");
            servicio.conciliarYAplicar(seleccionada.getId(), m.getId(), documentoSel, planilla.getId(),
                    emisorSel, seleccionada.getMoneda());
            aviso(FacesMessage.SEVERITY_INFO, "Conciliada y aplicada", "Se cruzo con el movimiento bancario y se aplico el cobro");
            cargar();
        } catch (NegocioException e) { aviso(FacesMessage.SEVERITY_WARN, "No se pudo conciliar", e.getMessage()); }
    }

    /** REQ-0085: registra un movimiento bancario manual. */
    public void registrarMovimiento() {
        try {
            servicio.registrarMovimiento(nuevoMov);
            aviso(FacesMessage.SEVERITY_INFO, "Movimiento registrado", null);
            nuevoMov = new PortalTransferenciaService.Mov();
        } catch (NegocioException e) { aviso(FacesMessage.SEVERITY_WARN, "No se pudo registrar", e.getMessage()); }
    }

    /** REQ-0085: importa movimientos desde un CSV (banco;cuenta;fecha;importe;referencia;remitente). */
    public void importarCsv() {
        try {
            if (csvMovimientos == null || csvMovimientos.getContent() == null) throw new NegocioException("Elija un archivo CSV");
            int n = servicio.importarCsv(csvMovimientos.getContent());
            aviso(FacesMessage.SEVERITY_INFO, "Importacion", n + " movimiento(s) importado(s)");
            csvMovimientos = null;
        } catch (NegocioException e) { aviso(FacesMessage.SEVERITY_WARN, "No se pudo importar", e.getMessage()); }
    }

    public void observar() {
        try {
            servicio.observar(seleccionada.getId(), motivo);
            aviso(FacesMessage.SEVERITY_INFO, "Transferencia observada", null);
            cargar();
        } catch (NegocioException e) { aviso(FacesMessage.SEVERITY_WARN, "No se pudo observar", e.getMessage()); }
    }

    public void rechazar() {
        try {
            servicio.rechazar(seleccionada.getId(), motivo);
            aviso(FacesMessage.SEVERITY_INFO, "Transferencia rechazada", null);
            cargar();
        } catch (NegocioException e) { aviso(FacesMessage.SEVERITY_WARN, "No se pudo rechazar", e.getMessage()); }
    }

    public void aprobar() {
        try {
            if (contexto.getEmpresa() == null || contexto.sucursal() == null)
                throw new NegocioException("Seleccione una empresa/sucursal");
            var planilla = cajaService.planillaAbierta(contexto.getEmpresa().getId(), contexto.sucursal().getId());
            if (planilla == null) throw new NegocioException("Abra la caja para aplicar el cobro");
            servicio.aprobar(seleccionada.getId(), documentoSel, planilla.getId(), null, emisorSel, seleccionada.getMoneda());
            aviso(FacesMessage.SEVERITY_INFO, "Transferencia aplicada", "Se genero el cobro y se imputo el documento");
            cargar();
        } catch (NegocioException e) { aviso(FacesMessage.SEVERITY_WARN, "No se pudo aplicar", e.getMessage()); }
    }

    public StreamedContent descargar(PortalTransferenciaService.Fila f) {
        final Long id = f.getId();
        return DefaultStreamedContent.builder()
                .name("comprobante")
                .contentType("application/octet-stream")
                .stream(() -> {
                    PortalTransferenciaService.Descarga d = servicio.descargar(id, null);
                    return new ByteArrayInputStream(d.datos);
                })
                .build();
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String v) { this.filtroEstado = v; }
    public List<PortalTransferenciaService.Fila> getLista() { return lista; }
    public PortalTransferenciaService.Fila getSeleccionada() { return seleccionada; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public List<Object[]> getDocumentos() { return documentos; }
    public Long getDocumentoSel() { return documentoSel; }
    public void setDocumentoSel(Long v) { this.documentoSel = v; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> getEmisores() { return emisores; }
    public String getEmisorSel() { return emisorSel; }
    public void setEmisorSel(String v) { this.emisorSel = v; }
    public List<String> getEstados() { return List.of("RECIBIDO", "EN_REVISION", "OBSERVADO", "RECHAZADO", "APLICADO"); }
    public List<PortalTransferenciaService.Mov> getCandidatos() { return candidatos; }
    public PortalTransferenciaService.Mov getNuevoMov() { return nuevoMov; }
    public org.primefaces.model.file.UploadedFile getCsvMovimientos() { return csvMovimientos; }
    public void setCsvMovimientos(org.primefaces.model.file.UploadedFile v) { this.csvMovimientos = v; }
}
