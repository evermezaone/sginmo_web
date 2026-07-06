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
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.dominio.persona.PersonaFisica;
import py.com.pysistemas.sginmo.dominio.persona.PersonaJuridica;
import py.com.pysistemas.sginmo.dominio.persona.PersonaRol;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.PersonaService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** ABM de personas / socios de negocios (REQ-0012), patron estandar. */
@Named
@ViewScoped
public class PersonaBean implements Serializable {

    public static final String PANTALLA = "personas";

    @Inject
    private transient PersonaService personaService;

    @Inject
    private transient CatalogoService catalogoService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Persona> modelo;
    private Persona seleccionado;
    private PersonaFisica fisica;
    private PersonaJuridica juridica;
    private String filtroGlobal = "";
    private boolean soloLectura;
    private boolean consultaFiltrada;
    private int tabActivo;

    private List<Entidad> roles;
    private List<Entidad> estadosCiviles;
    private List<PersonaRol> rolesPersona = java.util.List.of();
    private String nuevoRol;

    @PostConstruct
    public void iniciar() {
        roles = catalogoService.opciones("ROLES_PERSONA");
        estadosCiviles = catalogoService.opciones("ESTADOS_CIVILES");
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) personaService.contar(filtroGlobal);
            }

            @Override
            public List<Persona> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                consultaFiltrada = filtroGlobal != null && !filtroGlobal.isBlank();
                String orden = null; boolean asc = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    orden = sm.getField(); asc = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return personaService.listar(first, pageSize, filtroGlobal, orden, asc);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevoFisica() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Persona(); seleccionado.setTipoPersoneria("FISICA");
        fisica = new PersonaFisica(); fisica.setPersona(seleccionado);
        juridica = null;
        rolesPersona = java.util.List.of(); nuevoRol = null;
        soloLectura = false; tabActivo = 0;
    }

    public void nuevoJuridica() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Persona(); seleccionado.setTipoPersoneria("JURIDICA");
        juridica = new PersonaJuridica(); juridica.setPersona(seleccionado);
        fisica = null;
        rolesPersona = java.util.List.of(); nuevoRol = null;
        soloLectura = false; tabActivo = 0;
    }

    public void editar(Persona persona) {
        seleccionado = persona;
        if ("FISICA".equals(persona.getTipoPersoneria())) {
            fisica = personaService.fisicaDe(persona.getId()); juridica = null;
            if (fisica == null) { fisica = new PersonaFisica(); fisica.setPersona(persona); }
        } else {
            juridica = personaService.juridicaDe(persona.getId()); fisica = null;
            if (juridica == null) { juridica = new PersonaJuridica(); juridica.setPersona(persona); }
        }
        rolesPersona = personaService.rolesDe(persona.getId());
        nuevoRol = null;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
        tabActivo = 0;
    }

    public boolean isFisica() { return seleccionado != null && "FISICA".equals(seleccionado.getTipoPersoneria()); }

    public void guardar() {
        try {
            boolean esNueva = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNueva ? "CREAR" : "EDITAR")) return;
            if (isFisica()) {
                personaService.guardarFisica(seleccionado, fisica);
            } else {
                personaService.guardarJuridica(seleccionado, juridica);
            }
            aviso(FacesMessage.SEVERITY_INFO, esNueva ? "Persona creada" : "Persona actualizada", seleccionado.getNombre());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgPersona').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cambiarEstado(Persona persona) {
        try {
            String nuevo = "ACTIVO".equals(persona.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (!sesion.puede(PANTALLA, "ACTIVO".equals(nuevo) ? "REACTIVAR" : "INACTIVAR")) return;
            personaService.cambiarEstado(persona.getId(), nuevo);
            aviso(FacesMessage.SEVERITY_INFO, "ACTIVO".equals(nuevo) ? "Persona activada" : "Persona inactivada", persona.getNombre());
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    public void agregarRol() {
        try {
            personaService.agregarRol(seleccionado.getId(), nuevoRol);
            rolesPersona = personaService.rolesDe(seleccionado.getId());
            nuevoRol = null;
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar el rol", e.getMessage());
        }
    }

    public void quitarRol(Long personaRolId) {
        personaService.quitarRol(personaRolId);
        rolesPersona = personaService.rolesDe(seleccionado.getId());
    }

    public String descripcionRol(String codigo) {
        return roles.stream().filter(r -> r.getCodigo().equals(codigo)).map(Entidad::getDescripcion).findFirst().orElse(codigo);
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<Persona> getModelo() { return modelo; }
    public Persona getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Persona seleccionado) { this.seleccionado = seleccionado; }
    public PersonaFisica getFisica() { return fisica; }
    public PersonaJuridica getJuridica() { return juridica; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public boolean isConsultaFiltrada() { return consultaFiltrada; }
    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }
    public List<Entidad> getRoles() { return roles; }
    public List<Entidad> getEstadosCiviles() { return estadosCiviles; }
    public List<PersonaRol> getRolesPersona() { return rolesPersona; }
    public String getNuevoRol() { return nuevoRol; }
    public void setNuevoRol(String nuevoRol) { this.nuevoRol = nuevoRol; }
}
