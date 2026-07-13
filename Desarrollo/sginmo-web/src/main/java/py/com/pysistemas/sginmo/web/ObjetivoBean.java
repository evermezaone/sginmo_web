package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.CatalogoService;
import py.com.pysistemas.sginmo.servicio.ObjetivoService;

import java.io.Serializable;
import java.util.List;

/** REQ-0073 - ABM de objetivos gerenciales con calculo automatico y semaforo. */
@Named
@ViewScoped
public class ObjetivoBean implements Serializable {

    public static final String PANTALLA = "objetivos";

    @Inject
    private transient ObjetivoService servicio;
    @Inject
    private transient CatalogoService catalogoService;
    @Inject
    private SesionUsuario sesion;

    private List<ObjetivoService.Objetivo> objetivos;
    private ObjetivoService.Objetivo edicion;
    private List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> monedas;
    private List<ObjetivoService.Suc> sucursales;                 // obs 284
    private List<ObjetivoService.Medicion> historial;             // obs 285
    private ObjetivoService.Objetivo historialDe;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        monedas = catalogoService.monedasActivas();
        sucursales = servicio.sucursales();
        recargar();
    }

    public void verHistorial(ObjetivoService.Objetivo o) {
        historialDe = o;
        historial = servicio.mediciones(o.getId());
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void recargar() { objetivos = servicio.listar(false); }

    public void nuevo() {
        edicion = new ObjetivoService.Objetivo();
        edicion.setUnidad("PORCENTAJE");
        edicion.setSentido("MINIMO");
        edicion.setPeriodo("MENSUAL");
        edicion.setAlcance("EMPRESA");
    }

    public void editar(ObjetivoService.Objetivo o) { edicion = servicio.porId(o.getId()); }

    public void guardar() {
        try {
            servicio.guardar(edicion);
            aviso(FacesMessage.SEVERITY_INFO, "Objetivo guardado", null);
            edicion = null;
            recargar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo guardar", e.getMessage());
        }
    }

    public void inactivar(ObjetivoService.Objetivo o) { cambiar(o, "INACTIVO"); }
    public void reactivar(ObjetivoService.Objetivo o) { cambiar(o, "ACTIVO"); }

    private void cambiar(ObjetivoService.Objetivo o, String estado) {
        try {
            servicio.cambiarEstado(o.getId(), estado);
            recargar();
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo cambiar el estado", e.getMessage());
        }
    }

    public void medir(ObjetivoService.Objetivo o) {
        try {
            servicio.registrarMedicion(o.getId());
            aviso(FacesMessage.SEVERITY_INFO, "Medicion registrada", null);
        } catch (NegocioException e) {
            aviso(FacesMessage.SEVERITY_WARN, "No se pudo registrar la medicion", e.getMessage());
        }
    }

    private void aviso(FacesMessage.Severity s, String t, String d) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, t, d));
    }

    public List<ObjetivoService.Objetivo> getObjetivos() { return objetivos; }
    public ObjetivoService.Objetivo getEdicion() { return edicion; }
    public boolean isEditando() { return edicion != null; }
    public List<String> getIndicadores() { return ObjetivoService.indicadores(); }
    public List<String> getUnidades() { return List.of("PORCENTAJE", "MONTO", "CANTIDAD"); }
    public List<String> getSentidos() { return List.of("MINIMO", "MAXIMO"); }
    public List<String> getPeriodos() { return List.of("MENSUAL", "TRIMESTRAL", "ANUAL", "PERSONALIZADO"); }
    public List<String> getAlcances() { return ObjetivoService.alcances(); }
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Moneda> getMonedas() { return monedas; }
    public List<ObjetivoService.Suc> getSucursales() { return sucursales; }
    public List<ObjetivoService.Medicion> getHistorial() { return historial; }
    public ObjetivoService.Objetivo getHistorialDe() { return historialDe; }
}
