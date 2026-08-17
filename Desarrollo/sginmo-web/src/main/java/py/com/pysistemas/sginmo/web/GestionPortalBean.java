package py.com.pysistemas.sginmo.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;

import java.io.Serializable;

/**
 * REQ-0112 - "Gestion del portal": hub del back-office del portal (reclamos + conciliaciones).
 * Lo usa un usuario del login de sginmo-web con permiso, NO el login del cliente.
 */
@Named
@ViewScoped
public class GestionPortalBean implements Serializable {

    @Inject
    private SesionUsuario sesion;

    public static final String PANTALLA = "gestion_portal";

    public String verificarAcceso() {
        return sesion.puede(PANTALLA, "VER") ? null : "/index?faces-redirect=true";
    }

    public boolean isPuedeReclamos() { return sesion.puede("reclamos", "VER"); }
    public boolean isPuedeTransferencias() { return sesion.puede("transferencias", "VER"); }
}
