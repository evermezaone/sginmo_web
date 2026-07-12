package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * REQ-0058 - Recibos y comprobantes en PDF con OpenPDF (sin JasperReports). @AislarTenant para RLS.
 * El comprobante se genera on-demand desde el registro persistido (inmutable): reimprimir = regenerar,
 * conservando la trazabilidad (numero de cobro, fecha/hora, empresa/sucursal, usuario, cajero).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class ComprobanteService {

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

    /** Cobros del tenant para listar/reimprimir (cap 200). */
    public List<FilaCobro> cobrosRecientes(String estado) {
        List<FilaCobro> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) return out;
        String cond = (estado != null && !estado.isBlank()) ? " AND c.estado = :estado" : "";
        var q = em.createNativeQuery(
            "SELECT c.cobro, c.fecha, c.monto, c.estado, vp.nombre, mo.descripcion"
          + " FROM cobro c LEFT JOIN v_persona vp ON vp.persona=c.persona LEFT JOIN moneda mo ON mo.moneda=c.moneda"
          + " WHERE 1=1" + cond + " ORDER BY c.cobro DESC");
        if (!cond.isEmpty()) q.setParameter("estado", estado);
        q.setMaxResults(200);
        @SuppressWarnings("unchecked")
        List<Object[]> filas = q.getResultList();
        for (Object[] f : filas) {
            FilaCobro fc = new FilaCobro();
            fc.cobro = ((Number) f[0]).longValue();
            fc.fecha = ((java.sql.Date) f[1]).toLocalDate();
            fc.monto = f[2] == null ? BigDecimal.ZERO : new BigDecimal(f[2].toString());
            fc.estado = (String) f[3];
            fc.cliente = (String) f[4];
            fc.moneda = (String) f[5];
            out.add(fc);
        }
        return out;
    }

    /** Recibo de cobro en PDF. Requiere permiso EXPORTAR (generar/descargar/reimprimir). */
    public byte[] reciboCobro(Long cobroId) {
        autorizacion.exigir("comprobantes", "EXPORTAR");
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) throw new NegocioException("Seleccione una empresa");

        @SuppressWarnings("unchecked")
        List<Object[]> cab = em.createNativeQuery(
            "SELECT c.cobro, c.fecha, c.hora, c.monto, c.concepto, c.cajero, c.estado,"
          + " vp.nombre, fp.descripcion, mo.descripcion"
          + " FROM cobro c LEFT JOIN v_persona vp ON vp.persona=c.persona"
          + " LEFT JOIN forma_pago fp ON fp.forma_pago=c.forma_pago LEFT JOIN moneda mo ON mo.moneda=c.moneda"
          + " WHERE c.cobro=:id").setParameter("id", cobroId).getResultList();
        if (cab.isEmpty()) throw new NegocioException("El cobro no existe o no pertenece a su empresa");
        Object[] c = cab.get(0);

        String empresa = empresaNombre(emp);
        String usuario = sesion.codigoUsuario();
        String nro = String.valueOf(((Number) c[0]).longValue());
        String fecha = c[1] == null ? "" : ((java.sql.Date) c[1]).toLocalDate().toString();
        String estado = (String) c[6];

        var r = pdf.iniciar(empresa, "RECIBO DE COBRO", usuario, "Comprobante Nro " + nro + " · " + fecha
                + (estado != null && !"ACTIVO".equals(estado) ? "  [" + estado + "]" : ""));
        pdf.parrafo(r, "Cliente: " + str(c[7]));
        pdf.parrafo(r, "Forma de pago: " + str(c[8]) + "     Moneda: " + str(c[9]));
        if (c[4] != null && !str(c[4]).isBlank()) pdf.parrafo(r, "Concepto: " + str(c[4]));
        pdf.parrafo(r, "Cajero: " + str(c[5]));
        pdf.espacio(r);

        // Detalle de lo aplicado (documentos/cuotas del cobro)
        @SuppressWarnings("unchecked")
        List<Object[]> det = em.createNativeQuery(
            "SELECT cd.secuencia, cd.documento, cd.monto FROM cobro_detalle cd WHERE cd.cobro=:id ORDER BY cd.secuencia")
            .setParameter("id", cobroId).getResultList();
        if (!det.isEmpty()) {
            List<String[]> filas = new ArrayList<>();
            for (Object[] d : det) {
                filas.add(new String[]{ str(d[0]), str(d[1]),
                        GS.format(d[2] == null ? BigDecimal.ZERO : new BigDecimal(d[2].toString())) });
            }
            pdf.tabla(r, new String[]{"#", "Documento", "Monto"}, filas, new float[]{1, 3, 2});
            pdf.espacio(r);
        }

        BigDecimal monto = c[3] == null ? BigDecimal.ZERO : new BigDecimal(c[3].toString());
        pdf.titulo(r, "TOTAL: " + GS.format(monto) + " " + str(c[9]));
        return pdf.cerrar(r);
    }

    private String empresaNombre(Long emp) {
        try {
            Object n = em.createNativeQuery("SELECT nombre FROM v_persona WHERE persona=:t")
                    .setParameter("t", emp).getSingleResult();
            return n == null ? "SGInmo" : n.toString();
        } catch (RuntimeException e) {
            return "SGInmo";
        }
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    public static class FilaCobro {
        public Long cobro;
        public java.time.LocalDate fecha;
        public BigDecimal monto = BigDecimal.ZERO;
        public String estado, cliente, moneda;
        public Long getCobro() { return cobro; }
        public java.time.LocalDate getFecha() { return fecha; }
        public BigDecimal getMonto() { return monto; }
        public String getEstado() { return estado; }
        public String getCliente() { return cliente; }
        public String getMoneda() { return moneda; }
    }
}
