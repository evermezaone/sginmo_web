package py.com.one.security.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import py.com.one.security.servicio.OpcionPantalla;
import py.com.one.security.dominio.Accion;
import py.com.one.security.dominio.Grupo;
import py.com.one.security.dominio.PermisoGrupo;
import py.com.one.security.dominio.UsuarioGrupo;
import py.com.one.security.servicio.ProveedorPantallas;
import py.com.one.security.servicio.GrupoService;
import py.com.one.core.NegocioException;
import py.com.one.security.servicio.UsuarioService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de grupos de seguridad (REQ-0004, V10): permisos por grupo + integrantes. */
@Named
@ViewScoped
public class GrupoBean implements Serializable {

    public static final String PANTALLA = "grupos";

    @Inject
    private transient GrupoService grupoService;

    @Inject
    private transient UsuarioService usuarioService;

    @Inject
    private transient ProveedorPantallas proveedorPantallas;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Grupo> modelo;
    private Grupo seleccionado;
    private String filtroGlobal = "";
    private int tabActivo;

    private List<OpcionPantalla> pantallas;
    private List<PermisoGrupo> permisos = java.util.List.of();
    private String nuevoPermisoPantalla;
    private String nuevoPermisoAccion;

    private List<UsuarioGrupo> integrantes = java.util.List.of();
    private Map<Long, py.com.one.security.dominio.Usuario> usuariosPorId = java.util.Map.of();
    private Long nuevoIntegrante;

    @PostConstruct
    public void iniciar() {
        pantallas = proveedorPantallas.pantallas();
        cargarUsuarios();
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) grupoService.contar(filtroGlobal, sesion.tenantActual());
            }

            @Override
            public List<Grupo> load(int first, int pageSize,
                                    Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return grupoService.listar(first, pageSize, filtroGlobal, sesion.tenantActual());
            }
        };
    }

    private void cargarUsuarios() {
        var mapa = new java.util.LinkedHashMap<Long, py.com.one.security.dominio.Usuario>();
        // F6: los integrantes visibles son los del tenant del contexto (SUPERADMIN todos).
        usuarioService.listar(0, 500, "", sesion.tenantActual()).forEach(u -> mapa.put(u.getId(), u));
        usuariosPorId = mapa;
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) {
            return;
        }
        seleccionado = new Grupo();
        permisos = java.util.List.of();
        integrantes = java.util.List.of();
        limpiarAltas();
        tabActivo = 0;
    }

    public void editar(Grupo grupo) {
        seleccionado = grupo;
        permisos = grupoService.listarPermisos(grupo.getId());
        integrantes = grupoService.listarIntegrantes(grupo.getId());
        limpiarAltas();
        tabActivo = 0;
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (!sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) {
                return;
            }
            grupoService.guardar(seleccionado, sesion.tenantActual());
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Grupo creado" : "Grupo actualizado",
                    seleccionado.getDescripcion());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgGrupo').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(Grupo grupo) {
        try {
            String nuevo = "ACTIVO".equals(grupo.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) {
                return;
            }
            grupoService.cambiarEstado(grupo.getId(), nuevo, sesion.tenantActual());
            aviso(FacesMessage.SEVERITY_INFO,
                    "ACTIVO".equals(nuevo) ? "Grupo activado" : "Grupo inactivado (sus permisos dejan de aplicar)",
                    grupo.getDescripcion());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    // ── Permisos ──

    public void agregarPermiso() {
        try {
            grupoService.agregarPermiso(seleccionado.getId(), nuevoPermisoPantalla, nuevoPermisoAccion);
            permisos = grupoService.listarPermisos(seleccionado.getId());
            limpiarAltas();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar el permiso", e.getMessage());
        }
    }

    public void eliminarPermiso(Long permisoId) {
        grupoService.eliminarPermiso(permisoId);
        permisos = grupoService.listarPermisos(seleccionado.getId());
    }

    // ── Integrantes ──

    public void agregarIntegrante() {
        try {
            usuarioService.agregarAGrupo(nuevoIntegrante, seleccionado.getId());
            integrantes = grupoService.listarIntegrantes(seleccionado.getId());
            limpiarAltas();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar el integrante", e.getMessage());
        }
    }

    public void quitarIntegrante(Long usuarioGrupoId) {
        usuarioService.quitarDeGrupo(usuarioGrupoId);
        integrantes = grupoService.listarIntegrantes(seleccionado.getId());
    }

    public String nombreUsuario(Long usuarioId) {
        var u = usuariosPorId.get(usuarioId);
        return u == null ? String.valueOf(usuarioId) : u.getCodigoUsuario();
    }

    private void limpiarAltas() {
        nuevoPermisoPantalla = null;
        nuevoPermisoAccion = null;
        nuevoIntegrante = null;
    }

    public Accion[] getAcciones() { return Accion.values(); }

    private void aviso(FacesMessage.Severity severidad, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, titulo, detalle));
    }

    // ── Getters/Setters ──

    public LazyDataModel<Grupo> getModelo() { return modelo; }

    public Grupo getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Grupo seleccionado) { this.seleccionado = seleccionado; }

    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }

    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }

    public List<OpcionPantalla> getPantallas() { return pantallas; }
    public List<PermisoGrupo> getPermisos() { return permisos; }
    public List<UsuarioGrupo> getIntegrantes() { return integrantes; }

    public java.util.Collection<py.com.one.security.dominio.Usuario> getUsuarios() {
        return usuariosPorId.values();
    }

    public String getNuevoPermisoPantalla() { return nuevoPermisoPantalla; }
    public void setNuevoPermisoPantalla(String nuevoPermisoPantalla) { this.nuevoPermisoPantalla = nuevoPermisoPantalla; }

    public String getNuevoPermisoAccion() { return nuevoPermisoAccion; }
    public void setNuevoPermisoAccion(String nuevoPermisoAccion) { this.nuevoPermisoAccion = nuevoPermisoAccion; }

    public Long getNuevoIntegrante() { return nuevoIntegrante; }
    public void setNuevoIntegrante(Long nuevoIntegrante) { this.nuevoIntegrante = nuevoIntegrante; }
}
