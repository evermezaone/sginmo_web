package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.servicio.PortalAuthService;

import java.io.Serializable;
import java.util.List;

/**
 * REQ-0078 - Flujo de acceso publico del portal externo: login por CI/RUC + password, y primer
 * ingreso / recuperacion por OTP con definicion de clave. No depende del login administrativo.
 */
@Named
@ViewScoped
public class PortalLoginBean implements Serializable {

    @Inject
    private transient PortalAuthService auth;

    @Inject
    private PortalSesion sesion;

    private List<PortalAuthService.Empresa> empresas;
    private Long tenant;
    private String documento;
    private String password;
    private String codigo;
    private String nueva;
    private String repetir;
    private String canalInfo;   // texto informativo tras enviar OTP

    @PostConstruct
    public void iniciar() {
        try { empresas = auth.empresas(); } catch (RuntimeException e) { empresas = List.of(); }
    }

    /** Si ya hay sesion de portal, saltar directo a la cuenta. */
    public String verificarLogin() {
        return sesion.isAutenticado() ? "/portal/inicio?faces-redirect=true" : null;
    }

    /** Entrada del portal por la URL de carpeta (/portal/): manda a la cuenta o al login. */
    public String entrada() {
        return sesion.isAutenticado() ? "/portal/inicio?faces-redirect=true" : "/portal/login?faces-redirect=true";
    }

    /** La pagina de clave requiere una identidad validada por OTP pendiente de definir password. */
    public String verificarClave() {
        if (sesion.isAutenticado()) return "/portal/inicio?faces-redirect=true";
        return sesion.tienePendiente() ? null : "/portal/login?faces-redirect=true";
    }

    /** La pagina de OTP requiere una solicitud en curso. */
    public String verificarOtp() {
        if (sesion.isAutenticado()) return "/portal/inicio?faces-redirect=true";
        return sesion.tieneSolicitud() ? null : "/portal/login?faces-redirect=true";
    }

    // ── acciones ──────────────────────────────────────────────────────────────

    public String ingresar() {
        try {
            PortalAuthService.Identidad id = auth.loginPassword(tenant, documento, password, ip(), ua());
            sesion.autenticar(id);
            return "/portal/inicio?faces-redirect=true";
        } catch (NegocioException e) {
            aviso(e.getMessage());
            return null;
        }
    }

    /** Primer ingreso / olvide mi clave: envia OTP y navega a la pantalla de codigo. */
    public String enviarCodigo() {
        if (tenant == null || documento == null || documento.isBlank()) {
            aviso("Seleccione la empresa e ingrese su documento.");
            return null;
        }
        try {
            auth.solicitarOtp(tenant, documento.trim(), "LOGIN", ip(), ua());
        } catch (NegocioException e) {
            // Aun ante error de negocio mostramos el mensaje generico y avanzamos igual (no revela).
        }
        sesion.iniciarSolicitud(tenant, documento.trim());
        return "/portal/otp?faces-redirect=true";
    }

    public String reenviar() {
        if (!sesion.tieneSolicitud()) return "/portal/login?faces-redirect=true";
        try { auth.solicitarOtp(sesion.getSolicitudTenant(), sesion.getSolicitudDocumento(), "LOGIN", ip(), ua()); }
        catch (NegocioException ignore) { }
        aviso("Si el documento corresponde a un socio, enviamos un nuevo codigo.");
        return null;
    }

    public String validar() {
        if (!sesion.tieneSolicitud()) return "/portal/login?faces-redirect=true";
        try {
            PortalAuthService.Identidad id = auth.validarOtp(
                sesion.getSolicitudTenant(), sesion.getSolicitudDocumento(), codigo, ip(), ua());
            sesion.marcarPendiente(id);
            return "/portal/clave?faces-redirect=true";
        } catch (NegocioException e) {
            aviso(e.getMessage());
            return null;
        }
    }

    public String guardarClave() {
        if (!sesion.tienePendiente()) return "/portal/login?faces-redirect=true";
        if (nueva == null || !nueva.equals(repetir)) {
            aviso("Las contrasenas no coinciden.");
            return null;
        }
        try {
            Long t = sesion.getPendienteTenant();
            Long p = sesion.getPendientePersona();
            auth.definirPassword(t, p, nueva, ip(), ua());
            // Con la clave recien definida, autenticamos con la misma identidad ya validada por OTP.
            PortalAuthService.Identidad id = auth.loginPassword(t, sesion.getPendienteDocumento(), nueva, ip(), ua());
            sesion.autenticar(id);
            return "/portal/inicio?faces-redirect=true";
        } catch (NegocioException e) {
            aviso(e.getMessage());
            return null;
        }
    }

    // ── util ────────────────────────────────────────────────────────────────

    private void aviso(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null));
    }

    private String ip() {
        try {
            var ec = FacesContext.getCurrentInstance().getExternalContext();
            var req = (jakarta.servlet.http.HttpServletRequest) ec.getRequest();
            String fwd = req.getHeader("X-Forwarded-For");
            return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
        } catch (RuntimeException e) { return null; }
    }

    private String ua() {
        try {
            var ec = FacesContext.getCurrentInstance().getExternalContext();
            var req = (jakarta.servlet.http.HttpServletRequest) ec.getRequest();
            return req.getHeader("User-Agent");
        } catch (RuntimeException e) { return null; }
    }

    // ── getters/setters ───────────────────────────────────────────────────────
    public List<PortalAuthService.Empresa> getEmpresas() { return empresas; }
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getDocumentoSolicitud() { return sesion.getSolicitudDocumento(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNueva() { return nueva; }
    public void setNueva(String nueva) { this.nueva = nueva; }
    public String getRepetir() { return repetir; }
    public void setRepetir(String repetir) { this.repetir = repetir; }
    public String getCanalInfo() { return canalInfo; }
}
