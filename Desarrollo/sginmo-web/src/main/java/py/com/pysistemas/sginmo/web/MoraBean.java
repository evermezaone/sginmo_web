package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.dominio.cobranza.GestionCobranza;
import py.com.pysistemas.sginmo.dominio.cobranza.PromesaPago;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.MoraService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** REQ-0057 - Cartera vencida + gestiones de cobranza + promesas de pago. */
@Named
@ViewScoped
public class MoraBean implements Serializable {

    public static final String PANTALLA = "cobranza";

    @Inject
    private transient MoraService servicio;
    @Inject
    private transient CatalogoService catalogoService;
    @Inject
    private SesionUsuario sesion;

    private Integer diasMin;
    private BigDecimal montoMin;
    private Long monedaId;
    private Long clienteId;
    private Long operacionId;

    private List<MoraService.FilaCartera> cartera;
    private MoraService.FilaCartera seleccionada;
    private GestionCobranza gestion;
    private PromesaPago promesa;
    private List<GestionCobranza> gestiones;
    private List<PromesaPago> promesas;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> monedas;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        monedas = catalogoService.monedasActivas();
        buscar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void buscar() {
        cartera = servicio.carteraVencida(diasMin, montoMin, monedaId, clienteId, operacionId);
    }

    public void gestionar(MoraService.FilaCartera fila) {
        seleccionada = fila;
        gestiones = servicio.gestionesDe(fila.getOperacion());
        promesas = servicio.promesasDe(fila.getOperacion());
        gestion = new GestionCobranza();
        gestion.setOperacion(fila.getOperacion());
        gestion.setCronogramaCuota(fila.getCronogramaCuota());
        gestion.setCliente(fila.getCliente());
        gestion.setFecha(LocalDate.now());
        gestion.setResultado("CONTACTADO");
        promesa = new PromesaPago();
        promesa.setOperacion(fila.getOperacion());
        promesa.setCronogramaCuota(fila.getCronogramaCuota());
        promesa.setCliente(fila.getCliente());
        promesa.setMoneda(fila.getMoneda());
        promesa.setFechaPromesa(LocalDate.now().plusDays(7));
        promesa.setMonto(fila.getSaldo());
        promesa.setEstado("PENDIENTE");
    }

    public void guardarGestion() {
        try {
            servicio.registrarGestion(gestion);
            gestiones = servicio.gestionesDe(seleccionada.getOperacion());
            gestion = nuevaGestion();
            aviso(FacesMessage.SEVERITY_INFO, "Gestion registrada", "");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo registrar", e.getMessage());
        }
    }

    public void guardarPromesa() {
        try {
            servicio.registrarPromesa(promesa);
            promesas = servicio.promesasDe(seleccionada.getOperacion());
            aviso(FacesMessage.SEVERITY_INFO, "Promesa registrada", "");
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo registrar la promesa", e.getMessage());
        }
    }

    public void cambiarEstadoPromesa(PromesaPago p, String estado) {
        try {
            servicio.cambiarEstadoPromesa(p.getId(), estado);
            promesas = servicio.promesasDe(seleccionada.getOperacion());
            aviso(FacesMessage.SEVERITY_INFO, "Promesa actualizada", estado);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo actualizar", e.getMessage());
        }
    }

    private GestionCobranza nuevaGestion() {
        GestionCobranza g = new GestionCobranza();
        g.setOperacion(seleccionada.getOperacion());
        g.setCronogramaCuota(seleccionada.getCronogramaCuota());
        g.setCliente(seleccionada.getCliente());
        g.setFecha(LocalDate.now());
        g.setResultado("CONTACTADO");
        return g;
    }

    public List<String> getResultados() {
        return List.of("CONTACTADO", "NO_CONTACTADO", "COMPROMISO", "RECHAZO", "ILOCALIZABLE", "OTRO");
    }
    public List<String> getEstadosPromesa() { return List.of("PENDIENTE", "CUMPLIDA", "INCUMPLIDA"); }

    private void aviso(FacesMessage.Severity sev, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, t, d));
    }

    public Integer getDiasMin() { return diasMin; }
    public void setDiasMin(Integer v) { this.diasMin = v; }
    public BigDecimal getMontoMin() { return montoMin; }
    public void setMontoMin(BigDecimal v) { this.montoMin = v; }
    public Long getMonedaId() { return monedaId; }
    public void setMonedaId(Long v) { this.monedaId = v; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long v) { this.clienteId = v; }
    public Long getOperacionId() { return operacionId; }
    public void setOperacionId(Long v) { this.operacionId = v; }
    public List<MoraService.FilaCartera> getCartera() { return cartera; }
    public MoraService.FilaCartera getSeleccionada() { return seleccionada; }
    public GestionCobranza getGestion() { return gestion; }
    public void setGestion(GestionCobranza g) { this.gestion = g; }
    public PromesaPago getPromesa() { return promesa; }
    public void setPromesa(PromesaPago p) { this.promesa = p; }
    public List<GestionCobranza> getGestiones() { return gestiones; }
    public List<PromesaPago> getPromesas() { return promesas; }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> getMonedas() { return monedas; }
}
