package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.PortalService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;

/** REQ-0055 - Portal de cuenta del cliente (solo lectura). Aisla por la persona del usuario logueado. */
@Named
@ViewScoped
public class PortalBean implements Serializable {

    @Inject
    private transient PortalService portal;

    @Inject
    private SesionUsuario sesion;

    private Long persona;
    private Long tenant;
    private String usuario;
    private transient String ip;

    private PortalService.ResumenCuenta resumen;
    private List<PortalService.FilaCuota> cuotas;
    private List<PortalService.FilaPago> pagos;
    private List<PortalService.FilaDoc> documentos;

    @PostConstruct
    public void iniciar() {
        if (!esPortal()) return;
        persona = sesion.getUsuario().getPersona();
        tenant = sesion.tenantUsuario();
        usuario = sesion.codigoUsuario();
        ip = ipCliente();
        resumen = portal.resumen(persona);
        cuotas = portal.cuotas(persona);
        pagos = portal.pagos(persona);
        documentos = portal.documentos(persona);
        try { portal.registrarAcceso(tenant, usuario, persona, "ACCESO", "portal/inicio", ip); }
        catch (RuntimeException ignore) { /* la auditoria no bloquea el portal */ }
    }

    /** Solo usuarios con perfil PORTAL y persona vinculada. Admins van al panel; anonimos, al login. */
    public String verificarAcceso() {
        if (sesion == null || !sesion.isLogueado()) return "/login?faces-redirect=true";
        if (!esPortal()) return "/index?faces-redirect=true";
        if (sesion.getUsuario().getPersona() == null) return "/login?faces-redirect=true";
        return null;
    }

    private boolean esPortal() {
        return sesion != null && sesion.isLogueado() && sesion.getUsuario() != null
                && "PORTAL".equals(sesion.getUsuario().getPerfil());
    }

    public StreamedContent descargar(PortalService.FilaDoc d) {
        final Long id = d.getId();
        return DefaultStreamedContent.builder()
                .name(d.getNombre())
                .contentType("application/octet-stream")
                .stream(() -> {
                    PortalService.Descarga dd = portal.descargar(id, persona, tenant, usuario, ip);
                    return new ByteArrayInputStream(dd.datos);
                })
                .build();
    }

    public String salir() { return sesion.cerrarSesion(); }

    private String ipCliente() {
        try {
            var ec = FacesContext.getCurrentInstance().getExternalContext();
            var req = (jakarta.servlet.http.HttpServletRequest) ec.getRequest();
            String fwd = req.getHeader("X-Forwarded-For");
            return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public PortalService.ResumenCuenta getResumen() { return resumen; }
    public List<PortalService.FilaCuota> getCuotas() { return cuotas; }
    public List<PortalService.FilaPago> getPagos() { return pagos; }
    public List<PortalService.FilaDoc> getDocumentos() { return documentos; }
    public String getNombreUsuario() { return usuario; }
}
