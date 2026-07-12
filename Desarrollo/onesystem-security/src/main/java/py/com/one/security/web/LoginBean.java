package py.com.one.security.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import py.com.one.core.NegocioException;
import py.com.one.security.servicio.SeguridadService;

/** Formulario de login (REQ-0004). */
@Named
@RequestScoped
public class LoginBean {

    @Inject
    private SeguridadService seguridadService;

    @Inject
    private SesionUsuario sesion;

    private String codigo;
    private String password;

    /** Clave usada por el filtro para recordar el destino pedido antes del login (return-url). */
    public static final String ATTR_DESTINO = "post_login_redirect";

    public String entrar() {
        try {
            var usuario = seguridadService.autenticar(codigo, password);
            // renovar el id de sesion al autenticar (evita fijacion de sesion)
            var ctx = FacesContext.getCurrentInstance().getExternalContext();
            ((HttpServletRequest) ctx.getRequest()).changeSessionId();
            sesion.iniciar(usuario, seguridadService.permisosDe(usuario.getId()));
            // alta o reseteo de contrasena: se exige cambiarla antes de usar el sistema
            if (usuario.isDebeCambiar()) {
                return "/cambiar-password?faces-redirect=true";
            }
            // REQ-0055: los usuarios de portal no entran al panel administrativo.
            if ("PORTAL".equals(usuario.getPerfil())) {
                return "/portal/inicio?faces-redirect=true";
            }
            // Return-url: si el filtro guardo el destino que el usuario pidio antes de que le
            // pidieramos login, volver alli en vez de a /index (validado: interno y .xhtml).
            String destino = destinoGuardado(ctx);
            if (destino != null) {
                try { ctx.redirect(destino); FacesContext.getCurrentInstance().responseComplete(); return null; }
                catch (java.io.IOException ignore) { /* si falla, cae al index */ }
            }
            return "/index?faces-redirect=true";
        } catch (NegocioException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, e.getMessage(), null));
            return null;
        }
    }

    /** Destino post-login guardado por el filtro. Solo se acepta una ruta interna .xhtml (anti open-redirect). */
    private String destinoGuardado(jakarta.faces.context.ExternalContext ctx) {
        var request = (HttpServletRequest) ctx.getRequest();
        var session = request.getSession(false);
        if (session == null) return null;
        Object v = session.getAttribute(ATTR_DESTINO);
        session.removeAttribute(ATTR_DESTINO);
        if (!(v instanceof String dest)) return null;
        String cp = ctx.getRequestContextPath();
        boolean interno = dest.startsWith(cp + "/") && dest.contains(".xhtml")
                && !dest.contains("://") && !dest.contains("/login.xhtml")
                && !dest.contains("/cambiar-password.xhtml");
        return interno ? dest : null;
    }

    // ── Branding configurable por entorno (variable de entorno o -D system property) ──
    // Permite rebrandear el login sin tocar codigo: SGINMO_APP_TITULO / SGINMO_APP_SUBTITULO
    // (o -Dsginmo.app.titulo / -Dsginmo.app.subtitulo). Con defaults sanos.

    public String getAppTitulo() {
        return config("sginmo.app.titulo", "SGINMO_APP_TITULO", "SGInmo");
    }

    public String getAppSubtitulo() {
        return config("sginmo.app.subtitulo", "SGINMO_APP_SUBTITULO",
                "Gestión inmobiliaria — ingrese con su usuario");
    }

    private static String config(String propiedad, String envVar, String defecto) {
        String v = System.getProperty(propiedad);
        if (v == null || v.isBlank()) v = System.getenv(envVar);
        return (v == null || v.isBlank()) ? defecto : v;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
