package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.AuditoriaFuncionalService;

import java.io.Serializable;
import java.util.List;

/**
 * REQ-0067 - Auditoria funcional visible. Consulta filtrable del historial de cambios de registros
 * sensibles (permiso auditoria/VER). La RLS ya limita a la empresa activa.
 */
@Named
@ViewScoped
public class AuditoriaBean implements Serializable {

    public static final String PANTALLA = "auditoria";

    /** Acciones disponibles para el combo de filtro (coinciden con el CHECK de V46). */
    public static final List<String> ACCIONES = List.of(
            "", "CREAR", "EDITAR", "INACTIVAR", "REACTIVAR", "ANULAR", "COBRAR",
            "DESCUENTO", "LIQUIDAR", "REGENERAR", "DESBLOQUEAR", "OTRO");

    @Inject
    private transient AuditoriaFuncionalService servicio;
    @Inject
    private SesionUsuario sesion;

    private String fechaDesde;
    private String fechaHasta;
    private String usuario;
    private String accion;
    private String campo;
    private String entidad;

    private List<AuditoriaFuncionalService.Fila> resultados;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        buscar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void buscar() {
        try {
            resultados = servicio.consultar(fechaDesde, fechaHasta, usuario, accion, campo, entidad);
        } catch (RuntimeException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo consultar la auditoria", e.getMessage());
        }
    }

    public void limpiar() {
        fechaDesde = fechaHasta = usuario = accion = campo = entidad = null;
        buscar();
    }

    private void aviso(FacesMessage.Severity sev, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, t, d));
    }

    public List<String> getAcciones() { return ACCIONES; }
    public List<AuditoriaFuncionalService.Fila> getResultados() { return resultados; }

    public String getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }
    public String getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getCampo() { return campo; }
    public void setCampo(String campo) { this.campo = campo; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
}
