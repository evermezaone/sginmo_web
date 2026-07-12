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
import py.com.pysistemas.sginmo.dominio.agenda.AgendaEvento;
import py.com.pysistemas.sginmo.servicio.AgendaService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** REQ-0052 - Agenda operativa: tareas + vencimientos. Patron ABM lazy estandar. */
@Named
@ViewScoped
public class AgendaBean implements Serializable {

    public static final String PANTALLA = "agenda";

    @Inject
    private transient AgendaService agendaService;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<AgendaEvento> modelo;
    private AgendaEvento seleccionado;
    private String filtroGlobal = "";
    private String filtroTipo;
    private String filtroEstado;
    private String filtroResponsable;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        // Genera/actualiza los vencimientos automaticos del tenant (idempotente por dedup).
        try { agendaService.generarAutomaticos(); } catch (RuntimeException ignore) { /* no bloquea la vista */ }
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return (int) agendaService.contar(filtroGlobal, filtroTipo, filtroEstado, filtroResponsable);
            }
            @Override
            public List<AgendaEvento> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                String orden = null; boolean asc = true;
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sm = sortBy.values().iterator().next();
                    orden = sm.getField(); asc = sm.getOrder() != org.primefaces.model.SortOrder.DESCENDING;
                }
                return agendaService.listar(first, pageSize, filtroGlobal, filtroTipo, filtroEstado,
                        filtroResponsable, orden, asc);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void buscar() { /* el LazyDataModel recarga solo al actualizar la tabla */ }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new AgendaEvento();
        seleccionado.setTipo("TAREA");
        seleccionado.setEstado("PENDIENTE");
        seleccionado.setPrioridad("MEDIA");
        seleccionado.setFechaEvento(java.time.LocalDate.now());
        seleccionado.setResponsable(sesion.codigoUsuario());
    }

    public void editar(AgendaEvento e) { seleccionado = e; }

    public void guardar() {
        try {
            boolean nuevo = seleccionado.getId() == null;
            agendaService.guardar(seleccionado);
            aviso(FacesMessage.SEVERITY_INFO, nuevo ? "Tarea creada" : "Tarea actualizada", seleccionado.getTitulo());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgAgenda').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void cerrar(AgendaEvento e) {
        try {
            agendaService.cambiarEstado(e.getId(), "CERRADO");
            aviso(FacesMessage.SEVERITY_INFO, "Evento cerrado", e.getTitulo());
        } catch (NegocioException ex) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cerrar", ex.getMessage());
        }
    }

    public List<String> getTipos() { return List.of("RECORDATORIO", "TAREA", "VENCIMIENTO", "PROMESA"); }
    public List<String> getEstados() { return List.of("PENDIENTE", "EN_CURSO", "RESUELTO", "CERRADO"); }
    public List<String> getPrioridades() { return List.of("BAJA", "MEDIA", "ALTA"); }

    private void aviso(FacesMessage.Severity sev, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, titulo, detalle));
    }

    public LazyDataModel<AgendaEvento> getModelo() { return modelo; }
    public AgendaEvento getSeleccionado() { return seleccionado; }
    public void setSeleccionado(AgendaEvento seleccionado) { this.seleccionado = seleccionado; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String v) { this.filtroGlobal = v; }
    public String getFiltroTipo() { return filtroTipo; }
    public void setFiltroTipo(String v) { this.filtroTipo = v; }
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String v) { this.filtroEstado = v; }
    public String getFiltroResponsable() { return filtroResponsable; }
    public void setFiltroResponsable(String v) { this.filtroResponsable = v; }
}
