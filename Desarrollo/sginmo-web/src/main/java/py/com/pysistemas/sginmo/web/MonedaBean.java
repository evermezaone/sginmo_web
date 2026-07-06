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
import py.com.pysistemas.sginmo.dominio.catalogo.Moneda;
import py.com.pysistemas.sginmo.servicio.MonedaService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de monedas (patron estandar). */
@Named
@ViewScoped
public class MonedaBean implements Serializable {

    public static final String PANTALLA = "monedas";

    @Inject
    private transient MonedaService monedaService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Moneda> modelo;
    private Moneda seleccionado;
    private String filtroGlobal = "";
    private boolean soloLectura;

    @PostConstruct
    public void iniciar() {
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) monedaService.contar(filtroGlobal);
            }

            @Override
            public List<Moneda> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                String orden = null; boolean asc = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    orden = sm.getField(); asc = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return monedaService.listar(first, pageSize, filtroGlobal, orden, asc);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Moneda();
        soloLectura = false;
    }

    public void editar(Moneda moneda) {
        seleccionado = moneda;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) return;
            monedaService.guardar(seleccionado);
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Moneda creada" : "Moneda actualizada", seleccionado.getDescripcion());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgMoneda').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(Moneda moneda) {
        try {
            String nuevo = "ACTIVO".equals(moneda.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            monedaService.cambiarEstado(moneda.getId(), nuevo);
            aviso(FacesMessage.SEVERITY_INFO, "ACTIVO".equals(nuevo) ? "Moneda activada" : "Moneda inactivada", moneda.getDescripcion());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<Moneda> getModelo() { return modelo; }
    public Moneda getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Moneda seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
}
