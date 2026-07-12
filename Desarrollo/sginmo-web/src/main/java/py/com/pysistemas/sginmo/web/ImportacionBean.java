package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.ImportacionService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;

/** REQ-0061 - Importacion asistida CSV: plantilla, vista previa, confirmacion atomica e historial. */
@Named
@ViewScoped
public class ImportacionBean implements Serializable {

    public static final String PANTALLA = "importacion";

    @Inject
    private transient ImportacionService servicio;
    @Inject
    private SesionUsuario sesion;

    private String tipo = "PARAMETRO";
    private transient UploadedFile archivo;
    private byte[] csv;
    private String nombreArchivo;
    private List<ImportacionService.FilaPreview> preview;
    private ImportacionService.Resultado resultado;
    private List<Object[]> historial;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        historial = servicio.historial();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void previsualizar() {
        resultado = null;
        try {
            if (archivo == null || archivo.getContent() == null || archivo.getContent().length == 0) {
                aviso(FacesMessage.SEVERITY_WARN, "Falta el archivo", "Seleccione un CSV");
                return;
            }
            csv = archivo.getContent();
            nombreArchivo = archivo.getFileName();
            preview = servicio.preview(tipo, csv);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo leer el CSV", e.getMessage());
        }
    }

    public void confirmar() {
        try {
            if (csv == null) { aviso(FacesMessage.SEVERITY_WARN, "Nada para importar", "Previsualice primero"); return; }
            resultado = servicio.importar(tipo, nombreArchivo, csv);
            historial = servicio.historial();
            if (resultado.isOk()) {
                aviso(FacesMessage.SEVERITY_INFO, "Importacion OK", resultado.getFilasValidas() + " filas");
                preview = null; csv = null;
            } else {
                aviso(FacesMessage.SEVERITY_WARN, "Importacion no realizada (atomica)",
                        resultado.getFilasError() + " fila(s) con error; no se inserto nada");
            }
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo importar", e.getMessage());
        }
    }

    public StreamedContent getPlantilla() {
        try {
            byte[] p = servicio.plantilla(tipo);
            return DefaultStreamedContent.builder()
                    .name("plantilla_" + tipo.toLowerCase() + ".csv")
                    .contentType("text/csv")
                    .stream(() -> new ByteArrayInputStream(p))
                    .build();
        } catch (NegocioException e) {
            return null;
        }
    }

    public List<String> getTipos() { return List.of("PARAMETRO"); }

    private void aviso(FacesMessage.Severity sev, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, t, d));
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public UploadedFile getArchivo() { return archivo; }
    public void setArchivo(UploadedFile archivo) { this.archivo = archivo; }
    public List<ImportacionService.FilaPreview> getPreview() { return preview; }
    public ImportacionService.Resultado getResultado() { return resultado; }
    public List<Object[]> getHistorial() { return historial; }
}
