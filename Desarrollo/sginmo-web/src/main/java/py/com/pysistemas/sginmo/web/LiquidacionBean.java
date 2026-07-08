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
import py.com.pysistemas.sginmo.dominio.catalogo.Articulo;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.operacion.Liquidacion;
import py.com.pysistemas.sginmo.dominio.operacion.LiquidacionGasto;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.LiquidacionService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** ABM de liquidaciones (REQ-0025). */
@Named
@ViewScoped
public class LiquidacionBean implements Serializable {

    public static final String PANTALLA = "liquidaciones";

    @Inject
    private transient LiquidacionService service;
    @Inject
    private transient CatalogoService catalogoService;
    @Inject
    private ContextoEmpresa contexto;
    @Inject
    private SesionUsuario sesion;

    private LazyDataModel<Object[]> modelo;
    private Liquidacion seleccionado;
    private String filtroGlobal = "";
    private boolean soloLectura;

    private List<Object[]> operacionesLiquidables;
    private Long operacionSel;
    private List<Articulo> articulos;
    private List<Entidad> motivos;
    private List<LiquidacionGasto> gastos = new ArrayList<>();

    @PostConstruct
    public void iniciar() {
        articulos = catalogoService.articulosActivos();
        motivos = catalogoService.opciones("MOTIVOS_LIQUIDACION");
        operacionesLiquidables = service.operacionesLiquidables(empresaContexto());
        modelo = new LazyDataModel<>() {
            @Override
            public int count(Map<String, FilterMeta> f) { return (int) service.contar(empresaContexto(), filtroGlobal); }
            @Override
            public List<Object[]> load(int first, int size, Map<String, SortMeta> s, Map<String, FilterMeta> f) {
                return service.listar(empresaContexto(), first, size, filtroGlobal);
            }
        };
    }

    /** Empresa del contexto (obs 233): la pantalla solo opera sobre sus liquidaciones/operaciones. */
    private Long empresaContexto() {
        return contexto.getEmpresa() == null ? null : contexto.getEmpresa().getId();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void nuevo() {
        if (!sesion.puede(PANTALLA, "CREAR")) return;
        seleccionado = new Liquidacion();
        operacionSel = null;
        gastos = new ArrayList<>();
        operacionesLiquidables = service.operacionesLiquidables(empresaContexto());
        soloLectura = false;
    }

    public void operacionElegida() {
        if (operacionSel != null) {
            seleccionado.setOperacion(operacionSel);
            seleccionado.setTotalGarantia(service.garantiaDe(operacionSel, empresaContexto()));
            // Plantilla de gastos (obs 231, RN-PLANT-001/002): alquileres pendientes y
            // mora calculados desde las cuotas de la operacion; el usuario ajusta/agrega.
            gastos = new ArrayList<>(service.plantillaDe(operacionSel, empresaContexto()));
        }
    }

    public void editar(Object[] fila) {
        seleccionado = (Liquidacion) fila[0];
        operacionSel = seleccionado.getOperacion();
        gastos = new ArrayList<>();
        for (Object[] g : service.gastosDe(seleccionado.getId())) {
            var lg = new LiquidacionGasto();
            lg.setId(((Number) g[0]).longValue());
            lg.setArticulo(((Number) g[1]).longValue());
            lg.setConcepto((String) g[2]);
            lg.setMonto((BigDecimal) g[3]);
            gastos.add(lg);
        }
        soloLectura = !sesion.puede(PANTALLA, "EDITAR");
    }

    public void agregarGasto() {
        gastos.add(new LiquidacionGasto());
    }

    public void quitarGasto(LiquidacionGasto g) {
        gastos.remove(g);
    }

    public BigDecimal getTotalGastos() {
        return gastos.stream().map(g -> g.getMonto() == null ? BigDecimal.ZERO : g.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getSaldoCalculado() {
        BigDecimal gar = seleccionado == null || seleccionado.getTotalGarantia() == null
                ? BigDecimal.ZERO : seleccionado.getTotalGarantia();
        return gar.subtract(getTotalGastos());
    }

    public void guardar() {
        try {
            boolean esNueva = seleccionado.getId() == null;
            if (soloLectura || !sesion.puede(PANTALLA, esNueva ? "CREAR" : "EDITAR")) return;
            service.guardar(seleccionado, gastos, empresaContexto());
            aviso(FacesMessage.SEVERITY_INFO, esNueva ? "Liquidación registrada" : "Liquidación actualizada",
                    "Saldo: " + getSaldoCalculado());
            org.primefaces.PrimeFaces.current().executeScript("PF('dlgLiq').hide()");
            org.primefaces.PrimeFaces.current().ajax().update("frmLista:tabla", "frmLista:mensajes");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public LazyDataModel<Object[]> getModelo() { return modelo; }
    public Liquidacion getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Liquidacion v) { this.seleccionado = v; }
    public String getFiltroGlobal() { return filtroGlobal; }
    public void setFiltroGlobal(String v) { this.filtroGlobal = v; }
    public boolean isSoloLectura() { return soloLectura; }
    public List<Object[]> getOperacionesLiquidables() { return operacionesLiquidables; }
    public Long getOperacionSel() { return operacionSel; }
    public void setOperacionSel(Long v) { this.operacionSel = v; }
    public List<Articulo> getArticulos() { return articulos; }
    public List<Entidad> getMotivos() { return motivos; }
    public List<LiquidacionGasto> getGastos() { return gastos; }
}
