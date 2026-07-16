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

    private String documento;
    private String password;
    private String codigo;
    private String nueva;
    private String repetir;
    private String canalInfo;   // texto informativo tras enviar OTP
    private Long empresaSel;     // REQ-0102: seleccion del combo de empresa (si el socio tiene mas de una)

    @PostConstruct
    public void iniciar() { /* REQ-0102: ya no se carga la lista de empresas (no se expone) */ }

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
            List<PortalAuthService.Acceso> accesos = auth.loginPasswordMulti(documento, password, ip(), ua());
            sesion.autenticarMulti(accesos);
            return "/portal/inicio?faces-redirect=true";
        } catch (NegocioException e) {
            aviso(e.getMessage());
            return null;
        }
    }

    /** REQ-0102: cambia la empresa activa desde el selector del portal (socio con mas de una empresa). */
    public void cambiarEmpresa() {
        try {
            sesion.cambiarEmpresa(empresaSel);
            var ec = FacesContext.getCurrentInstance().getExternalContext();
            ec.redirect(ec.getRequestContextPath() + "/portal/inicio.xhtml");
        } catch (Exception ignore) { }
    }

    /** Primer ingreso / olvide mi clave: envia OTP (a la(s) empresa(s) del documento) y navega al codigo. */
    public String enviarCodigo() {
        if (documento == null || documento.isBlank()) {
            aviso("Ingrese su documento (CI / RUC).");
            return null;
        }
        try { auth.solicitarOtpDoc(documento.trim(), "LOGIN", ip(), ua()); }
        catch (RuntimeException ignore) { /* mensaje generico, no revela */ }
        sesion.iniciarSolicitud(documento.trim());
        return "/portal/otp?faces-redirect=true";
    }

    public String reenviar() {
        if (!sesion.tieneSolicitud()) return "/portal/login?faces-redirect=true";
        try { auth.solicitarOtpDoc(sesion.getSolicitudDocumento(), "LOGIN", ip(), ua()); }
        catch (RuntimeException ignore) { }
        aviso("Si el documento corresponde a un socio, enviamos un nuevo codigo.");
        return null;
    }

    public String validar() {
        if (!sesion.tieneSolicitud()) return "/portal/login?faces-redirect=true";
        try {
            PortalAuthService.Identidad id = auth.validarOtpDoc(sesion.getSolicitudDocumento(), codigo, ip(), ua());
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
            String doc = sesion.getPendienteDocumento();
            auth.definirPasswordDoc(doc, nueva, ip(), ua());   // misma clave para todas las empresas del socio
            // Con la clave recien definida, autenticamos (multi-empresa).
            List<PortalAuthService.Acceso> accesos = auth.loginPasswordMulti(doc, nueva, ip(), ua());
            sesion.autenticarMulti(accesos);
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
    public Long getEmpresaSel() { return empresaSel != null ? empresaSel : sesion.getTenant(); }
    public void setEmpresaSel(Long empresaSel) { this.empresaSel = empresaSel; }
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
