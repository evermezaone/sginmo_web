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

    @Inject
    private SesionUsuario sesion;

    /** Id de pantalla para permisos por accion (REQ-0004). */
    public static final String PANTALLA = "articulos";

    private LazyDataModel<Articulo> modelo;
    private Articulo seleccionado;
    private String filtroGlobal = "";

    private List<Entidad> categorias;
    private List<Entidad> unidadesMedida;
    private List<Impuesto> impuestos;

    // Propiedades parametrizables (tab habilitado solo con articulo guardado)
    private List<py.com.pysistemas.sginmo.dominio.catalogo.ArticuloPropiedad> propiedades = java.util.List.of();
    private List<Entidad> propiedadesDisponibles;
    private String nuevaPropiedadCodigo;
    private String nuevaPropiedadValor;

    /** Pestana activa del dialogo; se resetea a Principal en cada apertura (obs del usuario). */
    private int tabActivo;

    /** true si la ultima consulta tenia filtro global o de columna (para diferenciar el mensaje de grilla vacia). */
    private boolean consultaFiltrada;

    /** Dialogo en modo consulta: sin permiso EDITAR se puede VER el detalle pero no tocarlo. */
    private boolean soloLectura;

    @PostConstruct
    public void iniciar() {
        categorias = catalogoService.opciones("TIPOS_ARTICULO");
        unidadesMedida = catalogoService.opciones("UNIDADES_MEDIDA");
        impuestos = catalogoService.impuestosActivos();
        propiedadesDisponibles = catalogoService.opciones("PROPIEDADES_ARTICULO");

        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) articuloService.contar(filtroGlobal, filtrosColumna(filterBy));
            }

            @Override
            public List<Articulo> load(int first, int pageSize,
                                       Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                Map<String, Object> filtros = filtrosColumna(filterBy);
                consultaFiltrada = (filtroGlobal != null && !filtroGlobal.isBlank()) || !filtros.isEmpty();
                String ordenarPor = null;
                boolean ascendente = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    ordenarPor = sm.getField();
                    ascendente = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return articuloService.listar(first, pageSize, filtroGlobal,
                        filtros, ordenarPor, ascendente);
            }
        };
    }

    /** Convierte los FilterMeta de PrimeFaces en un mapa simple campo→valor para el servicio. */
    private Map<String, Object> filtrosColumna(Map<String, FilterMeta> filterBy) {
        var filtros = new java.util.HashMap<String, Object>();
        if (filterBy != null) {
            filterBy.forEach((clave, fm) -> {
                Object valor = fm.getFilterValue();
                if (valor != null && !valor.toString().isBlank()) {
                    filtros.put(fm.getField(), valor);
                }
            });
        }
        return filtros;
    }

    // ── Acciones ──

    /** viewAction: sin permiso VER no se entra a la pantalla. */
    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) {
            return;
        }
        seleccionado = new Articulo();
        propiedades = java.util.List.of();
        limpiarNuevaPropiedad();
        tabActivo = 0;
        soloLectura = false;
    }

    public void editar(Articulo articulo) {
        seleccionado = articulo;
        propiedades = articuloService.listarPropiedades(articulo.getId());
        limpiarNuevaPropiedad();
        tabActivo = 0;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    /** Limpia el buscador global; los filtros de columna los limpia PF('tabla').clearFilters() en el oncomplete. */
    public void limpiarBusqueda() {
        filtroGlobal = "";
    }

    /** Aviso temprano de duplicado al salir del campo codigo (el guardado igual re-valida). */
    public void verificarCodigo() {
        if (seleccionado != null
                && articuloService.existeCodigo(seleccionado.getCodigo(), seleccionado.getId())) {
            aviso(FacesMessage.SEVERITY_WARN, "Código ya utilizado",
                    "Ya existe un artículo con el código '" + seleccionado.getCodigo() + "'");
        }
    }

    /** Aviso temprano de duplicado al salir del campo aplicacion. */
    public void verificarAplicacion() {
        if (seleccionado != null
                && articuloService.existeAplicacion(seleccionado.getAplicacion(), seleccionado.getId())) {
            aviso(FacesMessage.SEVERITY_WARN, "Aplicación ya asignada",
                    "La aplicación '" + seleccionado.getAplicacion() + "' ya está asignada a otro artículo");
        }
    }

    public void agregarPropiedad() {
        try {
            articuloService.agregarPropiedad(seleccionado.getId(), nuevaPropiedadCodigo, nuevaPropiedadValor);
            propiedades = articuloService.listarPropiedades(seleccionado.getId());
            limpiarNuevaPropiedad();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar la propiedad", e.getMessage());
        }
    }

    public void eliminarPropiedad(Long propiedadId) {
        articuloService.eliminarPropiedad(propiedadId);
        propiedades = articuloService.listarPropiedades(seleccionado.getId());
    }

    private void limpiarNuevaPropiedad() {
        nuevaPropiedadCodigo = null;
        nuevaPropiedadValor = null;
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) {
                return;
            }
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
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) {
                return;
            }
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

    public List<py.com.pysistemas.sginmo.dominio.catalogo.ArticuloPropiedad> getPropiedades() { return propiedades; }
    public List<Entidad> getPropiedadesDisponibles() { return propiedadesDisponibles; }

    public String getNuevaPropiedadCodigo() { return nuevaPropiedadCodigo; }
    public void setNuevaPropiedadCodigo(String nuevaPropiedadCodigo) { this.nuevaPropiedadCodigo = nuevaPropiedadCodigo; }

    public String getNuevaPropiedadValor() { return nuevaPropiedadValor; }
    public void setNuevaPropiedadValor(String nuevaPropiedadValor) { this.nuevaPropiedadValor = nuevaPropiedadValor; }

    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }

    public boolean isConsultaFiltrada() { return consultaFiltrada; }

    public boolean isSoloLectura() { return soloLectura; }
}
