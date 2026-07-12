package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * REQ-0062 - Servicio comun de reportes con parametros tipados y salida PDF/CSV (sin JasperReports).
 * @AislarTenant (RLS por empresa). Respeta el limite de filas de exportacion (parametro EXPORT_LIMITE_FILAS,
 * REQ-0060). Los montos usan BigDecimal y formato local; no se mezclan monedas (se filtra por moneda).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class ReportesConsultaService {

    private static final DecimalFormat GS;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("es", "PY"));
        s.setGroupingSeparator('.');
        GS = new DecimalFormat("#,##0", s);
    }

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private PdfService pdf;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;
    @jakarta.inject.Inject
    private TenantContext tenant;
    @jakarta.inject.Inject
    private ParametroConfig parametros;

    private int limite() { return Math.max(1, parametros.entero("EXPORT_LIMITE_FILAS", 1000)); }

    /** Construye el reporte tabular pedido con sus parametros. Requiere permiso VER. */
    public Reporte generar(String tipo, LocalDate desde, LocalDate hasta, Long moneda) {
        autorizacion.exigir("reportes", "VER");
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) return new Reporte("Sin empresa", new String[0]);
        return switch (tipo) {
            case "PROPIEDADES" -> propiedades();
            case "COBROS"      -> cobros(desde, hasta, moneda);
            case "MORA"        -> mora();
            default -> throw new NegocioException("Reporte no soportado: " + tipo);
        };
    }

    private Reporte propiedades() {
        Reporte r = new Reporte("Propiedades disponibles", new String[]{"Nombre", "Estado", "Precio venta", "Precio alquiler"});
        Query q = em.createNativeQuery(
            "SELECT nombre, estado, COALESCE(precio_venta,0), COALESCE(precio_alquiler,0)"
          + " FROM activo WHERE estado='LIBRE' ORDER BY nombre");
        for (Object[] f : filas(q)) r.filas.add(new String[]{ s(f[0]), s(f[1]), GS.format(dec(f[2])), GS.format(dec(f[3])) });
        return r;
    }

    private Reporte cobros(LocalDate desde, LocalDate hasta, Long moneda) {
        if (desde == null || hasta == null) throw new NegocioException("Indique el periodo (desde/hasta)");
        Reporte r = new Reporte("Cobros por periodo", new String[]{"Fecha", "Cliente", "Forma de pago", "Moneda", "Monto"});
        StringBuilder sql = new StringBuilder(
            "SELECT c.fecha, vp.nombre, fp.descripcion, mo.descripcion, c.monto"
          + " FROM cobro c LEFT JOIN v_persona vp ON vp.persona=c.persona"
          + " LEFT JOIN forma_pago fp ON fp.forma_pago=c.forma_pago LEFT JOIN moneda mo ON mo.moneda=c.moneda"
          + " WHERE c.estado='ACTIVO' AND c.fecha BETWEEN :d AND :h");
        if (moneda != null) sql.append(" AND c.moneda = :mon");
        sql.append(" ORDER BY c.fecha");
        Query q = em.createNativeQuery(sql.toString()).setParameter("d", desde).setParameter("h", hasta);
        if (moneda != null) q.setParameter("mon", moneda);
        // Obs 265: NO se totaliza mezclando monedas. Con una sola moneda -> total unico; con "Todas" ->
        // subtotal por moneda (nunca una suma agregada sin criterio de conversion).
        java.util.LinkedHashMap<String, BigDecimal> porMoneda = new java.util.LinkedHashMap<>();
        for (Object[] f : filas(q)) {
            BigDecimal m = dec(f[4]);
            String monNom = s(f[3]);
            porMoneda.merge(monNom.isBlank() ? "(sin moneda)" : monNom, m, BigDecimal::add);
            r.filas.add(new String[]{ fecha(f[0]), s(f[1]), s(f[2]), s(f[3]), GS.format(m) });
        }
        if (moneda != null) {
            BigDecimal total = porMoneda.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            r.total = "Total: " + GS.format(total);
        } else {
            StringBuilder sb = new StringBuilder("Totales por moneda: ");
            boolean first = true;
            for (var e : porMoneda.entrySet()) {
                if (!first) sb.append("   |   ");
                sb.append(e.getKey()).append(": ").append(GS.format(e.getValue()));
                first = false;
            }
            r.total = porMoneda.isEmpty() ? "Sin cobros en el periodo" : sb.toString();
        }
        return r;
    }

    private Reporte mora() {
        Reporte r = new Reporte("Cartera en mora", new String[]{"Cliente", "Operacion", "Cuota", "Vencimiento", "Saldo", "Dias"});
        Query q = em.createNativeQuery(
            "SELECT vp.nombre, o.operacion, cc.numero_cuota, cc.fecha_vencimiento, cc.saldo,"
          + " (current_date - cc.fecha_vencimiento) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " LEFT JOIN v_persona vp ON vp.persona=o.cliente"
          + " WHERE cc.estado='PENDIENTE' AND cc.fecha_vencimiento < current_date ORDER BY 6 DESC");
        for (Object[] f : filas(q)) r.filas.add(new String[]{
            s(f[0]), s(f[1]), s(f[2]), fecha(f[3]), GS.format(dec(f[4])), s(f[5]) });
        return r;
    }

    // ── Salidas ──

    public byte[] pdf(Reporte r) {
        autorizacion.exigir("reportes", "EXPORTAR");
        var rep = pdf.iniciar(empresaNombre(), r.titulo, sesion.codigoUsuario(),
                r.filas.size() + " fila(s)" + (r.filas.size() >= limite() ? " (limitado a " + limite() + ")" : ""));
        float[] anchos = new float[r.columnas.length];
        java.util.Arrays.fill(anchos, 1f);
        pdf.tabla(rep, r.columnas, r.filas, anchos);
        if (r.total != null) { pdf.espacio(rep); pdf.titulo(rep, r.total); }
        return pdf.cerrar(rep);
    }

    public byte[] csv(Reporte r) {
        autorizacion.exigir("reportes", "EXPORTAR");
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", escapar(r.columnas))).append("\n");
        for (String[] fila : r.filas) sb.append(String.join(",", escapar(fila))).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Utilidades ──

    @SuppressWarnings("unchecked")
    private List<Object[]> filas(Query q) { return q.setMaxResults(limite()).getResultList(); }

    private String empresaNombre() {
        try {
            Object n = em.createNativeQuery("SELECT nombre FROM v_persona WHERE persona=:t")
                    .setParameter("t", tenant.actual()).getSingleResult();
            return n == null ? "SGInmo" : n.toString();
        } catch (RuntimeException e) { return "SGInmo"; }
    }

    private static String[] escapar(String[] vals) {
        String[] out = new String[vals.length];
        for (int i = 0; i < vals.length; i++) {
            String v = vals[i] == null ? "" : vals[i];
            out[i] = (v.contains(",") || v.contains("\"") || v.contains("\n"))
                    ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
        }
        return out;
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }
    private static BigDecimal dec(Object o) { return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString()); }
    private static String fecha(Object o) {
        if (o == null) return "";
        LocalDate d = (o instanceof java.sql.Date sd) ? sd.toLocalDate() : LocalDate.parse(o.toString());
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    public static class Reporte {
        public final String titulo;
        public final String[] columnas;
        public final List<String[]> filas = new ArrayList<>();
        public String total;
        public Reporte(String titulo, String[] columnas) { this.titulo = titulo; this.columnas = columnas; }
        public String getTitulo() { return titulo; }
        public String[] getColumnas() { return columnas; }
        public List<String[]> getFilas() { return filas; }
        public String getTotal() { return total; }
    }
}
