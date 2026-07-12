package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.SaludService;

import java.io.Serializable;

/** REQ-0051 - Backing bean del panel "Salud del sistema" (solo lectura). */
@Named
@ViewScoped
public class SaludBean implements Serializable {

    public static final String PANTALLA = "salud";

    @Inject
    private transient SaludService saludService;

    @Inject
    private SesionUsuario sesion;

    private SaludService.Salud salud;

    @PostConstruct
    public void iniciar() {
        if (sesion.puede(PANTALLA, "VER")) {
            refrescar();
        }
    }

    /** Corta el acceso directo por URL a quien no tiene permiso (via f:viewAction). */
    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public void refrescar() {
        salud = saludService.snapshot();
    }

    public SaludService.Salud getSalud() { return salud; }
}
