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
import py.com.pysistemas.sginmo.dominio.catalogo.Articulo;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.catalogo.FormaPago;
import py.com.pysistemas.sginmo.dominio.operacion.IngresoEgreso;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.IngresoEgresoService;
import py.com.pysistemas.sginmo.servicio.PersonaService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de ingresos/egresos (REQ-0024). */
@Named
@ViewScoped
public class IngresoEgresoBean implements Serializable {

    public static final String PANTALLA = "ingresos-egresos";

    @Inject
    private transient IngresoEgresoService service;
    @Inject
    private transient CatalogoService catalogoService;
    @Inject
    private transient PersonaService personaService;
    @Inject
    private ContextoEmpresa contexto;
    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<IngresoEgreso> modelo;
    private IngresoEgreso seleccionado;
    private String filtroGlobal = "";
    private String filtroTipo = "";
    private boolean soloLectura;

    private List<Articulo> articulos;
    private List<Persona> personas;
    private List<FormaPago> formasPago;
    private List<Entidad> imputaciones;

    @PostConstruct
    public void iniciar() {
        articulos = catalogoService.articulosActivos();
        personas = personaService.listar(0, 500, "", null, true);
        formasPago = catalogoService.formasHabilitadas();
        imputaciones = catalogoService.opciones("TIPOS_IMPUTACION");
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> f) { return (int) service.contar(empresaContexto(), filtroGlobal, filtroTipo); }
            @Override
            public List<IngresoEgreso> load(int first, int size, Map<String, SortMeta> s, Map<String, FilterMeta> f) {
                return service.listar(empresaContexto(), first, size, filtroGlobal, filtroTipo);
            }
        };
    }

    /** Empresa del contexto (obs 228): la pantalla solo opera sobre sus movimientos. */
    private Long empresaContexto() {
        return contexto.getEmpresa() == null ? null : contexto.getEmpresa().getId();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new IngresoEgreso();
        if (contexto.getEmpresa() != null) seleccionado.setTenant(contexto.getEmpresa().getId());
        soloLectura = false;
    }

    public void editar(IngresoEgreso ie) {
        seleccionado = ie;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR") || "ANULADO".equals(ie.getEstado());
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) return;
            service.guardar(seleccionado, empresaContexto());
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Movimiento registrado" : "Movimiento actualizado",
                    seleccionado.getTipo());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgIE').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void anular(IngresoEgreso ie) {
        try {
            service.anular(ie.getId(), empresaContexto());
            aviso(FacesMessage.SEVERITY_INFO, "Movimiento anulado", "");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo anular", e.getMessage());
        }
    }

    public String concepto(Long articuloId) { return service.conceptoDe(articuloId); }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<IngresoEgreso> getModelo() { return modelo; }
    public IngresoEgreso getSeleccionado() { return seleccionado; }
    public void setSeleccionado(IngresoEgreso v) { this.seleccionado = v; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String v) { this.filtroGlobal = v; }
    public String getFiltroTipo() { return filtroTipo; }
    public void setFiltroTipo(String v) { this.filtroTipo = v; }
    public boolean isSoloLectura() { return soloLectura; }
    public List<Articulo> getArticulos() { return articulos; }
    public List<Persona> getPersonas() { return personas; }
    public List<FormaPago> getFormasPago() { return formasPago; }
    public List<Entidad> getImputaciones() { return imputaciones; }
}
