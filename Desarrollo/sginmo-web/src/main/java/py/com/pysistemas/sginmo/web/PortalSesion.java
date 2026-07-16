package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import py.com.pysistemas.sginmo.servicio.PortalAuthService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0078 - Sesion del portal externo de socios. Identidad basada en persona + tenant + roles
 * comerciales, NO en el usuario administrativo ni en usuario.perfil='PORTAL'. Se crea recien
 * despues de validar OTP/password y es totalmente independiente de {@code SesionUsuario} (login
 * de empleados): timeout y logout propios.
 *
 * Guarda ademas una identidad "pendiente" (post-OTP, antes de definir/confirmar la clave) que
 * habilita solo la pantalla de definicion de clave; nunca da acceso a los datos.
 */
@Named
@SessionScoped
public class PortalSesion implements Serializable {

    private Long tenant;
    private Long persona;
    private String documento;
    private String nombre;
    private boolean esCliente;
    private boolean esPropietario;
    private boolean autenticado;

    /** Identidad validada por OTP que aun debe definir su clave (primer ingreso / recuperacion). */
    private Long pendientePersona;
    private Long pendienteTenant;
    private String pendienteDocumento;
    private String pendienteNombre;

    /** Solicitud de OTP en curso (solo documento; REQ-0102 ya no expone/necesita la empresa). */
    private String solicitudDocumento;

    /** REQ-0102: empresas accesibles del socio en esta sesion (para el selector si son mas de una). */
    private List<PortalAuthService.Acceso> accesos = new ArrayList<>();

    public void iniciarSolicitud(String documento) {
        this.solicitudDocumento = documento;
    }

    public boolean tieneSolicitud() { return solicitudDocumento != null && !solicitudDocumento.isBlank(); }
    public String getSolicitudDocumento() { return solicitudDocumento; }

    /** Acceso pleno tras login por clave, o tras OTP+definicion de clave. */
    public void autenticar(PortalAuthService.Identidad id) {
        this.tenant = id.tenant;
        this.persona = id.persona;
        this.documento = id.documento;
        this.nombre = id.nombre;
        this.esCliente = id.esCliente;
        this.esPropietario = id.esPropietario;
        this.autenticado = true;
        limpiarPendiente();
    }

    /** REQ-0102: autentica con la lista de empresas accesibles; activa la primera. */
    public void autenticarMulti(List<PortalAuthService.Acceso> accs) {
        this.accesos = (accs == null) ? new ArrayList<>() : new ArrayList<>(accs);
        if (!this.accesos.isEmpty()) autenticar(this.accesos.get(0).identidad);
    }

    /** REQ-0102: cambia la empresa activa (solo entre las accesibles ya autenticadas por clave). */
    public void cambiarEmpresa(Long tenantDestino) {
        if (tenantDestino == null) return;
        for (PortalAuthService.Acceso a : accesos) {
            if (tenantDestino.equals(a.tenant)) { autenticar(a.identidad); return; }
        }
    }

    public List<PortalAuthService.Acceso> getAccesos() { return accesos; }
    public boolean isMultiEmpresa() { return accesos != null && accesos.size() > 1; }
    public String getEmpresaActual() {
        if (accesos != null) for (PortalAuthService.Acceso a : accesos) if (a.tenant != null && a.tenant.equals(tenant)) return a.empresa;
        return null;
    }

    /** Registra la identidad validada por OTP que todavia debe fijar su clave. */
    public void marcarPendiente(PortalAuthService.Identidad id) {
        this.pendientePersona = id.persona;
        this.pendienteTenant = id.tenant;
        this.pendienteDocumento = id.documento;
        this.pendienteNombre = id.nombre;
    }

    public boolean tienePendiente() { return pendientePersona != null; }

    public void limpiarPendiente() {
        pendientePersona = null; pendienteTenant = null; pendienteDocumento = null; pendienteNombre = null;
    }

    public void cerrar() {
        tenant = null; persona = null; documento = null; nombre = null;
        esCliente = false; esPropietario = false; autenticado = false;
        solicitudDocumento = null;
        if (accesos != null) accesos.clear();
        limpiarPendiente();
    }

    public boolean isAutenticado() { return autenticado && persona != null && tenant != null; }

    public Long getTenant() { return tenant; }
    public Long getPersona() { return persona; }
    public String getDocumento() { return documento; }
    public String getNombre() { return nombre; }
    public boolean isEsCliente() { return esCliente; }
    public boolean isEsPropietario() { return esPropietario; }

    public Long getPendientePersona() { return pendientePersona; }
    public Long getPendienteTenant() { return pendienteTenant; }
    public String getPendienteDocumento() { return pendienteDocumento; }
    public String getPendienteNombre() { return pendienteNombre; }
}
