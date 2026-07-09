package py.com.pysistemas.sginmo.web;

import py.com.one.security.web.SesionUsuario;
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
import py.com.one.core.NegocioException;

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

    @Inject
    private transient py.com.one.security.servicio.PreferenciaService preferenciaService;

    @Inject
    private transient py.com.pysistemas.sginmo.servicio.PersonaService personaService;

    /** Id de pantalla para permisos por accion (REQ-0004). */
    public static final String PANTALLA = "articulos";

    private LazyDataModel<Articulo> modelo;
    private Articulo seleccionado;
    private String filtroGlobal = "";

    private List<Entidad> categorias;
    private List<Entidad> unidadesMedida;
    private List<Impuesto> impuestos;
    // Clasificacion extendida (listas configurables) + proveedores
    private List<Entidad> presentaciones;
    private List<Entidad> marcas;
    private List<Entidad> modelos;
    private List<Entidad> familias;
    private List<Entidad> grupos;
    private List<Entidad> subgrupos;
    private List<Entidad> procedencias;
    private List<py.com.pysistemas.sginmo.dominio.persona.Persona> proveedores;

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

    /** Panel de filtros por columna visible (se abre solo si Mi vista trae filtros). */
    private boolean filtrosVisibles;

    /** Origen del clonado: al guardar la copia se traen tambien sus propiedades. */
    private Long duplicadoDe;

    @PostConstruct
    public void iniciar() {
        categorias = catalogoService.opciones("TIPOS_ARTICULO");
        unidadesMedida = catalogoService.opciones("UNIDADES_MEDIDA");
        impuestos = catalogoService.impuestosActivos();
        propiedadesDisponibles = catalogoService.opciones("PROPIEDADES_ARTICULO");
        presentaciones = catalogoService.opciones("PRESENTACIONES");
        marcas = catalogoService.opciones("MARCAS");
        modelos = catalogoService.opciones("MODELOS");
        familias = catalogoService.opciones("FAMILIAS_ARTICULO");
        grupos = catalogoService.opciones("GRUPOS_ARTICULO");
        subgrupos = catalogoService.opciones("SUBGRUPOS_ARTICULO");
        procedencias = catalogoService.opciones("PROCEDENCIAS");
        proveedores = personaService.porRol("PROVEEDOR");

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

    /** viewAction: sin permiso VER no se entra a la pantalla; con acceso, se aplica Mi vista. */
    public String verificarAcceso() {
        if (!sesion.puede(PANTALLA, "VER")) {
            return "/index?faces-redirect=true";
        }
        aplicarMiVista();
        return null;
    }

    // ── Mi vista (REQ-0004): columnas visibles + orden + filtros + filas por pagina ──

    private org.primefaces.component.datatable.DataTable tabla() {
        return (org.primefaces.component.datatable.DataTable)
                FacesContext.getCurrentInstance().getViewRoot().findComponent("frmLista:tabla");
    }

    public void guardarMiVista() {
        var dt = tabla();
        if (dt == null) {
            return;
        }
        var json = jakarta.json.Json.createObjectBuilder();
        var columnas = jakarta.json.Json.createObjectBuilder();
        for (var col : dt.getColumns()) {
            if (col instanceof org.primefaces.component.column.Column c && c.isRendered() && c.isToggleable()) {
                columnas.add(c.getHeaderText(), c.isVisible());
            }
        }
        json.add("columnas", columnas);
        json.add("filas", dt.getRows());
        json.add("filtroGlobal", filtroGlobal == null ? "" : filtroGlobal);
        dt.getSortByAsMap().values().stream()
            .filter(SortMeta::isActive).findFirst()
            .ifPresent(s -> json.add("orden", s.getField())
                                .add("asc", s.getOrder() != org.primefaces.model.SortOrder.DESCENDING));
        var filtrosJson = jakarta.json.Json.createObjectBuilder();
        dt.getFilterByAsMap().forEach((clave, fm) -> {
            if (fm.getFilterValue() != null && !"globalFilter".equals(fm.getField())) {
                filtrosJson.add(fm.getField(), fm.getFilterValue().toString());
            }
        });
        json.add("filtros", filtrosJson);
        preferenciaService.guardar(sesion.getUsuario().getId(), PANTALLA, "mi_vista", json.build().toString());
        aviso(FacesMessage.SEVERITY_INFO, "Mi vista guardada", "Se aplicará automáticamente al entrar a esta pantalla");
    }

    public void quitarMiVista() {
        preferenciaService.eliminar(sesion.getUsuario().getId(), PANTALLA, "mi_vista");
        aviso(FacesMessage.SEVERITY_INFO, "Mi vista eliminada", "La pantalla vuelve a la configuración estándar");
    }

    /** Restaura la vista guardada; cualquier problema con el JSON se ignora (vista corrupta no rompe la pantalla). */
    private void aplicarMiVista() {
        try {
            String json = preferenciaService.leer(sesion.getUsuario().getId(), PANTALLA, "mi_vista");
            if (json == null) {
                return;
            }
            var dt = tabla();
            if (dt == null) {
                return;
            }
            try (var lector = jakarta.json.Json.createReader(new java.io.StringReader(json))) {
                var vista = lector.readObject();
                var columnas = vista.getJsonObject("columnas");
                if (columnas != null) {
                    for (var col : dt.getColumns()) {
                        if (col instanceof org.primefaces.component.column.Column c
                                && columnas.containsKey(c.getHeaderText())) {
                            c.setVisible(columnas.getBoolean(c.getHeaderText()));
                        }
                    }
                }
                dt.setRows(vista.getInt("filas", 10));
                filtroGlobal = vista.getString("filtroGlobal", "");
                if (vista.containsKey("orden")) {
                    var orden = SortMeta.builder()
                        .field(vista.getString("orden"))
                        .order(vista.getBoolean("asc", true)
                                ? org.primefaces.model.SortOrder.ASCENDING
                                : org.primefaces.model.SortOrder.DESCENDING)
                        .build();
                    var mapaOrden = new java.util.HashMap<String, SortMeta>();
                    mapaOrden.put(orden.getField(), orden);
                    dt.setSortByAsMap(mapaOrden);
                }
                var filtrosVista = vista.getJsonObject("filtros");
                if (filtrosVista != null && !filtrosVista.isEmpty()) {
                    var mapaFiltros = new java.util.HashMap<String, FilterMeta>();
                    filtrosVista.forEach((campo, valor) -> mapaFiltros.put(campo,
                        FilterMeta.builder().field(campo)
                            .filterValue(((jakarta.json.JsonString) valor).getString()).build()));
                    dt.setFilterByAsMap(mapaFiltros);
                    filtrosVisibles = true;   // que no queden filtros activos invisibles
                }
            }
        } catch (RuntimeException e) {
            // vista corrupta o incompatible con la pantalla actual: se ignora
        }
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
        duplicadoDe = null;
    }

    /** Clonado (regla 3 del estandar): copia todo salvo las claves unicas (codigo/aplicacion). */
    public void duplicar(Articulo origen) {
        if (!sesion.puede(PANTALLA, "CREAR")) {
            return;
        }
        var copia = new Articulo();
        copia.setDescripcion(origen.getDescripcion() + " (copia)");
        copia.setTipo(origen.getTipo());
        copia.setImpuesto(origen.getImpuesto());
        copia.setPrecioUnitario(origen.getPrecioUnitario());
        copia.setCategoria(origen.getCategoria());
        copia.setUnidadMedida(origen.getUnidadMedida());
        copia.setTipoMovimiento(origen.getTipoMovimiento());
        copia.setModificaEstado(origen.isModificaEstado());
        copia.setStockMinimo(origen.getStockMinimo());
        copia.setStockMaximo(origen.getStockMaximo());
        copia.setObservacion(origen.getObservacion());
        copia.setHabilitado(origen.getHabilitado());
        seleccionado = copia;
        duplicadoDe = origen.getId();
        propiedades = java.util.List.of();
        limpiarNuevaPropiedad();
        tabActivo = 0;
        soloLectura = false;
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
            if (esNuevo && duplicadoDe != null) {
                articuloService.copiarPropiedades(duplicadoDe, seleccionado.getId());
                propiedades = articuloService.listarPropiedades(seleccionado.getId());
                duplicadoDe = null;
            }
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
    public List<Entidad> getPresentaciones() { return presentaciones; }
    public List<Entidad> getMarcas() { return marcas; }
    public List<Entidad> getModelos() { return modelos; }
    public List<Entidad> getFamilias() { return familias; }
    public List<Entidad> getGrupos() { return grupos; }
    public List<Entidad> getSubgrupos() { return subgrupos; }
    public List<Entidad> getProcedencias() { return procedencias; }

    /** Descripcion de la propiedad (id -> texto) para la grilla (V26: propiedad es id). */
    public String descripcionPropiedad(Long id) { return catalogoService.descripcionOpcion(id); }
    public List<py.com.pysistemas.sginmo.dominio.persona.Persona> getProveedores() { return proveedores; }

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

    public boolean isFiltrosVisibles() { return filtrosVisibles; }
    public void setFiltrosVisibles(boolean filtrosVisibles) { this.filtrosVisibles = filtrosVisibles; }
}
