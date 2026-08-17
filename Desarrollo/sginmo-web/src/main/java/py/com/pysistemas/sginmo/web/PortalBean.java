package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
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

    @Inject
    private transient py.com.pysistemas.sginmo.servicio.QrPagoService qr;

    @Inject
    private transient py.com.pysistemas.sginmo.servicio.PortalTransferenciaService transferencias;

    private Long persona;
    private Long tenant;
    private transient String ip;

    private PortalService.ResumenCuenta resumen;
    private List<PortalService.FilaCuota> cuotas;
    private List<PortalService.FilaPago> pagos;
    // REQ-0100: transferencias informadas aun no aplicadas (en proceso de aceptacion).
    private List<py.com.pysistemas.sginmo.servicio.PortalTransferenciaService.Fila> transferenciasEnProceso = List.of();
    // REQ-0100: comprobante seleccionado para previsualizar en el modal (bytes + tipo + nombre reales).
    private transient byte[] comprobanteBytes;
    private String comprobanteMime;
    private String comprobanteNombre;
    private List<PortalService.FilaDoc> documentos;
    // Vista de propietario (obs 300).
    private List<PortalService.FilaActivo> activos = List.of();
    private List<PortalService.FilaOperacion> operaciones = List.of();
    private List<PortalService.FilaLiquidacion> liquidaciones = List.of();
    private List<PortalService.FilaDoc> documentosPropietario = List.of();

    // REQ-0097: filtro de anio para la grilla de cuotas (null = ano actual + pendientes).
    private Integer anioCuotas;
    private List<Integer> aniosCuotas = List.of();

    // REQ-0093: pago por QR (Fase 1). Se calcula una vez por vista.
    private boolean qrHabilitado;
    private java.math.BigDecimal qrMonto;
    private String qrDataUri;

    @PostConstruct
    public void iniciar() {
        if (!sesion.isAutenticado()) return;
        persona = sesion.getPersona();
        tenant = sesion.getTenant();
        ip = ipCliente();
        // Vista de cliente (si tiene rol CLIENTE).
        if (sesion.isEsCliente()) {
            resumen = portal.resumen(persona);
            aniosCuotas = portal.aniosConCuotas(persona);
            cuotas = portal.cuotas(persona, anioCuotas);
            pagos = portal.pagos(persona);
            documentos = portal.documentos(persona);
            // REQ-0100: transferencias informadas que aun no se aplicaron (las APLICADO ya figuran en pagos).
            transferenciasEnProceso = transferencias.mias(persona).stream()
                    .filter(t -> !"APLICADO".equals(t.getEstado())).toList();
            calcularQr();
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

    /** REQ-0100: carga el comprobante seleccionado (bytes + tipo + nombre) para mostrarlo en el modal. */
    public void verComprobante(py.com.pysistemas.sginmo.servicio.PortalTransferenciaService.Fila t) {
        try {
            var d = transferencias.descargar(t.getId(), persona);
            comprobanteBytes = d.datos;
            comprobanteMime = d.contentType;
            comprobanteNombre = d.nombre;
        } catch (RuntimeException e) {
            comprobanteBytes = null; comprobanteMime = null; comprobanteNombre = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "No se pudo abrir el comprobante", e.getMessage()));
        }
    }

    public boolean isComprobanteDisponible() { return comprobanteBytes != null && comprobanteBytes.length > 0; }
    public boolean isComprobanteEsImagen() { return comprobanteMime != null && comprobanteMime.startsWith("image/"); }
    public String getComprobanteNombre() { return comprobanteNombre; }

    /**
     * Comprobante embebido como data URI (base64) para mostrarlo INLINE en el modal, sin un segundo
     * request. Evita el problema de p:graphicImage+StreamedContent en @ViewScoped (imagen en blanco).
     */
    public String getComprobanteDataUri() {
        if (comprobanteBytes == null) return null;
        String m = (comprobanteMime == null || comprobanteMime.isBlank()) ? "application/octet-stream" : comprobanteMime;
        return "data:" + m + ";base64," + java.util.Base64.getEncoder().encodeToString(comprobanteBytes);
    }

    /** Descarga del comprobante seleccionado con su NOMBRE y TIPO reales (asi baja con extension). */
    public StreamedContent getComprobanteDescarga() {
        if (comprobanteBytes == null) return null;
        final byte[] b = comprobanteBytes;
        final String m = comprobanteMime == null ? "application/octet-stream" : comprobanteMime;
        final String n = (comprobanteNombre == null || comprobanteNombre.isBlank()) ? "comprobante" : comprobanteNombre;
        return DefaultStreamedContent.builder().name(n).contentType(m)
                .stream(() -> new ByteArrayInputStream(b)).build();
    }

    /**
     * REQ-0100: el socio elimina su transferencia informada mientras sigue PENDIENTE DE VALIDACION (RECIBIDO).
     * Si el operador ya la tomo (En verificacion) u otro estado, el servicio la rechaza. Refresca la lista.
     */
    public void eliminarTransferencia(py.com.pysistemas.sginmo.servicio.PortalTransferenciaService.Fila t) {
        try {
            transferencias.eliminar(t.getId(), persona);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Transferencia eliminada", "Se quito el registro y su comprobante."));
        } catch (RuntimeException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "No se pudo eliminar", e.getMessage()));
        }
        // refresca por si cambio de estado en el interin (paso a verificacion)
        transferenciasEnProceso = transferencias.mias(persona).stream()
                .filter(x -> !"APLICADO".equals(x.getEstado())).toList();
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

    /** REQ-0093: prepara el QR de pago para el saldo a pagar (deuda vencida, o la proxima cuota). */
    private void calcularQr() {
        try {
            if (qr == null || !qr.habilitado() || resumen == null) return;
            java.math.BigDecimal m = (resumen.deudaVencida != null && resumen.deudaVencida.signum() > 0)
                    ? resumen.deudaVencida : resumen.proximaCuotaMonto;
            if (m == null || m.signum() <= 0) return;
            qrMonto = m;
            // REQ-0094: referencia UNICA por intento (dinamica); si no se pudo, cae al documento (estatica).
            String ref = qr.referenciaIntento(persona, m, null);
            if (ref == null) ref = sesion.getDocumento();
            qrDataUri = qr.pngDataUri(qr.payload(m, ref), 240);
            qrHabilitado = qrDataUri != null;
        } catch (RuntimeException ignore) { /* el QR es opcional: nunca rompe el portal */ }
    }

    public boolean isQrHabilitado() { return qrHabilitado; }
    public java.math.BigDecimal getQrMonto() { return qrMonto; }
    public String getQrDataUri() { return qrDataUri; }

    /** REQ-0097: recarga la grilla de cuotas segun el anio elegido (null = actual + pendientes). */
    public void cambiarAnio() { cuotas = portal.cuotas(persona, anioCuotas); }
    public Integer getAnioCuotas() { return anioCuotas; }
    public void setAnioCuotas(Integer anioCuotas) { this.anioCuotas = anioCuotas; }
    public List<Integer> getAniosCuotas() { return aniosCuotas; }

    public List<py.com.pysistemas.sginmo.servicio.PortalTransferenciaService.Fila> getTransferenciasEnProceso() { return transferenciasEnProceso; }

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
