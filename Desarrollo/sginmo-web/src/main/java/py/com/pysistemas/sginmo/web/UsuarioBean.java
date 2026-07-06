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
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.seguridad.Accion;
import py.com.pysistemas.sginmo.dominio.seguridad.PermisoUsuario;
import py.com.pysistemas.sginmo.dominio.seguridad.Usuario;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.NegocioException;
import py.com.pysistemas.sginmo.servicio.UsuarioService;

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
    private transient CatalogoService catalogoService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Usuario> modelo;
    private Usuario seleccionado;
    private String filtroGlobal = "";
    private String passwordInicial;
    private int tabActivo;

    private List<Entidad> pantallas;
    private List<PermisoUsuario> permisos = java.util.List.of();
    private String nuevoPermisoPantalla;
    private String nuevoPermisoAccion;

    @PostConstruct
    public void iniciar() {
        pantallas = catalogoService.opciones("PANTALLAS");
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) usuarioService.contar(filtroGlobal);
            }

            @Override
            public List<Usuario> load(int first, int pageSize,
                                      Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return usuarioService.listar(first, pageSize, filtroGlobal);
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
        limpiarNuevoPermiso();
        tabActivo = 0;
    }

    public void editar(Usuario usuario) {
        seleccionado = usuario;
        passwordInicial = null;
        permisos = usuarioService.listarPermisos(usuario.getId());
        limpiarNuevoPermiso();
        tabActivo = 0;
    }

    public void guardar() {
        try {
            boolean esNuevo = seleccionado.getId() == null;
            if (!sesion.puede(PANTALLA, esNuevo ? "CREAR" : "EDITAR")) {
                return;
            }
            usuarioService.guardar(seleccionado, passwordInicial, sesion.getUsuario().getEmpresa());
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
            usuarioService.cambiarEstado(usuario.getId(), nuevo, sesion.getUsuario().getId());
            aviso(FacesMessage.SEVERITY_INFO,
                    "ACTIVO".equals(nuevo) ? "Usuario activado" : "Usuario inactivado",
                    usuario.getCodigoUsuario());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    public void desbloquear(Usuario usuario) {
        if (!sesion.puede(PANTALLA, "EDITAR")) {
            return;
        }
        usuarioService.desbloquear(usuario.getId());
        aviso(FacesMessage.SEVERITY_INFO, "Usuario desbloqueado", usuario.getCodigoUsuario());
    }

    // ── Permisos ──

    public void agregarPermiso() {
        try {
            usuarioService.agregarPermiso(seleccionado.getId(), nuevoPermisoPantalla, nuevoPermisoAccion);
            permisos = usuarioService.listarPermisos(seleccionado.getId());
            limpiarNuevoPermiso();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar el permiso", e.getMessage());
        }
    }

    public void eliminarPermiso(Long permisoId) {
        usuarioService.eliminarPermiso(permisoId);
        permisos = usuarioService.listarPermisos(seleccionado.getId());
    }

    private void limpiarNuevoPermiso() {
        nuevoPermisoPantalla = null;
        nuevoPermisoAccion = null;
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

    public List<Entidad> getPantallas() { return pantallas; }
    public List<PermisoUsuario> getPermisos() { return permisos; }

    public String getNuevoPermisoPantalla() { return nuevoPermisoPantalla; }
    public void setNuevoPermisoPantalla(String nuevoPermisoPantalla) { this.nuevoPermisoPantalla = nuevoPermisoPantalla; }

    public String getNuevoPermisoAccion() { return nuevoPermisoAccion; }
    public void setNuevoPermisoAccion(String nuevoPermisoAccion) { this.nuevoPermisoAccion = nuevoPermisoAccion; }
}
