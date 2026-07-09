package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reportes en PDF ESTANDAR (OpenPDF via PdfService). Consultas + armado directo, sin plantillas.
 * REQ-0026 (redefinido) / 0027 / 0028 / 0029.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class ReporteService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @Inject
    private PdfService pdf;

    @Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    private static final DecimalFormat GS;
    static {
        var s = new DecimalFormatSymbols(Locale.forLanguageTag("es-PY"));
        s.setGroupingSeparator('.');
        GS = new DecimalFormat("#,##0", s);
    }
    private static String gs(Object n) { return n == null ? "0" : GS.format(((Number) n).doubleValue()); }
    private static String txt(Object o) { return o == null ? "" : o.toString(); }

    private String empresaDe(Long cobroOEmpresaId, boolean esEmpresa) {
        Long emp = cobroOEmpresaId;
        if (!esEmpresa) {
            var r = em.createNativeQuery("SELECT tenant FROM cobro WHERE cobro = :c").setParameter("c", cobroOEmpresaId).getResultList();
            emp = r.isEmpty() ? null : ((Number) r.get(0)).longValue();
        }
        if (emp == null) return "SGInmo";
        var r = em.createNativeQuery("SELECT razon_social FROM persona_juridica WHERE persona = :e").setParameter("e", emp).getResultList();
        return r.isEmpty() ? "SGInmo" : txt(r.get(0));
    }

    // ── Recibo de cobro (REQ-0028) ──
    @SuppressWarnings("unchecked")
    public byte[] reciboCobro(Long cobroId, String usuario, Long empresaContexto) {
        autorizacion.exigir("caja", "EXPORTAR");
        if (empresaContexto == null) throw new py.com.one.core.NegocioException("Falta el contexto de empresa");
        // aislamiento (obs 236): el cobro debe ser de la empresa del contexto
        List<Object[]> cab = em.createNativeQuery(
            "SELECT c.cobro, c.fecha, c.monto, COALESCE(p.nombre,'-'), COALESCE(fp.descripcion,'Efectivo'), c.estado"
            + " FROM cobro c LEFT JOIN persona p ON p.persona=c.persona"
            + " LEFT JOIN forma_pago fp ON fp.forma_pago=c.forma_pago WHERE c.cobro=:c AND c.tenant=:emp")
            .setParameter("c", cobroId).setParameter("emp", empresaContexto).getResultList();
        if (cab.isEmpty()) throw new py.com.one.core.NegocioException("El cobro no existe");
        Object[] c = cab.get(0);
        var r = pdf.iniciar(empresaDe(cobroId, false), "RECIBO DE COBRO N° " + txt(c[0]), usuario, null);
        pdf.parrafo(r, "Fecha: " + txt(c[1]));
        pdf.parrafo(r, "Recibí de: " + txt(c[3]));
        pdf.parrafo(r, "Forma de pago: " + txt(c[4]));
        pdf.espacio(r);
        List<Object[]> det = em.createNativeQuery(
            "SELECT d.tipo, d.serie, d.numero, cd.monto"
            + " FROM cobro_detalle cd JOIN documento d ON d.documento=cd.documento"
            + " WHERE cd.cobro=:c AND cd.estado='ACTIVO'")
            .setParameter("c", cobroId).getResultList();
        var filas = new ArrayList<String[]>();
        for (Object[] d : det) {
            filas.add(new String[]{txt(d[0]) + " " + txt(d[1]) + "-" + txt(d[2]), gs(d[3])});
        }
        pdf.tabla(r, new String[]{"Comprobante aplicado", "Monto"}, filas, new float[]{4, 1});
        pdf.espacio(r);
        pdf.parrafo(r, "TOTAL: Gs. " + gs(c[2]) + (("ANULADO".equals(txt(c[5]))) ? "   [ANULADO]" : ""));
        return pdf.cerrar(r);
    }

    // ── Estado de cuenta / cronograma de una operacion (REQ-0028) ──
    @SuppressWarnings("unchecked")
    public byte[] estadoCuenta(Long operacionId, String usuario, Long empresaContexto) {
        autorizacion.exigir("operaciones", "EXPORTAR");
        if (empresaContexto == null) throw new py.com.one.core.NegocioException("Falta el contexto de empresa");
        // aislamiento (obs 236): la operacion debe ser de la empresa del contexto
        List<Object[]> cab = em.createNativeQuery(
            "SELECT o.operacion, p.nombre, a.nombre, o.monto_total_operacion, o.tenant,"
            + " s.saldo_pendiente, s.total_cancelado"
            + " FROM operacion o JOIN persona p ON p.persona=o.cliente JOIN activo a ON a.activo=o.activo"
            + " JOIN v_operacion_saldo s ON s.operacion=o.operacion WHERE o.operacion=:o AND o.tenant=:emp")
            .setParameter("o", operacionId).setParameter("emp", empresaContexto).getResultList();
        if (cab.isEmpty()) throw new py.com.one.core.NegocioException("La operación no existe");
        Object[] o = cab.get(0);
        var r = pdf.iniciar(empresaDe(((Number) o[4]).longValue(), true),
                "ESTADO DE CUENTA — Operación N° " + txt(o[0]), usuario,
                "Cliente: " + txt(o[1]) + "  ·  Activo: " + txt(o[2]));
        pdf.parrafo(r, "Monto total: Gs. " + gs(o[3])
                + "     Cancelado: Gs. " + gs(o[6]) + "     Saldo pendiente: Gs. " + gs(o[5]));
        pdf.espacio(r);
        List<Object[]> cuotas = em.createNativeQuery(
            "SELECT numero_cuota, fecha_vencimiento, monto, saldo, estado,"
            + " f_mora_cuota(cronograma_cuota, current_date) FROM cronograma_cuota"
            + " WHERE operacion=:o ORDER BY numero_cuota")
            .setParameter("o", operacionId).getResultList();
        var filas = new ArrayList<String[]>();
        for (Object[] q : cuotas) {
            filas.add(new String[]{txt(q[0]), txt(q[1]), gs(q[2]), gs(q[3]),
                    "CANCELADO".equals(txt(q[4])) ? "Cancelada" : "Pendiente", gs(q[5])});
        }
        pdf.tabla(r, new String[]{"Cuota", "Vencimiento", "Monto", "Saldo", "Estado", "Mora hoy"},
                filas, new float[]{1, 2, 2, 2, 2, 2});
        return pdf.cerrar(r);
    }

    // ── Recaudacion de una planilla de caja (REQ-0029) ──
    @SuppressWarnings("unchecked")
    public byte[] recaudacionPlanilla(Long planillaId, String usuario, Long empresaContexto) {
        autorizacion.exigir("caja", "EXPORTAR");
        if (empresaContexto == null) throw new py.com.one.core.NegocioException("Falta el contexto de empresa");
        // aislamiento (obs 236): la planilla debe ser de la empresa del contexto
        List<Object[]> cab = em.createNativeQuery(
            "SELECT pl.planilla, pl.fecha_apertura, pl.monto_apertura, pl.monto_cobro, pl.estado, pl.tenant"
            + " FROM planilla pl WHERE pl.planilla=:p AND pl.tenant=:emp")
            .setParameter("p", planillaId).setParameter("emp", empresaContexto).getResultList();
        if (cab.isEmpty()) throw new py.com.one.core.NegocioException("La planilla no existe");
        Object[] pl = cab.get(0);
        var r = pdf.iniciar(empresaDe(((Number) pl[5]).longValue(), true),
                "RECAUDACIÓN — Planilla N° " + txt(pl[0]), usuario, "Fecha: " + txt(pl[1]) + " · " + txt(pl[4]));
        List<Object[]> cobros = em.createNativeQuery(
            "SELECT c.cobro, COALESCE(p.nombre,'-'), c.monto, COALESCE(fp.descripcion,'Efectivo'), c.estado"
            + " FROM cobro c LEFT JOIN persona p ON p.persona=c.persona"
            + " LEFT JOIN forma_pago fp ON fp.forma_pago=c.forma_pago"
            + " WHERE c.planilla=:p ORDER BY c.cobro").setParameter("p", planillaId).getResultList();
        var filas = new ArrayList<String[]>();
        for (Object[] cb : cobros) {
            filas.add(new String[]{txt(cb[0]), txt(cb[1]), gs(cb[2]), txt(cb[3]),
                    "ACTIVO".equals(txt(cb[4])) ? "Activo" : "Anulado"});
        }
        pdf.tabla(r, new String[]{"#", "Cliente", "Monto", "Forma", "Estado"}, filas, new float[]{1, 4, 2, 2, 2});
        pdf.espacio(r);
        pdf.parrafo(r, "Apertura: Gs. " + gs(pl[2]) + "     Cobrado: Gs. " + gs(pl[3])
                + "     Total en caja: Gs. " + gs(((Number) pl[2]).doubleValue() + ((Number) pl[3]).doubleValue()));
        return pdf.cerrar(r);
    }

    // ── Listado de activos/propiedades (REQ-0027) ──
    @SuppressWarnings("unchecked")
    public byte[] listadoActivos(String usuario, Long empresaContexto) {
        autorizacion.exigir("activos", "EXPORTAR");
        if (empresaContexto == null) throw new py.com.one.core.NegocioException("Falta el contexto de empresa");
        // aislamiento (obs 236): activos de la empresa del contexto (los sin empresa son globales)
        var r = pdf.iniciar(empresaDe(empresaContexto, true), "LISTADO DE ACTIVOS / PROPIEDADES", usuario, null);
        List<Object[]> activos = em.createNativeQuery(
            "SELECT a.nombre, a.tipo, a.precio_venta, a.precio_alquiler, a.estado"
            + " FROM activo a WHERE a.tenant = :emp ORDER BY a.nombre")
            .setParameter("emp", empresaContexto).getResultList();
        var filas = new ArrayList<String[]>();
        for (Object[] a : activos) {
            filas.add(new String[]{txt(a[0]), txt(a[1]), gs(a[2]), gs(a[3]), txt(a[4])});
        }
        pdf.tabla(r, new String[]{"Nombre", "Tipo", "Precio venta", "Precio alquiler", "Situación"},
                filas, new float[]{4, 2, 2, 2, 2});
        pdf.espacio(r);
        pdf.parrafo(r, "Total de activos: " + activos.size());
        return pdf.cerrar(r);
    }
}
