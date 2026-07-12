package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.RolPlantillaService;

import java.io.Serializable;
import java.util.List;

/** REQ-0063 - Aplicar plantillas de roles/permisos a un grupo con diff previo. */
@Named
@ViewScoped
public class RolPlantillaBean implements Serializable {

    public static final String PANTALLA = "roles-plantilla";

    @Inject
    private transient RolPlantillaService servicio;
    @Inject
    private SesionUsuario sesion;

    private List<RolPlantillaService.Fila> plantillas;
    private List<RolPlantillaService.Fila> grupos;
    private Long plantillaId;
    private Long grupoId;
    private boolean reemplazar;
    private RolPlantillaService.Diff diff;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        plantillas = servicio.plantillas();
        grupos = servicio.grupos();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void verDiff() {
        diff = null;
        try {
            if (plantillaId == null || grupoId == null) {
                aviso(FacesMessage.SEVERITY_WARN, "Faltan datos", "Elija plantilla y grupo");
                return;
            }
            diff = servicio.diff(plantillaId, grupoId);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo comparar", e.getMessage());
        }
    }

    public void aplicar() {
        try {
            servicio.aplicar(plantillaId, grupoId, reemplazar);
            aviso(FacesMessage.SEVERITY_INFO, "Plantilla aplicada",
                    reemplazar ? "El grupo quedo igual a la plantilla" : "Permisos agregados al grupo");
            diff = servicio.diff(plantillaId, grupoId);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo aplicar", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity sev, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, t, d));
    }

    public List<RolPlantillaService.Fila> getPlantillas() { return plantillas; }
    public List<RolPlantillaService.Fila> getGrupos() { return grupos; }
    public Long getPlantillaId() { return plantillaId; }
    public void setPlantillaId(Long v) { this.plantillaId = v; }
    public Long getGrupoId() { return grupoId; }
    public void setGrupoId(Long v) { this.grupoId = v; }
    public boolean isReemplazar() { return reemplazar; }
    public void setReemplazar(boolean v) { this.reemplazar = v; }
    public RolPlantillaService.Diff getDiff() { return diff; }
}
