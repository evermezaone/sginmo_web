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
import py.com.pysistemas.sginmo.dominio.catalogo.Articulo;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.catalogo.Impuesto;
import py.com.pysistemas.sginmo.servicio.ArticuloService;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.NegocioException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * ABM de articulos — patron propuesto para todos los ABM del sistema:
 * dataTable lazy + dialog de edicion + growl; el bean solo orquesta la UI
 * (las reglas viven en ArticuloService).
 */
@Named
@ViewScoped
public class ArticuloBean implements Serializable {

    @Inject
    private transient ArticuloService articuloService;

    @Inject
    private transient CatalogoService catalogoService;

    private LazyDataModel<Articulo> modelo;
    private Articulo seleccionado;
    private String filtroGlobal = "";

    private List<Entidad> categorias;
    private List<Entidad> unidadesMedida;
    private List<Impuesto> impuestos;

    @PostConstruct
    public void iniciar() {
        categorias = catalogoService.opciones("TIPOS_ARTICULO");
        unidadesMedida = catalogoService.opciones("UNIDADES_MEDIDA");
        impuestos = catalogoService.impuestosActivos();

        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) articuloService.contar(filtroGlobal);
            }

            @Override
            public List<Articulo> load(int first, int pageSize,
                                       Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return articuloService.listar(first, pageSize, filtroGlobal);
            }
        };
    }

    // ── Acciones ──

    public void nuevo() {
        seleccionado = new Articulo();
    }

    public void editar(Articulo articulo) {
        seleccionado = articulo;
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            articuloService.guardar(seleccionado);
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Artículo creado" : "Artículo actualizado",
                    seleccionado.getDescripcion());
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(false);
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgArticulo').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(Articulo articulo) {
        try {
            String nuevo = "ACTIVO".equals(articulo.getEstado()) ? "INACTIVO" : "ACTIVO";
            articuloService.cambiarEstado(articulo.getId(), nuevo);
            aviso(FacesMessage.SEVERITY_INFO,
                    "ACTIVO".equals(nuevo) ? "Artículo activado" : "Artículo inactivado",
                    articulo.getDescripcion());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity severidad, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, titulo, detalle));
    }

    // ── Getters/Setters ──

    public LazyDataModel<Articulo> getModelo() { return modelo; }

    public Articulo getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Articulo seleccionado) { this.seleccionado = seleccionado; }

    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }

    public List<Entidad> getCategorias() { return categorias; }
    public List<Entidad> getUnidadesMedida() { return unidadesMedida; }
    public List<Impuesto> getImpuestos() { return impuestos; }
}
