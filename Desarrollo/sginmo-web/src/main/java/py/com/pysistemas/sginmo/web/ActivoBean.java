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
import py.com.pysistemas.sginmo.dominio.activo.Activo;
import py.com.pysistemas.sginmo.dominio.activo.ActivoAtributoValor;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.servicio.ActivoService;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.PersonaService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de activos inmobiliarios (REQ-0013/0014), patron estandar con atributos por tipo. */
@Named
@ViewScoped
public class ActivoBean implements Serializable {

    public static final String PANTALLA = "activos";

    @Inject
    private transient ActivoService activoService;

    @Inject
    private transient CatalogoService catalogoService;

    @Inject
    private transient PersonaService personaService;

    @Inject
    private transient py.com.pysistemas.sginmo.servicio.GeografiaService geografiaService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Activo> modelo;
    private Activo seleccionado;
    private String filtroGlobal = "";
    private boolean soloLectura;
    private boolean consultaFiltrada;
    private int tabActivo;

    private List<Entidad> tipos;
    private List<ActivoAtributoValor> atributos = java.util.List.of();
    private List<Object[]> propietarios = java.util.List.of();
    private List<Persona> propietariosPosibles;
    private Long nuevoPropietario;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> paises = java.util.List.of();
    private List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> departamentos = java.util.List.of();
    private List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> ciudades = java.util.List.of();
    private List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> barrios = java.util.List.of();
    private Long pais;
    private Long departamento;
    private Long ciudad;
    private Long barrio;

    // Generacion masiva de lotes (REQ-0015)
    private Long loteContenedor;
    private List<Activo> loteamientos = java.util.List.of();
    private String loteManzana;
    private int loteDesde = 1;
    private int loteCantidad = 10;
    private java.math.BigDecimal lotePrecio = java.math.BigDecimal.ZERO;
    private java.math.BigDecimal loteComision = java.math.BigDecimal.ZERO;

    @PostConstruct
    public void iniciar() {
        tipos = catalogoService.opciones("TIPOS_ACTIVO");
        propietariosPosibles = personaService.porRol("PROPIETARIO");
        paises = geografiaService.porNivel("PAIS");
        loteamientos = activoService.loteamientos();
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) activoService.contar(filtroGlobal);
            }

            @Override
            public List<Activo> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                consultaFiltrada = filtroGlobal != null && !filtroGlobal.isBlank();
                String orden = null; boolean asc = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    orden = sm.getField(); asc = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return activoService.listar(first, pageSize, filtroGlobal, orden, asc);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Activo();
        atributos = java.util.List.of();
        propietarios = java.util.List.of();
        nuevoPropietario = null;
        limpiarUbicacion();
        soloLectura = false; tabActivo = 0;
    }

    public void editar(Activo activo) {
        seleccionado = activo;
        atributos = activoService.atributosDe(activo.getId(), activo.getTipo());
        propietarios = activoService.propietariosConId(activo.getId());
        nuevoPropietario = null;
        cargarUbicacion(activo.getUbicacion());
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
        tabActivo = 0;
    }

    /** Al cambiar el tipo se recargan los atributos parametrizados de ese tipo. */
    public void tipoCambiado() {
        atributos = activoService.atributosDe(seleccionado == null ? null : seleccionado.getId(),
                seleccionado == null ? null : seleccionado.getTipo());
    }

    public List<Activo> completarContenedor(String texto) {
        return activoService.buscarContenedor(texto, seleccionado == null ? null : seleccionado.getId());
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) return;
            seleccionado.setUbicacion(ubicacionSeleccionada());
            activoService.guardar(seleccionado, atributos);
            guardarPropietariosPendientes();
            if (esNuevo) {
                atributos = activoService.atributosDe(seleccionado.getId(), seleccionado.getTipo());
            }
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Activo creado" : "Activo actualizado", seleccionado.getNombre());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgActivo').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void agregarPropietario() {
        try {
            if (nuevoPropietario == null) throw new NegocioException("Elija el propietario");
            if (seleccionado.getId() == null) {
                if (propietarios.stream().anyMatch(p -> nuevoPropietario.equals(propietarioIdFila(p)))) {
                    throw new NegocioException("Esa persona ya figura como propietaria");
                }
                String nombre = propietariosPosibles.stream()
                    .filter(p -> p.getId().equals(nuevoPropietario))
                    .map(Persona::getNombre)
                    .findFirst()
                    .orElseThrow(() -> new NegocioException("El propietario seleccionado ya no esta disponible"));
                var copia = new java.util.ArrayList<Object[]>(propietarios);
                copia.add(new Object[] { null, nombre, nuevoPropietario });
                propietarios = copia;
                nuevoPropietario = null;
                return;
            }
            activoService.agregarPropietario(seleccionado.getId(), nuevoPropietario);
            propietarios = activoService.propietariosConId(seleccionado.getId());
            nuevoPropietario = null;
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar el propietario", e.getMessage());
        }
    }

    public void quitarPropietario(Object[] fila) {
        try {
            if (fila == null) return;
            if (fila[0] == null) {
                Long propietarioId = propietarioIdFila(fila);
                propietarios = propietarios.stream()
                    .filter(p -> !java.util.Objects.equals(propietarioIdFila(p), propietarioId))
                    .toList();
                return;
            }
            activoService.quitarPropietario(((Number) fila[0]).longValue());
            propietarios = activoService.propietariosConId(seleccionado.getId());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo quitar el propietario", e.getMessage());
        }
    }

    private Long propietarioIdFila(Object[] fila) {
        if (fila == null) return null;
        if (fila.length > 2 && fila[2] != null) return ((Number) fila[2]).longValue();
        return null;
    }

    private void guardarPropietariosPendientes() {
        if (seleccionado.getId() == null) return;
        var pendientes = propietarios.stream()
            .filter(p -> p[0] == null && propietarioIdFila(p) != null)
            .map(this::propietarioIdFila)
            .toList();
        for (Long propietarioId : pendientes) {
            activoService.agregarPropietario(seleccionado.getId(), propietarioId);
        }
        if (!pendientes.isEmpty()) {
            propietarios = activoService.propietariosConId(seleccionado.getId());
        }
    }

    public List<Activo> completarLoteamiento(String texto) {
        return activoService.buscarLoteamiento(texto);
    }

    public void paisCambiado() {
        departamento = null; ciudad = null; barrio = null;
        departamentos = geografiaService.hijosDe(pais);
        ciudades = java.util.List.of();
        barrios = java.util.List.of();
    }

    public void departamentoCambiado() {
        ciudad = null; barrio = null;
        ciudades = geografiaService.hijosDe(departamento);
        barrios = java.util.List.of();
    }

    public void ciudadCambiada() {
        barrio = null;
        barrios = geografiaService.hijosDe(ciudad);
    }

    private Long ubicacionSeleccionada() {
        if (barrio != null) return barrio;
        if (ciudad != null) return ciudad;
        if (departamento != null) return departamento;
        return pais;
    }

    private void limpiarUbicacion() {
        pais = null; departamento = null; ciudad = null; barrio = null;
        departamentos = java.util.List.of();
        ciudades = java.util.List.of();
        barrios = java.util.List.of();
    }

    private void cargarUbicacion(Long ubicacionId) {
        limpiarUbicacion();
        var actual = geografiaService.buscarPorId(ubicacionId);
        while (actual != null) {
            String nivel = geografiaService.codigoNivel(actual.getNivel());
            if ("BARRIO".equals(nivel)) barrio = actual.getId();
            else if ("CIUDAD".equals(nivel)) ciudad = actual.getId();
            else if ("DEPARTAMENTO".equals(nivel)) departamento = actual.getId();
            else if ("PAIS".equals(nivel)) pais = actual.getId();
            actual = actual.getPadre();
        }
        if (pais != null) departamentos = geografiaService.hijosDe(pais);
        if (departamento != null) ciudades = geografiaService.hijosDe(departamento);
        if (ciudad != null) barrios = geografiaService.hijosDe(ciudad);
    }

    public void generarLotes() {
        try {
            if (!sesion.puede(PANTALLA, "CREAR")) return;
            int creados = activoService.generarLotes(loteContenedor, "LOTE", loteManzana,
                    loteDesde, loteCantidad, lotePrecio, loteComision);
            aviso(FacesMessage.SEVERITY_INFO, "Lotes generados", creados + " lote(s) creado(s)");
            loteamientos = activoService.loteamientos();
            loteContenedor = null; loteManzana = null; loteDesde = 1; loteCantidad = 10;
            lotePrecio = java.math.BigDecimal.ZERO; loteComision = java.math.BigDecimal.ZERO;
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgLotes').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudieron generar los lotes", e.getMessage());
        }
    }

    public String descripcionTipo(Long id) {
        if (id == null) return "";
        return tipos.stream().filter(t -> t.getId().equals(id)).map(Entidad::getDescripcion)
                .findFirst().orElse(String.valueOf(id));
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<Activo> getModelo() { return modelo; }
    public Activo getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Activo seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public boolean isConsultaFiltrada() { return consultaFiltrada; }
    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }
    public List<Entidad> getTipos() { return tipos; }
    public List<ActivoAtributoValor> getAtributos() { return atributos; }
    public List<Object[]> getPropietarios() { return propietarios; }
    public List<Persona> getPropietariosPosibles() { return propietariosPosibles; }
    public Long getNuevoPropietario() { return nuevoPropietario; }
    public void setNuevoPropietario(Long nuevoPropietario) { this.nuevoPropietario = nuevoPropietario; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> getPaises() { return paises; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> getDepartamentos() { return departamentos; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> getCiudades() { return ciudades; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> getBarrios() { return barrios; }
    public Long getPais() { return pais; }
    public void setPais(Long pais) { this.pais = pais; }
    public Long getDepartamento() { return departamento; }
    public void setDepartamento(Long departamento) { this.departamento = departamento; }
    public Long getCiudad() { return ciudad; }
    public void setCiudad(Long ciudad) { this.ciudad = ciudad; }
    public Long getBarrio() { return barrio; }
    public void setBarrio(Long barrio) { this.barrio = barrio; }

    public Long getLoteContenedor() { return loteContenedor; }
    public void setLoteContenedor(Long v) { this.loteContenedor = v; }
    public List<Activo> getLoteamientos() { return loteamientos; }
    public String getLoteManzana() { return loteManzana; }
    public void setLoteManzana(String v) { this.loteManzana = v; }
    public int getLoteDesde() { return loteDesde; }
    public void setLoteDesde(int v) { this.loteDesde = v; }
    public int getLoteCantidad() { return loteCantidad; }
    public void setLoteCantidad(int v) { this.loteCantidad = v; }
    public java.math.BigDecimal getLotePrecio() { return lotePrecio; }
    public void setLotePrecio(java.math.BigDecimal v) { this.lotePrecio = v; }
    public java.math.BigDecimal getLoteComision() { return loteComision; }
    public void setLoteComision(java.math.BigDecimal v) { this.loteComision = v; }
}
