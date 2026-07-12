package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.dominio.documento.DocumentoGenerado;
import py.com.pysistemas.sginmo.servicio.DocumentoGeneradoService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** REQ-0054 - Estado documental de contratos/pagares generados: firma, anulacion, seguimiento. */
@Named
@ViewScoped
public class DocumentoGeneradoBean implements Serializable {

    public static final String PANTALLA = "documentos-generados";

    @Inject
    private transient DocumentoGeneradoService servicio;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<DocumentoGenerado> modelo;
    private DocumentoGenerado seleccionado;
    private String filtroEstado;
    private boolean soloPendientesFirma;
    private String motivoAnulacion;
    private Long adjuntoFirmadoId;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) servicio.contar(filtroEstado, soloPendientesFirma);
            }
            @Override
            public List<DocumentoGenerado> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return servicio.listar(first, pageSize, filtroEstado, soloPendientesFirma);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void gestionar(DocumentoGenerado d) {
        seleccionado = d;
        motivoAnulacion = null;
        adjuntoFirmadoId = d.getAdjuntoFirmado();
    }

    public void cambiarEstado(String nuevoEstado) {
        try {
            servicio.cambiarEstado(seleccionado.getId(), nuevoEstado);
            aviso(FacesMessage.SEVERITY_INFO, "Estado actualizado", nuevoEstado);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    public void registrarFirma() {
        try {
            servicio.registrarFirma(seleccionado.getId(), adjuntoFirmadoId);
            aviso(FacesMessage.SEVERITY_INFO, "Firma registrada", "Documento marcado como FIRMADO");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo registrar la firma", e.getMessage());
        }
    }

    public void anular() {
        try {
            servicio.anular(seleccionado.getId(), motivoAnulacion);
            aviso(FacesMessage.SEVERITY_INFO, "Documento anulado", "");
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgGestion').hide()");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo anular", e.getMessage());
        }
    }

    public List<String> getEstados() {
        return List.of("GENERADO", "IMPRESO", "ENVIADO", "FIRMADO", "OBSERVADO", "ANULADO", "ARCHIVADO");
    }

    private void aviso(FacesMessage.Severity sev, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, titulo, detalle));
    }

    public LazyDataModel<DocumentoGenerado> getModelo() { return modelo; }
    public DocumentoGenerado getSeleccionado() { return seleccionado; }
    public void setSeleccionado(DocumentoGenerado s) { this.seleccionado = s; }
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String v) { this.filtroEstado = v; }
    public boolean isSoloPendientesFirma() { return soloPendientesFirma; }
    public void setSoloPendientesFirma(boolean v) { this.soloPendientesFirma = v; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String v) { this.motivoAnulacion = v; }
    public Long getAdjuntoFirmadoId() { return adjuntoFirmadoId; }
    public void setAdjuntoFirmadoId(Long v) { this.adjuntoFirmadoId = v; }
}
