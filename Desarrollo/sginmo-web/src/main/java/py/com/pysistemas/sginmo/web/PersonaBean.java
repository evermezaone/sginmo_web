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
import py.com.pysistemas.sginmo.dominio.persona.PersonaEmpresa;
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
    private transient py.com.pysistemas.sginmo.servicio.GeografiaService geografiaService;

    @Inject
    private SesionUsuario sesion;

    @Inject
    private ContextoEmpresa contexto;

    private LazyDataModel<Persona> modelo;
    private Persona seleccionado;
    private PersonaFisica fisica;
    private PersonaJuridica juridica;
    /** Datos comerciales de la persona en el tenant del contexto (V26). */
    private PersonaEmpresa datosEmpresa;
    private String filtroGlobal = "";
    private boolean soloLectura;
    private boolean consultaFiltrada;
    private int tabActivo;

    private List<Entidad> roles;
    private List<Entidad> estadosCiviles;
    private List<Entidad> tiposDocumento;
    private List<Entidad> actividades;
    private List<Entidad> nacionalidades;   // REQ-0043
    private List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> ubicaciones;
    private List<Persona> representantes;
    private List<PersonaRol> rolesPersona = java.util.List.of();
    private String nuevoRol;

    @PostConstruct
    public void iniciar() {
        roles = catalogoService.opciones("ROLES_PERSONA");
        estadosCiviles = catalogoService.opciones("ESTADOS_CIVILES");
        tiposDocumento = catalogoService.opciones("TIPOS_DOCUMENTOS_IDENTIDAD");
        actividades = catalogoService.opciones("ACTIVIDADES_ECONOMICAS");
        nacionalidades = catalogoService.opciones("NACIONALIDADES");   // REQ-0043
        ubicaciones = geografiaService.listar(0, 1000, "", null, true);
        // representante legal: personas fisicas activas
        representantes = personaService.listar(0, 1000, "", null, true).stream()
                .filter(p -> "FISICA".equals(p.getTipoPersoneria()))
                .toList();
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
        datosEmpresa = new PersonaEmpresa();
        rolesPersona = java.util.List.of(); nuevoRol = null;
        soloLectura = false; tabActivo = 0;
    }

    public void nuevoJuridica() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Persona(); seleccionado.setTipoPersoneria("JURIDICA");
        juridica = new PersonaJuridica(); juridica.setPersona(seleccionado);
        fisica = null;
        datosEmpresa = new PersonaEmpresa();
        rolesPersona = java.util.List.of(); nuevoRol = null;
        soloLectura = false; tabActivo = 0;
    }

    /** Tenant del contexto (empresa del usuario logueado); null si aun no cargo. */
    private Long tenantActual() {
        return contexto.getEmpresa() == null ? null : contexto.getEmpresa().getId();
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
        datosEmpresa = personaService.datosEmpresaDe(persona.getId(), tenantActual());
        if (datosEmpresa == null) datosEmpresa = new PersonaEmpresa();
        rolesPersona = personaService.rolesDe(persona.getId());
        nuevoRol = null;
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
        tabActivo = 0;
    }

    /** Metodo (no getter) para no colisionar en EL con getFisica() -> la propiedad "fisica"
     *  debe resolver a la entidad PersonaFisica, no a un boolean. */
    private boolean seleccionadoEsFisica() {
        return seleccionado != null && "FISICA".equals(seleccionado.getTipoPersoneria());
    }

    public void guardar() {
        try {
            boolean esNueva = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNueva ? "CREAR" : "EDITAR")) return;
            Long tenant = tenantActual();
            if (seleccionadoEsFisica()) {
                personaService.guardarFisica(seleccionado, fisica, datosEmpresa, tenant);
            } else {
                personaService.guardarJuridica(seleccionado, juridica, datosEmpresa, tenant);
            }
            reconciliarRoles();
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

    /**
     * REQ-0089: agregar/quitar rol es DIFERIDO — solo modifica la lista en memoria; la persistencia
     * (insertar los nuevos y dar de baja los desmarcados) ocurre al Guardar, via reconciliarRoles.
     * Asi los cambios de rol en la edicion se guardan realmente.
     */
    public void agregarRol() {
        try {
            if (nuevoRol == null || nuevoRol.isBlank()) {
                throw new NegocioException("Elija el rol");
            }
            Long rolId = catalogoService.idOpcion("ROLES_PERSONA", nuevoRol);
            if (rolId == null) throw new NegocioException("El rol '" + nuevoRol + "' no existe en el catalogo");
            if (rolesPersona.stream().anyMatch(r -> rolId.equals(r.getRol()))) {
                throw new NegocioException("La persona ya tiene ese rol");
            }
            var nuevo = new PersonaRol();
            nuevo.setRol(rolId);
            nuevo.setTenant(tenantActual());
            var copia = new java.util.ArrayList<>(rolesPersona);
            copia.add(nuevo);
            rolesPersona = copia;
            nuevoRol = null;
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo agregar el rol", e.getMessage());
        }
    }

    public void quitarRol(PersonaRol rol) {
        if (rol == null) return;
        rolesPersona = rolesPersona.stream()
            .filter(r -> !java.util.Objects.equals(r.getRol(), rol.getRol()))
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    /** REQ-0089: al guardar, reconcilia los roles de la persona con la lista editada (inserta nuevos, baja desmarcados). */
    private void reconciliarRoles() {
        if (seleccionado.getId() == null) return;
        var rolIds = rolesPersona.stream()
            .map(PersonaRol::getRol)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        personaService.reconciliarRoles(seleccionado.getId(), rolIds);
        rolesPersona = personaService.rolesDe(seleccionado.getId());
    }

    public String descripcionRol(Long id) {
        if (id == null) return "";
        return roles.stream().filter(r -> r.getId().equals(id)).map(Entidad::getDescripcion)
                .findFirst().orElse(String.valueOf(id));
    }

    public String getTituloDialogo() {
        if (seleccionado == null) return "Persona";
        return seleccionadoEsFisica() ? "Persona fisica" : "Persona juridica";
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<Persona> getModelo() { return modelo; }
    public Persona getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Persona seleccionado) { this.seleccionado = seleccionado; }
    public PersonaFisica getFisica() { return fisica; }
    public PersonaJuridica getJuridica() { return juridica; }
    public PersonaEmpresa getDatosEmpresa() { return datosEmpresa; }
    public void setDatosEmpresa(PersonaEmpresa datosEmpresa) { this.datosEmpresa = datosEmpresa; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String filtroGlobal) { this.filtroGlobal = filtroGlobal; }
    public boolean isSoloLectura() { return soloLectura; }
    public boolean isConsultaFiltrada() { return consultaFiltrada; }
    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }
    public List<Entidad> getRoles() { return roles; }
    public List<Entidad> getEstadosCiviles() { return estadosCiviles; }
    public List<Entidad> getNacionalidades() { return nacionalidades; }   // REQ-0043
    public List<Entidad> getTiposDocumento() { return tiposDocumento; }
    public List<Entidad> getActividades() { return actividades; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica> getUbicaciones() { return ubicaciones; }
    public List<Persona> getRepresentantes() { return representantes; }
    public List<PersonaRol> getRolesPersona() { return rolesPersona; }
    public String getNuevoRol() { return nuevoRol; }
    public void setNuevoRol(String nuevoRol) { this.nuevoRol = nuevoRol; }
}
