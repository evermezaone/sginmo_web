package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.dominio.documento.DocumentoAdjunto;
import py.com.pysistemas.sginmo.servicio.DocumentoAdjuntoService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** REQ-0053 - Gestion de adjuntos documentales: alta (upload), listado, descarga y baja logica. */
@Named
@ViewScoped
public class DocumentoBean implements Serializable {

    public static final String PANTALLA = "documentos";

    @Inject
    private transient DocumentoAdjuntoService servicio;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<DocumentoAdjunto> modelo;
    private DocumentoAdjunto seleccionado;
    private transient UploadedFile archivo;
    private String filtroEntidadTipo;
    private String filtroTipo;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) servicio.contar(filtroEntidadTipo, filtroTipo, false);
            }
            @Override
            public List<DocumentoAdjunto> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return servicio.listar(first, pageSize, filtroEntidadTipo, filtroTipo, false);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new DocumentoAdjunto();
        seleccionado.setEntidadTipo("GENERAL");
        seleccionado.setTipo("OTRO");
        archivo = null;
    }

    public void guardar() {
        try {
            if (archivo == null || archivo.getContent() == null || archivo.getContent().length == 0) {
                aviso(FacesMessage.SEVERITY_WARN, "Falta el archivo", "Seleccione un archivo para adjuntar");
                return;
            }
            servicio.guardar(seleccionado, archivo.getContent(), archivo.getFileName(), archivo.getContentType());
            aviso(FacesMessage.SEVERITY_INFO, "Documento adjuntado", archivo.getFileName());
            archivo = null;
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo adjuntar", e.getMessage());
        }
    }

    /** Descarga protegida (permiso + tenant). Stream perezoso: solo lee el archivo al descargar. */
    public StreamedContent descargar(DocumentoAdjunto d) {
        final Long id = d.getId();
        return DefaultStreamedContent.builder()
                .name(d.getNombreOriginal())
                .contentType(d.getContentType() != null ? d.getContentType() : "application/octet-stream")
                .stream(() -> new ByteArrayInputStream(servicio.leer(id)))
                .build();
    }

    public void baja(DocumentoAdjunto d) {
        try {
            servicio.baja(d.getId());
            aviso(FacesMessage.SEVERITY_INFO, "Documento dado de baja", d.getNombreOriginal());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo dar de baja", e.getMessage());
        }
    }

    public List<String> getTipos() {
        return List.of("CEDULA", "RUC", "CONTRATO", "PAGARE", "COMPROBANTE", "TITULO", "PLANO", "FOTO", "OTRO");
    }
    public List<String> getEntidadTipos() {
        return List.of("PERSONA", "ACTIVO", "OPERACION", "COBRO", "LIQUIDACION", "PLANTILLA", "GENERAL");
    }

    private void aviso(FacesMessage.Severity sev, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, titulo, detalle));
    }

    public LazyDataModel<DocumentoAdjunto> getModelo() { return modelo; }
    public DocumentoAdjunto getSeleccionado() { return seleccionado; }
    public void setSeleccionado(DocumentoAdjunto s) { this.seleccionado = s; }
    public UploadedFile getArchivo() { return archivo; }
    public void setArchivo(UploadedFile archivo) { this.archivo = archivo; }
    public String getFiltroEntidadTipo() { return filtroEntidadTipo; }
    public void setFiltroEntidadTipo(String v) { this.filtroEntidadTipo = v; }
    public String getFiltroTipo() { return filtroTipo; }
    public void setFiltroTipo(String v) { this.filtroTipo = v; }
}
