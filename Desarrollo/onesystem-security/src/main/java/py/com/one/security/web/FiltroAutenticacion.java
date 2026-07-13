package py.com.one.security.web;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Proteccion de paginas (REQ-0004): sin sesion iniciada solo se accede a login.xhtml
 * y a los recursos estaticos de JSF. Los POST AJAX de una sesion vencida reciben la
 * redireccion en formato partial-response (asi PrimeFaces navega al login en vez de
 * romperse en silencio).
 */
@WebFilter(urlPatterns = "*.xhtml")
public class FiltroAutenticacion implements Filter {

    @Inject
    private SesionUsuario sesion;

    @Override
    public void doFilter(ServletRequest solicitud, ServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        var req = (HttpServletRequest) solicitud;
        var res = (HttpServletResponse) respuesta;

        String uri = req.getRequestURI();
        boolean esLogin = uri.endsWith("/login.xhtml");
        boolean esRecurso = uri.contains("/jakarta.faces.resource/");
        boolean esCambioPassword = uri.endsWith("/cambiar-password.xhtml");
        // Portal externo de socios (REQ-0078): rutas /portal/** son PUBLICAS respecto del login
        // administrativo; su propia sesion (PortalSesion) y el viewAction de cada pagina controlan
        // el acceso del socio. Asi el socio nunca pasa por el login de empleados.
        boolean esPortal = uri.contains("/portal/");

        if (esLogin || esRecurso || esPortal) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        String destino;
        if (sesion.isLogueado()) {
            // con cambio de contrasena pendiente, la unica pantalla permitida es esa
            if (esCambioPassword || !sesion.getUsuario().isDebeCambiar()) {
                cadena.doFilter(solicitud, respuesta);
                return;
            }
            destino = req.getContextPath() + "/cambiar-password.xhtml";
        } else {
            destino = req.getContextPath() + "/login.xhtml";
            // Return-url: recordar la pagina que el usuario pidio (solo navegacion GET) para volver
            // alli despues del login. LoginBean lo lee y valida antes de redirigir (anti open-redirect).
            if ("GET".equalsIgnoreCase(req.getMethod()) && !esCambioPassword) {
                String qs = req.getQueryString();
                req.getSession(true).setAttribute(LoginBean.ATTR_DESTINO, uri + (qs != null ? "?" + qs : ""));
            }
        }
        if ("partial/ajax".equals(req.getHeader("Faces-Request"))) {
            res.setContentType("text/xml");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().printf(
                "<?xml version='1.0' encoding='UTF-8'?><partial-response><redirect url=\"%s\"/></partial-response>",
                destino);
        } else {
            res.sendRedirect(destino);
        }
    }
}
