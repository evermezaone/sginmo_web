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
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.dominio.persona.PersonaEmpresa;
import py.com.pysistemas.sginmo.dominio.persona.PersonaJuridica;
import py.com.pysistemas.sginmo.dominio.persona.Sucursal;
import py.com.pysistemas.sginmo.servicio.EmpresaService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de empresas y sus sucursales (REQ-0009, patron estandar). */
@Named
@ViewScoped
public class EmpresaBean implements Serializable {

    public static final String PANTALLA = "empresas";

    @Inject
    private transient EmpresaService empresaService;

    @Inject
    private SesionUsuario sesion;

    @Inject
    private ContextoEmpresa contexto;

    @Inject
    private TenantContext tenant;

    private LazyDataModel<PersonaJuridica> modelo;
    private PersonaJuridica seleccionado;
    /** Datos comerciales propios de la empresa (persona_empresa, tenant = ella misma). */
    private PersonaEmpresa datosEmpresa = new PersonaEmpresa();
    private String filtroGlobal = "";
    private boolean soloLectura;
    private boolean consultaFiltrada;
    private int tabActivo;

    private List<Sucursal> sucursales = java.util.List.of();
    private Sucursal sucursalNueva = new Sucursal();

    /** Alta de empresa como unidad (F6, SUPERADMIN): admin inicial + sucursal por defecto. */
    private py.com.one.security.dominio.Usuario adminInicial = new py.com.one.security.dominio.Usuario();
    private String passwordAdmin;
    private Sucursal sucursalInicial = new Sucursal();

    @PostConstruct
    public void iniciar() {
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) empresaService.contar(filtroGlobal);
            }

            @Override
            public List<PersonaJuridica> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                consultaFiltrada = filtroGlobal != null && !filtroGlobal.isBlank();
                return empresaService.listar(first, pageSize, filtroGlobal);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new PersonaJuridica();
        seleccionado.setPersona(new Persona());
        datosEmpresa = new PersonaEmpresa();
        sucursales = java.util.List.of();
        sucursalNueva = new Sucursal();
        adminInicial = new py.com.one.security.dominio.Usuario();
        passwordAdmin = null;
        sucursalInicial = new Sucursal();
        soloLectura = false;
        tabActivo = 0;
    }

    /** Alta como UNIDAD: solo cuando el SUPERADMIN crea una empresa nueva (provisiona
     *  ademas el usuario ADMINISTRADOR inicial y la sucursal por defecto en una sola tx). */
    public boolean isAltaUnidad() {
        return seleccionado != null && seleccionado.getId() == null && tenant.esSuperadmin();
    }

    public void editar(PersonaJuridica empresa) {
        seleccionado = empresa;
        datosEmpresa = empresaService.datosDe(empresa.getId());
        if (datosEmpresa == null) datosEmpresa = new PersonaEmpresa();
        sucursales = empresaService.sucursalesDe(empresa.getId());
        sucursalNueva = new Sucursal();
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
        tabActivo = 0;
    }

    public void guardar() {
        try {
            boolean esNueva = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNueva ? "CREAR" : "EDITAR")) return;
            if (isAltaUnidad()) {
                // SUPERADMIN provisiona la empresa completa (pj+rol+sucursal por defecto+admin) en una tx.
                empresaService.altaEmpresa(seleccionado, datosEmpresa, sucursalInicial, adminInicial, passwordAdmin);
            } else {
                empresaService.guardar(seleccionado, datosEmpresa);
            }
            contexto.refrescar();
            aviso(FacesMessage.SEVERITY_INFO, esNueva ? "Empresa creada" : "Empresa actualizada", seleccionado.getRazonSocial());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgEmpresa').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(PersonaJuridica empresa) {
        try {
            String nuevo = "ACTIVO".equals(empresa.getPersona().getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            empresaService.cambiarEstado(empresa.getId(), nuevo);
            aviso(FacesMessage.SEVERITY_INFO, "ACTIVO".equals(nuevo) ? "Empresa activada" : "Empresa inactivada", empresa.getRazonSocial());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    // ── Sucursales ──

    public void agregarSucursal() {
        try {
            sucursalNueva.setPersonaJuridica(seleccionado.getId());
            empresaService.guardarSucursal(sucursalNueva);
            sucursales = empresaService.sucursalesDe(seleccionado.getId());
            sucursalNueva = new Sucursal();
            contexto.refrescar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar la sucursal", e.getMessage());
        }
    }

    public void cambiarEstadoSucursal(Sucursal s) {
        try {
            String nuevo = "ACTIVO".equals(s.getEstado()) ? "INACTIVO" : "ACTIVO";
            empresaService.cambiarEstadoSucursal(s.getId(), nuevo);
            sucursales = empresaService.sucursalesDe(seleccionado.getId());
            contexto.refrescar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<PersonaJuridica> getModelo() { return modelo; }
    public PersonaJuridica getSeleccionado() { return seleccionado; }
    public void setSeleccionado(PersonaJuridica seleccionado) { this.seleccionado = seleccionado; }
    public PersonaEmpresa getDatosEmpresa() { return datosEmpresa; }
    public void setDatosEmpresa(PersonaEmpresa datosEmpresa) { this.datosEmpresa = datosEmpresa; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public boolean isConsultaFiltrada() { return consultaFiltrada; }
    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }
    public List<Sucursal> getSucursales() { return sucursales; }
    public Sucursal getSucursalNueva() { return sucursalNueva; }
    public py.com.one.security.dominio.Usuario getAdminInicial() { return adminInicial; }
    public void setAdminInicial(py.com.one.security.dominio.Usuario adminInicial) { this.adminInicial = adminInicial; }
    public String getPasswordAdmin() { return passwordAdmin; }
    public void setPasswordAdmin(String passwordAdmin) { this.passwordAdmin = passwordAdmin; }
    public Sucursal getSucursalInicial() { return sucursalInicial; }
    public void setSucursalInicial(Sucursal sucursalInicial) { this.sucursalInicial = sucursalInicial; }
}
