package py.com.pysistemas.sginmo.web;

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

        if (esLogin || esRecurso || sesion.isLogueado()) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        String destino = req.getContextPath() + "/login.xhtml";
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
