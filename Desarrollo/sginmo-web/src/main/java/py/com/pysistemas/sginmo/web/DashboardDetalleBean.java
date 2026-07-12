package py.com.pysistemas.sginmo.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.servicio.DrilldownService;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * REQ-0074 - Pantalla generica de evidencia (drill-down). Lee la clave del indicador y los filtros como
 * view params (whitelist en el servicio). Requiere ver el dashboard para abrir; el detalle exige ademas
 * el permiso del modulo origen. Exporta CSV con los filtros aplicados.
 */
@Named
@ViewScoped
public class DashboardDetalleBean implements Serializable {

    @Inject
    private transient DrilldownService servicio;
    @Inject
    private SesionUsuario sesion;

    private String clave;
    private String desde;   // yyyy-MM-dd
    private String hasta;
    private Long moneda;
    private Long sucursal;
    private Long ref;
    private String aplic;   // filtro de aplicacion (rentabilidad por tipo, REQ-0071)
    private DrilldownService.Detalle detalle;
    private String error;

    public String verificarAcceso() {
        return sesion.puede("dashboard-gerencial", "VER") ? null : "/index?faces-redirect=true";
    }

    public void cargar() {
        if (!sesion.puede("dashboard-gerencial", "VER")) return;
        if (clave == null || clave.isBlank()) return;   // estado vacio: sin indicador
        try {
            detalle = servicio.detalle(clave, parse(desde), parse(hasta), moneda, sucursal, ref, aplic);
        } catch (NegocioException e) {
            error = e.getMessage();
        } catch (RuntimeException e) {
            error = "No se pudo cargar el detalle";
        }
    }

    public org.primefaces.model.StreamedContent getCsv() {
        if (detalle == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("Detalle,").append(esc(detalle.getTitulo())).append("\n");
        if (detalle.getFiltros() != null) sb.append("Filtros,").append(esc(detalle.getFiltros())).append("\n");
        sb.append("Generado,").append(esc(detalle.getGenerado())).append("\n\n");
        sb.append(join(detalle.getColumnas())).append("\n");
        for (String[] fila : detalle.getFilas()) sb.append(join(fila)).append("\n");
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return org.primefaces.model.DefaultStreamedContent.builder()
                .name("detalle_" + detalle.getClave() + ".csv").contentType("text/csv")
                .stream(() -> new ByteArrayInputStream(bytes)).build();
    }

    private static LocalDate parse(String s) {
        try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s.trim()); }
        catch (RuntimeException e) { return null; }
    }
    private static String join(String[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) { if (i > 0) sb.append(','); sb.append(esc(v[i])); }
        return sb.toString();
    }
    private static String esc(String v) {
        if (v == null) return "";
        return (v.contains(",") || v.contains("\"") || v.contains("\n")) ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    public String getClave() { return clave; }
    public void setClave(String v) { clave = v; }
    public String getDesde() { return desde; }
    public void setDesde(String v) { desde = v; }
    public String getHasta() { return hasta; }
    public void setHasta(String v) { hasta = v; }
    public Long getMoneda() { return moneda; }
    public void setMoneda(Long v) { moneda = v; }
    public Long getSucursal() { return sucursal; }
    public void setSucursal(Long v) { sucursal = v; }
    public Long getRef() { return ref; }
    public void setRef(Long v) { ref = v; }
    public String getAplic() { return aplic; }
    public void setAplic(String v) { aplic = v; }
    public DrilldownService.Detalle getDetalle() { return detalle; }
    public String getError() { return error; }
    public boolean isVacio() { return detalle == null; }
}
