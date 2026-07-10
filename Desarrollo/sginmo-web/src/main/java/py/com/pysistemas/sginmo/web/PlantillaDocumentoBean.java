package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.documento.PlantillaDocumento;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.PlantillaDocumentoMotor;
import py.com.pysistemas.sginmo.servicio.PlantillaDocumentoService;

import java.io.Serializable;
import java.util.List;

/** ABM de plantillas documentales para contratos y pagares. */
@Named
@ViewScoped
public class PlantillaDocumentoBean implements Serializable {

    public static final String PANTALLA = "plantillas-documentos";

    @Inject
    private transient PlantillaDocumentoService service;

    @Inject
    private transient CatalogoService catalogoService;

    @Inject
    private SesionUsuario sesion;

    private List<PlantillaDocumento> plantillas;
    private List<Entidad> tiposContrato;
    private List<PlantillaDocumentoMotor.Variable> variables;
    private PlantillaDocumento seleccionado;
    private String filtroGlobal = "";

    @PostConstruct
    public void iniciar() {
        tiposContrato = catalogoService.opciones("TIPOS_CONTRATOS");
        variables = service.variablesDisponibles();
        plantillas = java.util.List.of();
        if (sesion.puede(PANTALLA, "VER")) {
            buscar();
        }
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void buscar() {
        if (!sesion.puede(PANTALLA, "VER")) {
            plantillas = java.util.List.of();
            return;
        }
        plantillas = service.listar(filtroGlobal);
    }

    public void nuevoContrato() {
        nuevo("CONTRATO");
    }

    public void nuevoPagare() {
        nuevo("PAGARE");
    }

    private void nuevo(String tipo) {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = service.nueva(tipo);
    }

    public void editar(PlantillaDocumento plantilla) {
        seleccionado = plantilla;
    }

    public void guardar() {
        try {
            boolean nuevo = seleccionado.getId() == null;
            service.guardar(seleccionado);
            buscar();
            aviso(FacesMessage.SEVERITY_INFO, nuevo ? "Plantilla creada" : "Plantilla actualizada",
                    seleccionado.getDescripcion());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgPlantilla').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(PlantillaDocumento plantilla) {
        try {
            String nuevo = "ACTIVO".equals(plantilla.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            service.cambiarEstado(plantilla.getId(), nuevo);
            buscar();
            aviso(FacesMessage.SEVERITY_INFO,
                    "ACTIVO".equals(nuevo) ? "Plantilla activada" : "Plantilla inactivada",
                    plantilla.getDescripcion());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    public String alcance(PlantillaDocumento plantilla) {
        if (plantilla == null || plantilla.getTenant() == null) return "";
        return plantilla.getTenant() == -1L ? "Global" : "Empresa";
    }

    private void aviso(FacesMessage.Severity severidad, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, titulo, detalle));
    }

    public List<PlantillaDocumento> getPlantillas() { return plantillas; }
    public List<Entidad> getTiposContrato() { return tiposContrato; }
    public List<PlantillaDocumentoMotor.Variable> getVariables() { return variables; }
    public PlantillaDocumento getSeleccionado() { return seleccionado; }
    public void setSeleccionado(PlantillaDocumento seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
}
