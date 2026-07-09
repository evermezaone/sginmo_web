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
import py.com.pysistemas.sginmo.servicio.ListaService;

import java.io.Serializable;
import java.util.List;

/**
 * ABM de las listas configurables del sistema (tabla generica `entidad`):
 * se elige la lista y se administran sus opciones. El codigo es inmutable al editar.
 */
@Named
@ViewScoped
public class ListaBean implements Serializable {

    public static final String PANTALLA = "listas";

    @Inject
    private transient ListaService listaService;

    @Inject
    private SesionUsuario sesion;

    private List<String> listas;
    private String listaSeleccionada;
    private String filtroGlobal = "";
    private List<Entidad> opciones = java.util.List.of();

    private Entidad seleccionado;
    private boolean esNueva;
    private boolean soloLectura;

    @PostConstruct
    public void iniciar() {
        listas = listaService.listas();
        if (!listas.isEmpty()) {
            listaSeleccionada = listas.get(0);
            refrescar();
        }
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void refrescar() {
        opciones = listaSeleccionada == null ? java.util.List.of()
                : listaService.opcionesDe(listaSeleccionada, filtroGlobal);
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Entidad();
        seleccionado.setLista(listaSeleccionada);
        esNueva = true;
        soloLectura = false;
    }

    public void editar(Entidad opcion) {
        seleccionado = opcion;
        esNueva = false;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    public void guardar() {
        try {
            if (soloLectura || !sesion.puede(PANTALLA, esNueva ? "CREAR" : "EDITAR")) return;
            listaService.guardar(seleccionado, esNueva);
            aviso(FacesMessage.SEVERITY_INFO, esNueva ? "Opción creada" : "Opción actualizada",
                    seleccionado.getLista() + " / " + seleccionado.getCodigo());
            listas = listaService.listas();   // por si nacio una lista nueva
            refrescar();
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgLista').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes", "frmLista:cboLista");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(Entidad opcion) {
        try {
            String nuevo = "ACTIVO".equals(opcion.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            listaService.cambiarEstado(opcion.getId(), nuevo);
            refrescar();
            aviso(FacesMessage.SEVERITY_INFO, "ACTIVO".equals(nuevo) ? "Opción activada" : "Opción inactivada (deja de aparecer en los combos)",
                    opcion.getCodigo());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public List<String> getListas() { return listas; }
    public String getListaSeleccionada() { return listaSeleccionada; }
    public void setListaSeleccionada(String listaSeleccionada) { this.listaSeleccionada = listaSeleccionada; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public List<Entidad> getOpciones() { return opciones; }
    public Entidad getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Entidad seleccionado) { this.seleccionado = seleccionado; }
    public boolean isEsNueva() { return esNueva; }
    public boolean isSoloLectura() { return soloLectura; }
}
