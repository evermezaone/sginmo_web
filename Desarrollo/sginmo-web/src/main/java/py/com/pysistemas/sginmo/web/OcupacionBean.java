package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.OcupacionService;

import java.io.Serializable;
import java.util.List;

/** REQ-0072 - Ocupacion/vacancia con brecha al objetivo y lista clicable de propiedades vacantes. */
@Named
@ViewScoped
public class OcupacionBean implements Serializable {

    public static final String PANTALLA = "ocupacion";
    private static final int LIMITE_VACANTES = 300;

    @Inject
    private transient OcupacionService servicio;
    @Inject
    private SesionUsuario sesion;

    private OcupacionService.Resumen resumen;
    private List<OcupacionService.Vacante> vacantes;
    private List<OcupacionService.PorGrupo> porTipo;

    @PostConstruct
    public void iniciar() {
        if (!sesion.puede(PANTALLA, "VER")) return;
        recargar();
    }

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void recargar() {
        resumen = servicio.resumen();
        vacantes = servicio.vacantes(LIMITE_VACANTES);
        porTipo = servicio.porTipo();
    }

    /** Marca si una fila vacante esta dentro de la brecha (las primeras N que faltan alquilar). */
    public boolean enBrecha(int indexFila) {
        return resumen != null && indexFila < resumen.getBrecha();
    }

    public OcupacionService.Resumen getResumen() { return resumen; }
    public List<OcupacionService.Vacante> getVacantes() { return vacantes; }
    public List<OcupacionService.PorGrupo> getPorTipo() { return porTipo; }
}
