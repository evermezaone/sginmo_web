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
import py.com.one.security.dominio.PermisoUsuario;
import py.com.one.security.dominio.Usuario;
import py.com.one.security.servicio.ProveedorPantallas;
import py.com.one.core.NegocioException;
import py.com.one.security.servicio.UsuarioService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de usuarios y permisos por accion (REQ-0004), siguiendo el patron estandar. */
@Named
@ViewScoped
public class UsuarioBean implements Serializable {

    public static final String PANTALLA = "usuarios";

    @Inject
    private transient UsuarioService usuarioService;

    @Inject
    private transient ProveedorPantallas proveedorPantallas;

    @Inject
    private transient py.com.one.security.servicio.GrupoService grupoService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Usuario> modelo;
    private Usuario seleccionado;
    private String filtroGlobal = "";
    private String passwordInicial;
    private int tabActivo;

    /** Dialogo en modo consulta: sin EDITAR se ve el detalle pero no se toca (obs 209 de Codex). */
    private boolean soloLectura;

    private List<OpcionPantalla> pantallas;
    private List<PermisoUsuario> permisos = java.util.List.of();
    private String nuevoPermisoPantalla;
    private String nuevoPermisoAccion;

    // Grupos del usuario (V10)
    private List<py.com.one.security.dominio.UsuarioGrupo> grupos = java.util.List.of();
    private List<py.com.one.security.dominio.Grupo> gruposActivos = java.util.List.of();
    private java.util.Map<Long, py.com.one.security.dominio.Grupo> gruposPorId = java.util.Map.of();
    private Long nuevoGrupo;

    @PostConstruct
    public void iniciar() {
        pantallas = proveedorPantallas.pantallas();
        // F6: el ABM se aisla por tenant; SUPERADMIN (-1) ve todo, ADMINISTRADOR solo su empresa.
        gruposActivos = grupoService.gruposActivos(sesion.tenantActual());
        gruposPorId = usuarioService.gruposPorId(sesion.tenantActual());
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) usuarioService.contar(filtroGlobal, sesion.tenantActual());
            }

            @Override
            public List<Usuario> load(int first, int pageSize,
                                      Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return usuarioService.listar(first, pageSize, filtroGlobal, sesion.tenantActual());
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) {
            return;
        }
        seleccionado = new Usuario();
        seleccionado.setPerfil("USUARIO");
        passwordInicial = null;
        permisos = java.util.List.of();
        grupos = java.util.List.of();
        limpiarNuevoPermiso();
        tabActivo = 0;
        soloLectura = false;
    }

    public void editar(Usuario usuario) {
        seleccionado = usuario;
        passwordInicial = null;
        permisos = usuarioService.listarPermisos(usuario.getId(), sesion.tenantActual());
        grupos = usuarioService.listarGruposDe(usuario.getId(), sesion.tenantActual());
        limpiarNuevoPermiso();
        tabActivo = 0;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) {
                return;
            }
            usuarioService.guardar(seleccionado, passwordInicial,
                    sesion.tenantActual(), sesion.tenantActual());
            aviso(FacesMessage.SEVERITY_INFO, esNuevo ? "Usuario creado" : "Usuario actualizado",
                    seleccionado.getCodigoUsuario()
                    + (passwordInicial != null && !passwordInicial.isBlank()
                        ? " — deberá cambiar la contraseña al ingresar" : ""));
            passwordInicial = null;
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgUsuario').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(Usuario usuario) {
        try {
            String nuevo = "ACTIVO".equals(usuario.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) {
                return;
            }
            usuarioService.cambiarEstado(usuario.getId(), nuevo,
                    sesion.getUsuario().getId(), sesion.tenantActual());
            aviso(FacesMessage.SEVERITY_INFO,
                    "ACTIVO".equals(nuevo) ? "Usuario activado" : "Usuario inactivado",
                    usuario.getCodigoUsuario());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    public void desbloquear(Usuario usuario) {
        try {
            if (!sesion.puede(PANTALLA, "EDITAR")) {
                return;
            }
            usuarioService.desbloquear(usuario.getId(), sesion.tenantActual());
            aviso(FacesMessage.SEVERITY_INFO, "Usuario desbloqueado", usuario.getCodigoUsuario());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo desbloquear", e.getMessage());
        }
    }

    // ── Permisos ──

    public void agregarPermiso() {
        try {
            usuarioService.agregarPermiso(seleccionado.getId(), nuevoPermisoPantalla, nuevoPermisoAccion,
                    sesion.tenantActual());
            permisos = usuarioService.listarPermisos(seleccionado.getId(), sesion.tenantActual());
            limpiarNuevoPermiso();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar el permiso", e.getMessage());
        }
    }

    public void eliminarPermiso(Long permisoId) {
        try {
            usuarioService.eliminarPermiso(permisoId, sesion.tenantActual());
            permisos = usuarioService.listarPermisos(seleccionado.getId(), sesion.tenantActual());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo quitar el permiso", e.getMessage());
        }
    }

    private void limpiarNuevoPermiso() {
        nuevoPermisoPantalla = null;
        nuevoPermisoAccion = null;
        nuevoGrupo = null;
    }

    // ── Grupos del usuario ──

    public void agregarGrupo() {
        try {
            usuarioService.agregarAGrupo(seleccionado.getId(), nuevoGrupo, sesion.tenantActual());
            grupos = usuarioService.listarGruposDe(seleccionado.getId(), sesion.tenantActual());
            limpiarNuevoPermiso();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar al grupo", e.getMessage());
        }
    }

    public void quitarGrupo(Long usuarioGrupoId) {
        try {
            usuarioService.quitarDeGrupo(usuarioGrupoId, sesion.tenantActual());
            grupos = usuarioService.listarGruposDe(seleccionado.getId(), sesion.tenantActual());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo quitar del grupo", e.getMessage());
        }
    }

    public String nombreGrupo(Long grupoId) {
        var g = gruposPorId.get(grupoId);
        return g == null ? String.valueOf(grupoId) : g.getDescripcion() + " (" + g.getCodigo() + ")";
    }

    public Accion[] getAcciones() {
        return Accion.values();
    }

    private void aviso(FacesMessage.Severity severidad, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, titulo, detalle));
    }

    // ── Getters/Setters ──

    public LazyDataModel<Usuario> getModelo() { return modelo; }

    public Usuario getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Usuario seleccionado) { this.seleccionado = seleccionado; }

    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }

    public String getPasswordInicial() { return passwordInicial; }
    public void setPasswordInicial(String passwordInicial) { this.passwordInicial = passwordInicial; }

    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }

    public List<OpcionPantalla> getPantallas() { return pantallas; }
    public List<PermisoUsuario> getPermisos() { return permisos; }

    public String getNuevoPermisoPantalla() { return nuevoPermisoPantalla; }
    public void setNuevoPermisoPantalla(String nuevoPermisoPantalla) { this.nuevoPermisoPantalla = nuevoPermisoPantalla; }

    public String getNuevoPermisoAccion() { return nuevoPermisoAccion; }
    public void setNuevoPermisoAccion(String nuevoPermisoAccion) { this.nuevoPermisoAccion = nuevoPermisoAccion; }

    public List<py.com.one.security.dominio.UsuarioGrupo> getGrupos() { return grupos; }
    public List<py.com.one.security.dominio.Grupo> getGruposActivos() { return gruposActivos; }

    public Long getNuevoGrupo() { return nuevoGrupo; }
    public void setNuevoGrupo(Long nuevoGrupo) { this.nuevoGrupo = nuevoGrupo; }

    public boolean isSoloLectura() { return soloLectura; }
}
