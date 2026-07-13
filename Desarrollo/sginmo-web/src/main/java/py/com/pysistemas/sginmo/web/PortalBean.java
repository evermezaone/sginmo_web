package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import py.com.pysistemas.sginmo.servicio.PortalService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;

/**
 * REQ-0055/REQ-0078 - Portal de cuenta del socio (solo lectura). La identidad proviene de la
 * {@link PortalSesion} externa (CI/RUC + OTP + password), NO del usuario administrativo ni de
 * usuario.perfil='PORTAL'. Todas las consultas se aislan por la persona autenticada.
 */
@Named
@ViewScoped
public class PortalBean implements Serializable {

    @Inject
    private transient PortalService portal;

    @Inject
    private transient py.com.pysistemas.sginmo.servicio.PortalAuthService auth;

    @Inject
    private PortalSesion sesion;

    private Long persona;
    private Long tenant;
    private transient String ip;

    private PortalService.ResumenCuenta resumen;
    private List<PortalService.FilaCuota> cuotas;
    private List<PortalService.FilaPago> pagos;
    private List<PortalService.FilaDoc> documentos;
    // Vista de propietario (obs 300).
    private List<PortalService.FilaActivo> activos = List.of();
    private List<PortalService.FilaOperacion> operaciones = List.of();
    private List<PortalService.FilaLiquidacion> liquidaciones = List.of();
    private List<PortalService.FilaDoc> documentosPropietario = List.of();

    @PostConstruct
    public void iniciar() {
        if (!sesion.isAutenticado()) return;
        persona = sesion.getPersona();
        tenant = sesion.getTenant();
        ip = ipCliente();
        // Vista de cliente (si tiene rol CLIENTE).
        if (sesion.isEsCliente()) {
            resumen = portal.resumen(persona);
            cuotas = portal.cuotas(persona);
            pagos = portal.pagos(persona);
            documentos = portal.documentos(persona);
        }
        // Vista de propietario (obs 300): sus activos, operaciones, liquidaciones y documentos.
        if (sesion.isEsPropietario()) {
            activos = portal.activosPropietario(persona);
            operaciones = portal.operacionesPropietario(persona);
            liquidaciones = portal.liquidacionesPropietario(persona);
            documentosPropietario = portal.documentosPropietario(persona);
        }
        try { portal.registrarAcceso(tenant, "portal", persona, "ACCESO", "portal/inicio", ip); }
        catch (RuntimeException ignore) { /* la auditoria no bloquea el portal */ }
    }

    /** Solo socios autenticados por el portal externo; si no, al login publico del portal. */
    public String verificarAcceso() {
        return sesion.isAutenticado() ? null : "/portal/login?faces-redirect=true";
    }

    public StreamedContent descargar(PortalService.FilaDoc d) {
        final Long id = d.getId();
        return DefaultStreamedContent.builder()
                .name(d.getNombre())
                .contentType("application/octet-stream")
                .stream(() -> {
                    PortalService.Descarga dd = portal.descargar(id, persona, tenant, "portal", ip);
                    return new ByteArrayInputStream(dd.datos);
                })
                .build();
    }

    public String salir() {
        try { auth.auditarLogout(tenant, persona, ip, null); } catch (RuntimeException ignore) { }
        sesion.cerrar();
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/portal/login?faces-redirect=true";
    }

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
    public String getNombreUsuario() { return sesion.getNombre(); }
    public boolean isEsPropietario() { return sesion.isEsPropietario(); }
    public boolean isEsCliente() { return sesion.isEsCliente(); }
    public List<PortalService.FilaActivo> getActivos() { return activos; }
    public List<PortalService.FilaOperacion> getOperaciones() { return operaciones; }
    public List<PortalService.FilaLiquidacion> getLiquidaciones() { return liquidaciones; }
    public List<PortalService.FilaDoc> getDocumentosPropietario() { return documentosPropietario; }
}
