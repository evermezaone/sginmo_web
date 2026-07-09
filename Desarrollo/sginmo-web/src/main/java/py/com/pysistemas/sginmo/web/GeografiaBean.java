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
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.GeografiaService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de geografia (REQ-0007): 8.276 filas lazy + autocomplete de padre (regla combos grandes). */
@Named
@ViewScoped
public class GeografiaBean implements Serializable {

    public static final String PANTALLA = "geografia";

    @Inject
    private transient GeografiaService geografiaService;

    @Inject
    private transient CatalogoService catalogoService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<UbicacionGeografica> modelo;
    private UbicacionGeografica seleccionado;
    private String filtroGlobal = "";
    private boolean soloLectura;
    private List<Entidad> niveles;

    @PostConstruct
    public void iniciar() {
        niveles = catalogoService.opciones("NIVELES_UBICACION");
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) geografiaService.contar(filtroGlobal);
            }

            @Override
            public List<UbicacionGeografica> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                String orden = null; boolean asc = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    orden = sm.getField(); asc = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return geografiaService.listar(first, pageSize, filtroGlobal, orden, asc);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    /** Autocomplete lazy del padre. */
    public List<UbicacionGeografica> completarUbicacion(String texto) {
        return geografiaService.buscar(texto);
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new UbicacionGeografica();
        soloLectura = false;
    }

    public void editar(UbicacionGeografica u) {
        seleccionado = u;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) return;
            geografiaService.guardar(seleccionado);
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Ubicación creada" : "Ubicación actualizada", seleccionado.getNombre());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgGeo').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(UbicacionGeografica u) {
        try {
            String nuevo = "ACTIVO".equals(u.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            geografiaService.cambiarEstado(u.getId(), nuevo);
            aviso(FacesMessage.SEVERITY_INFO, "ACTIVO".equals(nuevo) ? "Ubicación activada" : "Ubicación inactivada", u.getNombre());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<UbicacionGeografica> getModelo() { return modelo; }
    public UbicacionGeografica getSeleccionado() { return seleccionado; }
    public void setSeleccionado(UbicacionGeografica seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public List<Entidad> getNiveles() { return niveles; }

    /** Descripcion del nivel (id -> texto) para la grilla (V26: nivel es id). */
    public String descripcionNivel(Long id) { return catalogoService.descripcionOpcion(id); }
}
