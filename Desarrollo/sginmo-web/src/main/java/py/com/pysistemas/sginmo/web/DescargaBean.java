package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.PlantillaDocumentoService;
import py.com.pysistemas.sginmo.servicio.ReporteService;

import java.io.Serializable;
import java.util.List;

/** Descarga de PDFs estandar (OpenPDF). Escribe el PDF en la respuesta HTTP. */
@Named
@RequestScoped
public class DescargaBean implements Serializable {

    @Inject
    private transient ReporteService reporteService;

    @Inject
    private transient PlantillaDocumentoService plantillaDocumentoService;

    @Inject
    private SesionUsuario sesion;

    @Inject
    private ContextoEmpresa contexto;

    /** Empresa del contexto (obs 236): los reportes solo exponen datos de esa empresa. */
    private Long empresaContexto() {
        return contexto.getEmpresa() == null ? null : contexto.getEmpresa().getId();
    }

    public void recibo(Long cobroId) {
        enviar(reporteService.reciboCobro(cobroId, sesion.codigoUsuario(), empresaContexto()), "recibo-" + cobroId + ".pdf");
    }

    public void estadoCuenta(Long operacionId) {
        enviar(reporteService.estadoCuenta(operacionId, sesion.codigoUsuario(), empresaContexto()), "estado-cuenta-" + operacionId + ".pdf");
    }

    public void recaudacion(Long planillaId) {
        enviar(reporteService.recaudacionPlanilla(planillaId, sesion.codigoUsuario(), empresaContexto()), "recaudacion-" + planillaId + ".pdf");
    }

    public void listadoActivos() {
        enviar(reporteService.listadoActivos(sesion.codigoUsuario(), empresaContexto()), "activos.pdf");
    }

    public void contrato(Long operacionId) {
        var doc = plantillaDocumentoService.contrato(operacionId, sesion.codigoUsuario());
        enviar(doc.contenido(), doc.nombreArchivo());
    }

    public void pagare(Long operacionId, Long cuotaId) {
        var doc = plantillaDocumentoService.pagare(operacionId, cuotaId, sesion.codigoUsuario());
        enviar(doc.contenido(), doc.nombreArchivo());
    }

    public void pagares(Long operacionId, List<Long> cuotaIds) {
        var doc = plantillaDocumentoService.pagares(operacionId, cuotaIds, sesion.codigoUsuario());
        enviar(doc.contenido(), doc.nombreArchivo());
    }

    private void enviar(byte[] pdf, String archivo) {
        var ctx = FacesContext.getCurrentInstance();
        var ext = ctx.getExternalContext();
        try {
            ext.responseReset();
            ext.setResponseContentType("application/pdf");
            ext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + archivo + "\"");
            ext.setResponseContentLength(pdf.length);
            ext.getResponseOutputStream().write(pdf);
            ctx.responseComplete();
        } catch (Exception e) {
            throw new py.com.one.core.NegocioException("No se pudo descargar el PDF: " + e.getMessage());
        }
    }
}
