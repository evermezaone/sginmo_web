package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.ReporteService;

import java.io.Serializable;

/** Descarga de PDFs estandar (OpenPDF). Escribe el PDF en la respuesta HTTP. */
@Named
@RequestScoped
public class DescargaBean implements Serializable {

    @Inject
    private transient ReporteService reporteService;

    @Inject
    private SesionUsuario sesion;

    public void recibo(Long cobroId) {
        enviar(reporteService.reciboCobro(cobroId, sesion.codigoUsuario()), "recibo-" + cobroId + ".pdf");
    }

    public void estadoCuenta(Long operacionId) {
        enviar(reporteService.estadoCuenta(operacionId, sesion.codigoUsuario()), "estado-cuenta-" + operacionId + ".pdf");
    }

    public void recaudacion(Long planillaId) {
        enviar(reporteService.recaudacionPlanilla(planillaId, sesion.codigoUsuario()), "recaudacion-" + planillaId + ".pdf");
    }

    public void listadoActivos() {
        enviar(reporteService.listadoActivos(sesion.codigoUsuario()), "activos.pdf");
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
