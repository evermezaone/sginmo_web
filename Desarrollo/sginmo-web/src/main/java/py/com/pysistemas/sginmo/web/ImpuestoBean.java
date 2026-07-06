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
import py.com.pysistemas.sginmo.dominio.catalogo.Impuesto;
import py.com.pysistemas.sginmo.servicio.ImpuestoService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de impuestos: modo simplificado (solo descripcion+porcentaje) o avanzado (factores). */
@Named
@ViewScoped
public class ImpuestoBean implements Serializable {

    public static final String PANTALLA = "impuestos";

    @Inject
    private transient ImpuestoService impuestoService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Impuesto> modelo;
    private Impuesto seleccionado;
    private String filtroGlobal = "";
    private boolean soloLectura;

    /** true si la ultima consulta tenia filtro global (mensaje de grilla vacia diferenciado). */
    private boolean consultaFiltrada;

    /** Mi vista reutilizable del modulo (obs 206 de Codex). */
    @jakarta.inject.Inject
    private transient py.com.one.security.servicio.VistaUsuario vista;
    private boolean modoAvanzado;

    @PostConstruct
    public void iniciar() {
        modoAvanzado = impuestoService.modoAvanzado();
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) impuestoService.contar(filtroGlobal);
            }

            @Override
            public List<Impuesto> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                consultaFiltrada = filtroGlobal != null && !filtroGlobal.isBlank();
                String orden = null; boolean asc = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    orden = sm.getField(); asc = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return impuestoService.listar(first, pageSize, filtroGlobal, orden, asc);
            }
        };
    }

    public String verificarAcceso() {
        if (!sesion.puede(PANTALLA, "VER")) {
            return "/index?faces-redirect=true";
        }
        String filtroVista = vista.aplicar(PANTALLA);
        if (filtroVista != null) {
            filtroGlobal = filtroVista;
        }
        return null;
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Impuesto();
        soloLectura = false;
    }

    public void editar(Impuesto impuesto) {
        seleccionado = impuesto;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) return;
            impuestoService.guardar(seleccionado, modoAvanzado);
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Impuesto creado" : "Impuesto actualizado", seleccionado.getDescripcion());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgImpuesto').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(Impuesto impuesto) {
        try {
            String nuevo = "ACTIVO".equals(impuesto.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            impuestoService.cambiarEstado(impuesto.getId(), nuevo);
            aviso(FacesMessage.SEVERITY_INFO, "ACTIVO".equals(nuevo) ? "Impuesto activado" : "Impuesto inactivado", impuesto.getDescripcion());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    public void guardarMiVista() {
        if (vista.guardar(PANTALLA, filtroGlobal)) {
            aviso(FacesMessage.SEVERITY_INFO, "Mi vista guardada", "Se aplicará automáticamente al entrar a esta pantalla");
        }
    }

    public void quitarMiVista() {
        vista.quitar(PANTALLA);
        aviso(FacesMessage.SEVERITY_INFO, "Mi vista eliminada", "La pantalla vuelve a la configuración estándar");
    }

    public void limpiarBusqueda() {
        filtroGlobal = "";
    }

    public boolean isConsultaFiltrada() { return consultaFiltrada; }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<Impuesto> getModelo() { return modelo; }
    public Impuesto getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Impuesto seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public boolean isModoAvanzado() { return modoAvanzado; }
}
