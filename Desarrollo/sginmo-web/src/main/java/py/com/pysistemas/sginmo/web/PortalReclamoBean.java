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
import py.com.pysistemas.sginmo.servicio.ReclamoService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * REQ-0110 - Portal: el cliente abre reclamos sobre su unidad y adjunta fotos, y ve el estado
 * y la respuesta. La identidad viene de la sesion de portal (persona + tenant).
 */
@Named
@ViewScoped
public class PortalReclamoBean implements Serializable {

    @Inject
    private transient ReclamoService servicio;
    @Inject
    private PortalSesion sesion;

    private String tipo = "MANTENIMIENTO";
    private String titulo;
    private String descripcion;
    private String unidad;                 // fallback (solo si el cliente no tuviera unidades)
    // Unidades del cliente (por su identidad): si es una sola se asume; si son varias, se elige.
    private List<ReclamoService.Unidad> unidades = List.of();
    private Long operacionSel;
    // fotos adjuntadas por ajax (advanced/auto), retenidas hasta el envio.
    private transient List<ReclamoService.Adjunto> fotos = new ArrayList<>();
    private List<String> fotosNombres = new ArrayList<>();

    private List<ReclamoService.Fila> mios = List.of();

    // foto seleccionada para ver en el modal (data URI base64).
    private transient String fotoDataUri;
    private String fotoNombre;

    @PostConstruct
    public void iniciar() {
        if (sesion.isAutenticado()) recargar();
    }

    public String verificarAcceso() {
        return sesion.isAutenticado() ? null : "/portal/login?faces-redirect=true";
    }

    private void recargar() {
        mios = servicio.mios(sesion.getPersona());
        unidades = servicio.unidadesDe(sesion.getPersona());
        if (unidades.size() == 1) operacionSel = unidades.get(0).getOperacion();
    }

    private String labelUnidad(Long operacion) {
        for (ReclamoService.Unidad u : unidades) if (u.getOperacion().equals(operacion)) return u.getNombre();
        return null;
    }

    /** REQ-0110: cada foto se sube apenas se adjunta (advanced/auto) y se retiene en bytes. */
    public void subirFoto(FileUploadEvent event) {
        UploadedFile f = event.getFile();
        if (f == null || f.getContent() == null || f.getContent().length == 0) return;
        fotos.add(new ReclamoService.Adjunto(f.getContent(), f.getFileName(), f.getContentType()));
        fotosNombres.add(f.getFileName());
        aviso(FacesMessage.SEVERITY_INFO, "Foto adjuntada", f.getFileName());
    }

    public void quitarFotos() {
        fotos.clear(); fotosNombres.clear();
    }

    public void crear() {
        try {
            ReclamoService.Datos d = new ReclamoService.Datos();
            d.tipo = tipo; d.titulo = titulo; d.descripcion = descripcion;
            if (!unidades.isEmpty()) {
                if (operacionSel == null) { aviso(FacesMessage.SEVERITY_WARN, "Falta la unidad", "Elegí a qué unidad corresponde el reclamo."); return; }
                d.operacion = operacionSel; d.unidad = labelUnidad(operacionSel);
            } else {
                d.unidad = unidad;
            }
            servicio.crear(sesion.getPersona(), d, fotos);
            aviso(FacesMessage.SEVERITY_INFO, "Reclamo enviado",
                    "Lo revisaremos y te responderemos por este mismo portal.");
            tipo = "MANTENIMIENTO"; titulo = null; descripcion = null; unidad = null; operacionSel = null;
            fotos = new ArrayList<>(); fotosNombres = new ArrayList<>();
            recargar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo enviar", e.getMessage());
        }
    }

    /** Carga la foto seleccionada (bytes reales) como data URI para mostrarla en el modal. */
    public void verFoto(ReclamoService.Foto f) {
        try {
            ReclamoService.Descarga d = servicio.descargarFoto(f.getId(), sesion.getPersona());
            String m = d.contentType == null || d.contentType.isBlank() ? "application/octet-stream" : d.contentType;
            fotoDataUri = "data:" + m + ";base64," + Base64.getEncoder().encodeToString(d.datos);
            fotoNombre = d.nombre;
        } catch (RuntimeException e) {
            fotoDataUri = null; fotoNombre = null;
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo abrir la foto", e.getMessage());
        }
    }

    public boolean isFotoEsImagen() { return fotoDataUri != null && fotoDataUri.startsWith("data:image/"); }
    public boolean isFotoDisponible() { return fotoDataUri != null; }
    public String getFotoDataUri() { return fotoDataUri; }
    public String getFotoNombre() { return fotoNombre; }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public List<ReclamoService.Unidad> getUnidades() { return unidades; }
    public Long getOperacionSel() { return operacionSel; }
    public void setOperacionSel(Long v) { this.operacionSel = v; }
    public boolean isUnaUnidad() { return unidades.size() == 1; }
    public boolean isVariasUnidades() { return unidades.size() > 1; }
    public boolean isSinUnidades() { return unidades.isEmpty(); }
    public ReclamoService.Unidad getUnicaUnidad() { return unidades.isEmpty() ? null : unidades.get(0); }
    public List<String> getFotosNombres() { return fotosNombres; }
    public int getCantidadFotos() { return fotosNombres.size(); }
    public boolean isHayFotos() { return !fotosNombres.isEmpty(); }
    public List<ReclamoService.Fila> getMios() { return mios; }
    public String getNombre() { return sesion.getNombre(); }
}
