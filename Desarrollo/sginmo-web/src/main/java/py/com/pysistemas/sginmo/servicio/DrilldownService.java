package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import py.com.one.core.NegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REQ-0074 - Drill-down de evidencia para los indicadores del dashboard. @AislarTenant + RLS. Los enlaces
 * NO aceptan JPQL/SQL libre: solo una WHITELIST de claves de indicador, cada una con su consulta
 * parametrizada. Ademas exige el permiso del MODULO ORIGEN (ver dashboard no basta para ver datos
 * sensibles). Cada detalle muestra fecha/hora de generacion y los filtros aplicados; exportable a CSV.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional
public class DrilldownService {

    /** Whitelist: clave -> permiso (pantalla,accion) del modulo de origen. */
    private static final Map<String, String[]> PERMISO = Map.of(
        "cobros", new String[]{"caja", "VER"},
        "mora", new String[]{"cobranza", "VER"},
        "ingresos", new String[]{"ingresos-egresos", "VER"},
        "egresos", new String[]{"ingresos-egresos", "VER"},
        "ocupacion", new String[]{"ocupacion", "VER"},
        "vacancia", new String[]{"ocupacion", "VER"},
        "rentabilidad_activo", new String[]{"rentabilidad", "VER"});

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    public boolean claveValida(String clave) { return clave != null && PERMISO.containsKey(clave); }

    /** Evidencia de una clave del dashboard con filtros tipados (anti-injection: solo whitelist). */
    public Detalle detalle(String clave, LocalDate desde, LocalDate hasta, Long moneda, Long sucursal, Long refId) {
        return detalle(clave, desde, hasta, moneda, sucursal, refId, null);
    }

    /** Variante con filtro de aplicacion (para el drill de rentabilidad por tipo, REQ-0071 obs 277). */
    public Detalle detalle(String clave, LocalDate desde, LocalDate hasta, Long moneda, Long sucursal, Long refId, String aplicacion) {
        String[] permiso = PERMISO.get(clave);
        if (permiso == null) throw new NegocioException("Indicador de detalle no valido");
        // Permiso del modulo origen (no basta ver dashboard).
        autorizacion.exigir(permiso[0], permiso[1]);
        Detalle d = new Detalle();
        d.clave = clave;
        d.generado = java.time.LocalDateTime.now().format(FMT_TS);
        d.filtros = filtrosDesc(desde, hasta, moneda, sucursal, refId)
                + (aplicacion != null && !aplicacion.isBlank() ? "   |   Tipo: " + aplicacion : "");
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) { d.titulo = "Sin empresa"; return d; }
        switch (clave) {
            case "cobros" -> cobros(d, desde, hasta, moneda, sucursal);
            case "mora" -> mora(d, hasta, moneda, sucursal);
            case "ingresos" -> ingEgr(d, "INGRESO", desde, hasta, aplicacion);
            case "egresos" -> ingEgr(d, "EGRESO", desde, hasta, aplicacion);
            case "ocupacion" -> propiedades(d, hasta, true);
            case "vacancia" -> propiedades(d, hasta, false);
            case "rentabilidad_activo" -> rentabilidadActivo(d, refId, desde, hasta);
            default -> throw new NegocioException("Indicador de detalle no valido");
        }
        return d;
    }

    private void cobros(Detalle d, LocalDate desde, LocalDate hasta, Long moneda, Long sucursal) {
        d.titulo = "Cobros del periodo";
        d.columnas = new String[]{"Fecha", "Cliente", "Forma de pago", "Moneda", "Monto"};
        String suc = sucursal != null ? " AND c.sucursal = :suc" : "";
        String mon = moneda != null ? " AND c.moneda = :mon" : "";
        Query q = em.createNativeQuery(
            "SELECT c.fecha, vp.nombre, fp.descripcion, mo.descripcion, c.monto FROM cobro c"
          + " LEFT JOIN v_persona vp ON vp.persona=c.persona LEFT JOIN forma_pago fp ON fp.forma_pago=c.forma_pago"
          + " LEFT JOIN moneda mo ON mo.moneda=c.moneda"
          + " WHERE c.estado='ACTIVO' AND c.fecha BETWEEN :d AND :h" + mon + suc + " ORDER BY c.fecha")
            .setParameter("d", desde).setParameter("h", hasta);
        if (moneda != null) q.setParameter("mon", moneda);
        if (sucursal != null) q.setParameter("suc", sucursal);
        for (Object[] f : rows(q)) d.filas.add(new String[]{ fecha(f[0]), s(f[1]), s(f[2]), s(f[3]), gs(f[4]) });
    }

    private void mora(Detalle d, LocalDate hasta, Long moneda, Long sucursal) {
        d.titulo = "Mora - cuotas vencidas";
        d.columnas = new String[]{"Cliente", "Operacion", "Cuota", "Vencimiento", "Moneda", "Saldo"};
        String suc = sucursal != null ? " AND o.sucursal = :suc" : "";
        String mon = moneda != null ? " AND cc.moneda = :mon" : "";
        Query q = em.createNativeQuery(
            "SELECT vp.nombre, o.operacion, cc.numero_cuota, cc.fecha_vencimiento, mo.descripcion, cc.saldo"
          + " FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " LEFT JOIN v_persona vp ON vp.persona=o.cliente LEFT JOIN moneda mo ON mo.moneda=cc.moneda"
          + " WHERE cc.estado='PENDIENTE' AND cc.fecha_vencimiento < :h" + mon + suc
          + " ORDER BY cc.fecha_vencimiento")
            .setParameter("h", hasta);
        if (moneda != null) q.setParameter("mon", moneda);
        if (sucursal != null) q.setParameter("suc", sucursal);
        for (Object[] f : rows(q)) d.filas.add(new String[]{ s(f[0]), s(f[1]), s(f[2]), fecha(f[3]), s(f[4]), gs(f[5]) });
    }

    private void ingEgr(Detalle d, String tipo, LocalDate desde, LocalDate hasta, String aplicacion) {
        d.titulo = ("INGRESO".equals(tipo) ? "Ingresos" : "Egresos") + " del periodo"
                + (aplicacion != null && !aplicacion.isBlank() ? " - " + aplicacion : "");
        d.columnas = new String[]{"Fecha", "Tipo/aplicacion", "Articulo", "Monto", "Observacion"};
        boolean filtraApl = aplicacion != null && !aplicacion.isBlank();
        Query q = em.createNativeQuery(
            "SELECT ie.fecha, COALESCE(art.aplicacion,'OTROS'), art.descripcion, ie.monto, ie.observacion"
          + " FROM ingreso_egreso ie LEFT JOIN articulo art ON art.articulo=ie.articulo"
          + " WHERE ie.tipo=:t AND ie.estado='CANCELADO' AND ie.fecha BETWEEN :d AND :h"
          + (filtraApl ? " AND COALESCE(art.aplicacion,'OTROS') = :apl" : "") + " ORDER BY ie.fecha")
            .setParameter("t", tipo).setParameter("d", desde).setParameter("h", hasta);
        if (filtraApl) q.setParameter("apl", aplicacion);
        for (Object[] f : rows(q)) d.filas.add(new String[]{ fecha(f[0]), s(f[1]), s(f[2]), gs(f[3]), s(f[4]) });
    }

    private void propiedades(Detalle d, LocalDate hasta, boolean ocupadas) {
        d.titulo = ocupadas ? "Propiedades ocupadas" : "Propiedades vacantes";
        d.columnas = new String[]{"Propiedad", "Tipo", "Direccion", "Precio alquiler"};
        String cond = ocupadas ? "IN" : "NOT IN";
        Query q = em.createNativeQuery(
            "SELECT a.nombre, COALESCE(te.descripcion,'-'), COALESCE(a.direccion,''), a.precio_alquiler"
          + " FROM activo a LEFT JOIN entidad te ON te.entidad=a.tipo"
          + " WHERE a.precio_alquiler > 0 AND a.estado <> 'VENDIDA' AND a.activo " + cond + " ("
          + "   SELECT DISTINCT o.activo FROM operacion o WHERE o.tipo_operacion='ALQUILER'"
          + "     AND o.fecha_inicio_contrato <= :h AND (o.fecha_finalizacion IS NULL OR o.fecha_finalizacion > :h))"
          + " ORDER BY a.precio_alquiler DESC, a.nombre")
            .setParameter("h", hasta);
        for (Object[] f : rows(q)) d.filas.add(new String[]{ s(f[0]), s(f[1]), s(f[2]), gs(f[3]) });
    }

    private void rentabilidadActivo(Detalle d, Long refId, LocalDate desde, LocalDate hasta) {
        if (refId == null) throw new NegocioException("Falta el activo");
        d.titulo = "Rentabilidad - movimientos del activo";
        d.columnas = new String[]{"Fecha", "Tipo", "Aplicacion", "Monto"};
        Query q = em.createNativeQuery(
            "SELECT ie.fecha, ie.tipo, COALESCE(art.aplicacion,'OTROS'), ie.monto FROM ingreso_egreso ie"
          + " LEFT JOIN articulo art ON art.articulo=ie.articulo"
          + " WHERE ie.activo=:a AND ie.estado='CANCELADO' AND ie.fecha BETWEEN :d AND :h ORDER BY ie.fecha")
            .setParameter("a", refId).setParameter("d", desde).setParameter("h", hasta);
        for (Object[] f : rows(q)) d.filas.add(new String[]{ fecha(f[0]), s(f[1]), s(f[2]), gs(f[3]) });
    }

    private String filtrosDesc(LocalDate desde, LocalDate hasta, Long moneda, Long sucursal, Long refId) {
        List<String> l = new ArrayList<>();
        if (desde != null && hasta != null) l.add("Periodo: " + fecha(desde) + " a " + fecha(hasta));
        if (moneda != null) l.add("Moneda: #" + moneda);
        if (sucursal != null) l.add("Sucursal: #" + sucursal);
        if (refId != null) l.add("Ref: #" + refId);
        return String.join("   |   ", l);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(Query q) { return q.setMaxResults(2000).getResultList(); }

    private static final java.time.format.DateTimeFormatter FMT_TS =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final java.text.DecimalFormat GS;
    static {
        java.text.DecimalFormatSymbols x = new java.text.DecimalFormatSymbols(new java.util.Locale("es", "PY"));
        x.setGroupingSeparator('.'); GS = new java.text.DecimalFormat("#,##0", x);
    }
    private static String s(Object o) { return o == null ? "" : o.toString(); }
    private static String gs(Object o) { return o == null ? "0" : GS.format(new BigDecimal(o.toString())); }
    private static String fecha(Object o) {
        if (o == null) return "";
        LocalDate d = (o instanceof java.sql.Date sd) ? sd.toLocalDate()
                : (o instanceof LocalDate ld ? ld : LocalDate.parse(o.toString()));
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    // ── DTO ──
    public static class Detalle {
        public String clave, titulo, generado, filtros;
        public String[] columnas = new String[0];
        public List<String[]> filas = new ArrayList<>();
        public String getClave() { return clave; }
        public String getTitulo() { return titulo; }
        public String getGenerado() { return generado; }
        public String getFiltros() { return filtros; }
        public String[] getColumnas() { return columnas; }
        public List<String[]> getFilas() { return filas; }
    }
}
