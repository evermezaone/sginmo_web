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
import py.com.pysistemas.sginmo.dominio.operacion.CronogramaCuota;
import py.com.pysistemas.sginmo.dominio.operacion.Operacion;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.servicio.OperacionService;
import py.com.pysistemas.sginmo.servicio.PersonaService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Operaciones de alquiler/venta (REQ-0016..0021). El cuadre lo hace la BD. */
@Named
@ViewScoped
public class OperacionBean implements Serializable {

    public static final String PANTALLA = "operaciones";

    @Inject
    private transient OperacionService operacionService;

    @Inject
    private transient PersonaService personaService;

    @Inject
    private transient py.com.pysistemas.sginmo.servicio.CatalogoService catalogoService;

    @Inject
    private ContextoEmpresa contexto;

    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Object[]> modelo;
    private Operacion seleccionado;
    private String filtroGlobal = "";
    private boolean consultaFiltrada;
    private List<CronogramaCuota> cuotas = java.util.List.of();
    private List<CronogramaCuota> cuotasSeleccionadas = new java.util.ArrayList<>();

    private List<Persona> clientes;
    private List<Persona> vendedores;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> tiposContrato;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> tiposFinanciacion;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> monedas;   // REQ-0046

    // renovacion / rescision / regeneracion
    private int renovMeses = 12;
    private BigDecimal renovPrecio;
    private String rescisionMotivo;
    private int regenCuotas = 12;
    private java.time.LocalDate regenDesde = java.time.LocalDate.now().plusMonths(1);

    @PostConstruct
    public void iniciar() {
        clientes = personaService.porRol("CLIENTE");
        vendedores = personaService.porRol("VENDEDOR");
        tiposContrato = catalogoService.opciones("TIPOS_CONTRATOS");
        tiposFinanciacion = catalogoService.opciones("TIPOS_FINANCIACIONES");
        monedas = catalogoService.monedasActivas();   // REQ-0046
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> f) { return (int) operacionService.contar(filtroGlobal); }
            @Override
            public List<Object[]> load(int first, int size, Map<String, SortMeta> s, Map<String, FilterMeta> f) {
                consultaFiltrada = filtroGlobal != null && !filtroGlobal.isBlank();
                return operacionService.listar(first, size, filtroGlobal);
            }
        };
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Operacion();
        if (contexto.getEmpresa() != null) seleccionado.setTenant(contexto.getEmpresa().getId());
        if (contexto.sucursal() != null) seleccionado.setSucursal(contexto.sucursal().getId());
        // Moneda obligatoria (REQ-0046): por defecto Guaranies si existe, si no la primera visible.
        if (monedas != null && !monedas.isEmpty()) {
            seleccionado.setMoneda(monedas.stream()
                .filter(m -> m.getDescripcion() != null && m.getDescripcion().toLowerCase().contains("guaran"))
                .findFirst().orElse(monedas.get(0)).getId());
        }
        cuotas = java.util.List.of();
        cuotasSeleccionadas = new java.util.ArrayList<>();
    }

    public void ver(Long operacionId) {
        // Resolver por id directo: no depende de las primeras 1000 filas de la grilla lazy.
        seleccionado = operacionService.porId(operacionId);
        cuotas = operacionService.cuotasDe(operacionId);
        cuotasSeleccionadas = new java.util.ArrayList<>();
    }

    public List<Activo> completarActivo(String t) { return operacionService.activosLibres(t); }

    public void crear() {
        try {
            if (!sesion.puede(PANTALLA, "CREAR")) return;
            operacionService.crear(seleccionado);
            aviso(FacesMessage.SEVERITY_INFO, "Operación registrada",
                    "Se generó el cronograma y los movimientos automáticos");
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgOperacion').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo registrar", e.getMessage());
        }
    }

    public void renovar() {
        try {
            operacionService.renovar(seleccionado.getId(), renovMeses, renovPrecio);
            cuotas = operacionService.cuotasDe(seleccionado.getId());
            aviso(FacesMessage.SEVERITY_INFO, "Operación renovada", renovMeses + " cuota(s) agregada(s)");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo renovar", e.getMessage());
        }
    }

    public void regenerar() {
        try {
            operacionService.regenerarCuotas(seleccionado.getId(), regenCuotas, regenDesde);
            cuotas = operacionService.cuotasDe(seleccionado.getId());
            aviso(FacesMessage.SEVERITY_INFO, "Cronograma regenerado", regenCuotas + " cuotas");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo regenerar", e.getMessage());
        }
    }

    public void finalizar() {
        try {
            operacionService.finalizar(seleccionado.getId(), rescisionMotivo);
            aviso(FacesMessage.SEVERITY_INFO, "Operación finalizada", "El activo vuelve a estar disponible");
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgDetalle').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo finalizar", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public List<Long> idsCuotasSeleccionadas() {
        if (cuotasSeleccionadas == null || cuotasSeleccionadas.isEmpty()) {
            return java.util.List.of();
        }
        return cuotasSeleccionadas.stream().map(CronogramaCuota::getId).toList();
    }

    public LazyDataModel<Object[]> getModelo() { return modelo; }
    public Operacion getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Operacion v) { this.seleccionado = v; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String v) { this.filtroGlobal = v; }
    public boolean isConsultaFiltrada() { return consultaFiltrada; }
    public List<CronogramaCuota> getCuotas() { return cuotas; }
    public List<CronogramaCuota> getCuotasSeleccionadas() { return cuotasSeleccionadas; }
    public void setCuotasSeleccionadas(List<CronogramaCuota> v) { this.cuotasSeleccionadas = v; }
    public List<Persona> getClientes() { return clientes; }
    public List<Persona> getVendedores() { return vendedores; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> getTiposContrato() { return tiposContrato; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> getMonedas() { return monedas; }   // REQ-0046
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Entidad> getTiposFinanciacion() { return tiposFinanciacion; }
    public int getRenovMeses() { return renovMeses; }
    public void setRenovMeses(int v) { this.renovMeses = v; }
    public BigDecimal getRenovPrecio() { return renovPrecio; }
    public void setRenovPrecio(BigDecimal v) { this.renovPrecio = v; }
    public String getRescisionMotivo() { return rescisionMotivo; }
    public void setRescisionMotivo(String v) { this.rescisionMotivo = v; }
    public int getRegenCuotas() { return regenCuotas; }
    public void setRegenCuotas(int v) { this.regenCuotas = v; }
    public java.time.LocalDate getRegenDesde() { return regenDesde; }
    public void setRegenDesde(java.time.LocalDate v) { this.regenDesde = v; }
}
