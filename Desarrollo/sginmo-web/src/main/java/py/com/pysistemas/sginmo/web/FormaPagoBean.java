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
import py.com.pysistemas.sginmo.dominio.catalogo.FormaPago;
import py.com.pysistemas.sginmo.servicio.FormaPagoService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de formas de pago (patron estandar, con habilitado y requisitos del cobro). */
@Named
@ViewScoped
public class FormaPagoBean implements Serializable {

    public static final String PANTALLA = "formas-pago";

    @Inject
    private transient FormaPagoService formaPagoService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<FormaPago> modelo;
    private FormaPago seleccionado;
    private String filtroGlobal = "";
    private boolean soloLectura;
    private int tabActivo;

    @PostConstruct
    public void iniciar() {
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) formaPagoService.contar(filtroGlobal);
            }

            @Override
            public List<FormaPago> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                String orden = null; boolean asc = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    orden = sm.getField(); asc = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return formaPagoService.listar(first, pageSize, filtroGlobal, orden, asc);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new FormaPago();
        soloLectura = false;
        tabActivo = 0;
    }

    public void editar(FormaPago fp) {
        seleccionado = fp;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
        tabActivo = 0;
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) return;
            formaPagoService.guardar(seleccionado);
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Forma de pago creada" : "Forma de pago actualizada", seleccionado.getDescripcion());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgFormaPago').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(FormaPago fp) {
        try {
            String nuevo = "ACTIVO".equals(fp.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            formaPagoService.cambiarEstado(fp.getId(), nuevo);
            aviso(FacesMessage.SEVERITY_INFO, "ACTIVO".equals(nuevo) ? "Forma de pago activada" : "Forma de pago inactivada", fp.getDescripcion());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<FormaPago> getModelo() { return modelo; }
    public FormaPago getSeleccionado() { return seleccionado; }
    public void setSeleccionado(FormaPago seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }
}
