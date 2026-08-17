package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.ReclamoService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * REQ-0111 - Bandeja interna de reclamos: recibir, priorizar, asignar, calendarizar, proveedor
 * (con cotizacion -> orden de pago), verificar con evidencia de fotos y cerrar. + dashboard.
 */
@Named
@ViewScoped
public class ReclamoAdminBean implements Serializable {

    @Inject
    private transient ReclamoService servicio;
    @Inject
    private SesionUsuario sesion;

    public static final String PANTALLA = ReclamoService.PANTALLA;

    private String filtroEstado;
    private List<ReclamoService.Fila> lista = List.of();
    private ReclamoService.Dash dash = new ReclamoService.Dash();
    private ReclamoService.Fila sel;
    private List<ReclamoService.Seg> seguimiento = List.of();

    // entradas del detalle
    private String prioridadInput = "MEDIA";
    private String asignadoInput;
    private LocalDate fechaProgramar;
    private String proveedorInput;
    private BigDecimal cotizacionInput;
    private String respuestaInput;
    private String motivoInput;
    private String notaInput;

    // evidencia (fotos de la solucion) pendientes de guardar
    private transient List<ReclamoService.Adjunto> evidencias = new ArrayList<>();
    private List<String> evidenciasNombres = new ArrayList<>();

    // foto seleccionada para el modal
    private transient String fotoDataUri;

    @PostConstruct
    public void iniciar() {
        if (sesion.puede(PANTALLA, "VER")) cargar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void cargar() {
        lista = servicio.bandeja(filtroEstado);
        dash = servicio.dashboard();
    }

    public void abrir(ReclamoService.Fila f) {
        sel = f;
        prioridadInput = f.getPrioridad() == null ? "MEDIA" : f.getPrioridad();
        asignadoInput = f.getAsignado();
        fechaProgramar = f.getFechaProgramada();
        proveedorInput = f.getProveedorNombre();
        cotizacionInput = f.getCotizacion();
        respuestaInput = f.getRespuesta();
        motivoInput = null; notaInput = null;
        evidencias = new ArrayList<>(); evidenciasNombres = new ArrayList<>();
        seguimiento = servicio.seguimientoDe(f.getId());
    }

    private void refrescar() {
        Long id = sel == null ? null : sel.getId();
        cargar();
        if (id != null) {
            for (ReclamoService.Fila f : lista) if (f.getId().equals(id)) { sel = f; break; }
            seguimiento = servicio.seguimientoDe(id);
        }
    }

    // ── acciones del workflow ──
    public void priorizar() { run(() -> servicio.priorizar(sel.getId(), prioridadInput), "Prioridad actualizada"); }
    public void asignar()   { run(() -> servicio.asignar(sel.getId(), asignadoInput), "Reclamo asignado"); }
    public void programar() { run(() -> servicio.programar(sel.getId(), fechaProgramar), "Visita programada"); }
    public void guardarProveedor() { run(() -> servicio.asignarProveedor(sel.getId(), proveedorInput, null, cotizacionInput, null), "Proveedor y cotización guardados"); }
    public void generarOrden() { run(() -> servicio.generarOrdenPago(sel.getId()), "Orden de pago generada"); }
    public void resolver()  { run(() -> servicio.resolver(sel.getId(), respuestaInput), "Reclamo resuelto"); }
    public void cerrar()    { run(() -> servicio.cerrar(sel.getId()), "Reclamo cerrado"); }
    public void rechazar()  { run(() -> servicio.rechazar(sel.getId(), motivoInput), "Reclamo rechazado"); }
    public void agregarNota() { run(() -> { servicio.nota(sel.getId(), notaInput); notaInput = null; }, "Nota agregada"); }

    public void subirEvidencia(FileUploadEvent event) {
        UploadedFile f = event.getFile();
        if (f == null || f.getContent() == null || f.getContent().length == 0) return;
        evidencias.add(new ReclamoService.Adjunto(f.getContent(), f.getFileName(), f.getContentType()));
        evidenciasNombres.add(f.getFileName());
        aviso(FacesMessage.SEVERITY_INFO, "Foto lista", f.getFileName());
    }
    public void guardarEvidencia() {
        run(() -> {
            servicio.agregarEvidencia(sel.getId(), evidencias);
            evidencias = new ArrayList<>(); evidenciasNombres = new ArrayList<>();
        }, "Evidencia guardada");
    }

    private void run(Runnable r, String ok) {
        try { r.run(); refrescar(); aviso(FacesMessage.SEVERITY_INFO, ok, null); }
        catch (NegocioException e) { aviso(FacesMessage.SEVERITY_WARN, "No se pudo completar", e.getMessage()); }
        catch (RuntimeException e) { aviso(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()); }
    }

    public void verFoto(ReclamoService.Foto f) {
        try {
            ReclamoService.Descarga d = servicio.descargarFoto(f.getId(), null);
            String m = d.contentType == null || d.contentType.isBlank() ? "application/octet-stream" : d.contentType;
            fotoDataUri = "data:" + m + ";base64," + Base64.getEncoder().encodeToString(d.datos);
        } catch (RuntimeException e) {
            fotoDataUri = null;
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo abrir la foto", e.getMessage());
        }
    }
    public boolean isFotoEsImagen() { return fotoDataUri != null && fotoDataUri.startsWith("data:image/"); }
    public boolean isFotoDisponible() { return fotoDataUri != null; }
    public String getFotoDataUri() { return fotoDataUri; }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public List<String> getEstados() { return List.of("ABIERTO", "EN_PROCESO", "RESUELTO", "RECHAZADO", "CERRADO"); }

    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String v) { this.filtroEstado = v; }
    public List<ReclamoService.Fila> getLista() { return lista; }
    public ReclamoService.Dash getDash() { return dash; }
    public ReclamoService.Fila getSel() { return sel; }
    public boolean isHaySel() { return sel != null; }
    public List<ReclamoService.Seg> getSeguimiento() { return seguimiento; }
    public String getPrioridadInput() { return prioridadInput; }
    public void setPrioridadInput(String v) { this.prioridadInput = v; }
    public String getAsignadoInput() { return asignadoInput; }
    public void setAsignadoInput(String v) { this.asignadoInput = v; }
    public LocalDate getFechaProgramar() { return fechaProgramar; }
    public void setFechaProgramar(LocalDate v) { this.fechaProgramar = v; }
    public String getProveedorInput() { return proveedorInput; }
    public void setProveedorInput(String v) { this.proveedorInput = v; }
    public BigDecimal getCotizacionInput() { return cotizacionInput; }
    public void setCotizacionInput(BigDecimal v) { this.cotizacionInput = v; }
    public String getRespuestaInput() { return respuestaInput; }
    public void setRespuestaInput(String v) { this.respuestaInput = v; }
    public String getMotivoInput() { return motivoInput; }
    public void setMotivoInput(String v) { this.motivoInput = v; }
    public String getNotaInput() { return notaInput; }
    public void setNotaInput(String v) { this.notaInput = v; }
    public List<String> getEvidenciasNombres() { return evidenciasNombres; }
    public boolean isHayEvidencia() { return !evidenciasNombres.isEmpty(); }
}
