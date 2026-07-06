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
import py.com.pysistemas.sginmo.dominio.catalogo.ParametroSistema;
import py.com.pysistemas.sginmo.servicio.ParametroService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de parametros de configuracion (clave inmutable; valores sensibles enmascarados en grilla). */
@Named
@ViewScoped
public class ParametroBean implements Serializable {

    public static final String PANTALLA = "parametros";

    @Inject
    private transient ParametroService parametroService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<ParametroSistema> modelo;
    private ParametroSistema seleccionado;
    private String filtroGlobal = "";
    private boolean soloLectura;
    private boolean esNuevo;

    @PostConstruct
    public void iniciar() {
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) parametroService.contar(filtroGlobal);
            }

            @Override
            public List<ParametroSistema> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return parametroService.listar(first, pageSize, filtroGlobal);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new ParametroSistema();
        esNuevo = true;
        soloLectura = false;
    }

    public void editar(ParametroSistema parametro) {
        seleccionado = parametro;
        esNuevo = false;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    public void guardar() {
        try {
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) return;
            parametroService.guardar(seleccionado, esNuevo);
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Parámetro creado" : "Parámetro actualizado", seleccionado.getClave());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgParametro').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<ParametroSistema> getModelo() { return modelo; }
    public ParametroSistema getSeleccionado() { return seleccionado; }
    public void setSeleccionado(ParametroSistema seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public boolean isEsNuevo() { return esNuevo; }
}
