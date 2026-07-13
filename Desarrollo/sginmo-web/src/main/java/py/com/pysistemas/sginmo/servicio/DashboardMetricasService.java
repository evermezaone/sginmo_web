package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REQ-0069 - Motor de metricas gerenciales COMPARATIVAS (solo lectura). @AislarTenant + @Transactional
 * para que la RLS (V28) filtre por empresa. Devuelve, por indicador, el valor del periodo actual y sus
 * comparativos (periodo anterior, mismo periodo del anio anterior, YTD y YTD del anio anterior) con
 * variaciones absolutas/porcentuales y direccion semantica (MEJORA/EMPEORA/NEUTRO/NA).
 *
 * Reglas: los montos NUNCA mezclan monedas (cobros/mora se filtran por moneda; ingreso_egreso es de la
 * moneda base de la empresa). Todo en BigDecimal. Cada KPI expone metadata de evidencia (drillKey +
 * filtros) para el drill-down (REQ-0074). Las consultas son agregadas y acotadas por rango de fecha.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional
public class DashboardMetricasService {

    public static final String PANTALLA = "dashboard-gerencial";

    // Indicadores (drillKey estable; whitelist para REQ-0074).
    public static final String COBROS = "cobros";
    public static final String MORA = "mora";
    public static final String INGRESOS = "ingresos";
    public static final String EGRESOS = "egresos";
    public static final String RENTABILIDAD = "rentabilidad";
    public static final String OCUPACION = "ocupacion";
    public static final String VACANCIA = "vacancia";
    public static final String CONTRATOS_NUEVOS = "contratos_nuevos";
    public static final String CONTRATOS_FINALIZADOS = "contratos_finalizados";

    /** Indicadores donde "mas es mejor" (el resto: mas es peor). */
    private static final java.util.Set<String> MAS_ES_MEJOR = java.util.Set.of(
            COBROS, INGRESOS, RENTABILIDAD, OCUPACION, CONTRATOS_NUEVOS);
    /** Indicadores monetarios (requieren moneda; no mezclan monedas). */
    private static final java.util.Set<String> MONETARIOS = java.util.Set.of(
            COBROS, MORA, INGRESOS, EGRESOS, RENTABILIDAD);
    /** Indicadores de foto a fecha (snapshot) vs de flujo por rango. */
    private static final java.util.Set<String> SNAPSHOT = java.util.Set.of(MORA, OCUPACION, VACANCIA);
    /** Indicadores con evidencia navegable (drill-down REQ-0074): coinciden con la whitelist de DrilldownService. */
    private static final java.util.Set<String> NAVEGABLES = java.util.Set.of(
            COBROS, MORA, INGRESOS, EGRESOS, OCUPACION, VACANCIA);

    private static final Map<String, String> ETIQUETA = new LinkedHashMap<>();
    static {
        ETIQUETA.put(COBROS, "Cobros");
        ETIQUETA.put(MORA, "Mora (saldo vencido)");
        ETIQUETA.put(INGRESOS, "Ingresos");
        ETIQUETA.put(EGRESOS, "Egresos");
        ETIQUETA.put(RENTABILIDAD, "Rentabilidad (neto)");
        ETIQUETA.put(OCUPACION, "Ocupacion %");
        ETIQUETA.put(VACANCIA, "Vacancia (unidades)");
        ETIQUETA.put(CONTRATOS_NUEVOS, "Contratos nuevos");
        ETIQUETA.put(CONTRATOS_FINALIZADOS, "Contratos finalizados");
    }

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    // ── API publica ──────────────────────────────────────────────────────────

    /**
     * Comparativos de todos los indicadores para el mes de referencia (por defecto el mes en curso).
     * moneda: obligatoria para los indicadores monetarios (si es null, esos KPIs quedan "sin moneda").
     */
    public List<Comparativo> comparativos(LocalDate mesRef, Long moneda, Long sucursal) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Comparativo> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        LocalDate ref = mesRef == null ? LocalDate.now() : mesRef;
        Periodos p = Periodos.para(ref, LocalDate.now());
        for (String ind : ETIQUETA.keySet()) {
            out.add(comparativo(ind, p, moneda, sucursal));
        }
        return out;
    }

    /**
     * Obs 272: privado. Un indicador puntual con sus comparativos. La entrada publica autorizada es
     * comparativos() (que ya exige dashboard-gerencial/VER); este helper no se expone.
     */
    private Comparativo comparativo(String indicador, Periodos p, Long moneda, Long sucursal) {
        Comparativo c = new Comparativo();
        c.indicador = indicador;
        c.etiqueta = ETIQUETA.getOrDefault(indicador, indicador);
        c.monetario = MONETARIOS.contains(indicador);
        c.periodoDesc = p.actual.desde + " a " + p.actual.hasta;
        c.monedaId = c.monetario ? moneda : null;
        c.sucursalId = sucursal;
        c.drillKey = indicador;
        // obs 275: clave de evidencia navegable (null si el indicador no tiene detalle en la whitelist).
        c.detalleClave = NAVEGABLES.contains(indicador) ? indicador : null;
        boolean sinMoneda = c.monetario && moneda == null;
        if (sinMoneda) {
            c.aplicable = false;   // no se calcula un monto sin moneda (no mezclar monedas)
            return c;
        }
        c.aplicable = true;
        c.actual = valor(indicador, p.actual, moneda, sucursal);
        c.periodoAnterior = valor(indicador, p.anterior, moneda, sucursal);
        c.mismoMesAnioAnterior = valor(indicador, p.mismoMesAnioAnterior, moneda, sucursal);
        c.ytd = valor(indicador, p.ytd, moneda, sucursal);
        c.ytdAnterior = valor(indicador, p.ytdAnterior, moneda, sucursal);
        boolean masMejor = MAS_ES_MEJOR.contains(indicador);
        c.mom = Variacion.entre(c.actual, c.periodoAnterior, masMejor);
        c.yoy = Variacion.entre(c.actual, c.mismoMesAnioAnterior, masMejor);
        c.ytdVar = Variacion.entre(c.ytd, c.ytdAnterior, masMejor);
        return c;
    }

    /**
     * Serie mensual de un indicador para graficos de evolucion (REQ-0070). Los {@code meses} terminan en
     * el mes de {@code refHasta} (el filtro de periodo del dashboard), no en hoy (obs 274), asi los
     * graficos respetan el periodo seleccionado. Cada mes se acota a refHasta si cae dentro.
     */
    public List<Punto> serieMensual(String indicador, Long moneda, Long sucursal, int meses, LocalDate refHasta) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Punto> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        if (MONETARIOS.contains(indicador) && moneda == null) return out;   // no mezcla monedas
        LocalDate ref = refHasta == null ? LocalDate.now() : refHasta;
        LocalDate primer = ref.withDayOfMonth(1).minusMonths(Math.max(1, meses) - 1L);
        for (int i = 0; i < Math.max(1, meses); i++) {
            LocalDate ini = primer.plusMonths(i);
            LocalDate fin = ini.withDayOfMonth(ini.lengthOfMonth());
            if (fin.isAfter(ref)) fin = ref;
            out.add(new Punto(ini, valor(indicador, new Rango(ini, fin), moneda, sucursal)));
        }
        return out;
    }

    /**
     * Obs 272: API INTERNA (package-private) para los servicios gerenciales del mismo paquete
     * (ObjetivoService, AlertaService), que YA exigen su propio permiso (objetivos/alertas VER) antes
     * de llamar. No es una entrada publica del dashboard; por eso no repite dashboard-gerencial/VER.
     * Valor de un indicador para el mes en curso.
     */
    BigDecimal valorMesActual(String indicador, Long moneda, Long sucursal) {
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return BigDecimal.ZERO;
        Periodos p = Periodos.para(LocalDate.now(), LocalDate.now());
        return valor(indicador, p.actual, moneda, sucursal);
    }

    // ── Calculo por indicador y rango ─────────────────────────────────────────

    private BigDecimal valor(String indicador, Rango r, Long moneda, Long sucursal) {
        return switch (indicador) {
            case COBROS -> cobros(r, moneda, sucursal);
            case MORA -> moraAFecha(r.hasta, moneda, sucursal);
            case INGRESOS -> flujoIngEgr("INGRESO", r, sucursal);
            case EGRESOS -> flujoIngEgr("EGRESO", r, sucursal);
            case RENTABILIDAD -> flujoIngEgr("INGRESO", r, sucursal).subtract(flujoIngEgr("EGRESO", r, sucursal));
            // Obs 273: ocupacion/vacancia NO aplican sucursal: el universo (activo) no tiene sucursal, y
            // filtrar solo los ocupados por operacion.sucursal daria un % falso. Se calcula por tenant.
            case OCUPACION -> ocupacionPct(r.hasta);
            case VACANCIA -> BigDecimal.valueOf(vacantes(r.hasta));
            case CONTRATOS_NUEVOS -> BigDecimal.valueOf(contratosNuevos(r, sucursal));
            case CONTRATOS_FINALIZADOS -> BigDecimal.valueOf(contratosFinalizados(r, sucursal));
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal cobros(Rango r, Long moneda, Long sucursal) {
        String suc = sucursal != null ? " AND c.sucursal = :suc" : "";
        Query q = em.createNativeQuery(
            "SELECT COALESCE(SUM(c.monto),0) FROM cobro c WHERE c.estado='ACTIVO'"
          + " AND c.fecha BETWEEN :d AND :h AND c.moneda = :mon" + suc);
        q.setParameter("d", r.desde).setParameter("h", r.hasta).setParameter("mon", moneda);
        if (sucursal != null) q.setParameter("suc", sucursal);
        return dec(q);
    }

    /** Mora = saldo pendiente vencido a la fecha de corte (snapshot), en la moneda pedida. */
    private BigDecimal moraAFecha(LocalDate corte, Long moneda, Long sucursal) {
        String suc = sucursal != null ? " AND o.sucursal = :suc" : "";
        Query q = em.createNativeQuery(
            "SELECT COALESCE(SUM(cc.saldo),0) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion"
          + " WHERE cc.estado='PENDIENTE' AND cc.fecha_vencimiento < :corte AND cc.moneda = :mon" + suc);
        q.setParameter("corte", corte).setParameter("mon", moneda);
        if (sucursal != null) q.setParameter("suc", sucursal);
        return dec(q);
    }

    /** Ingresos/egresos realizados (base caja: estado CANCELADO) por rango. ingreso_egreso es moneda base. */
    private BigDecimal flujoIngEgr(String tipo, Rango r, Long sucursal) {
        // ingreso_egreso no tiene sucursal directa; se acota por operacion cuando hay filtro de sucursal.
        String suc = sucursal != null
                ? " AND ie.operacion IN (SELECT o.operacion FROM operacion o WHERE o.sucursal = :suc)" : "";
        Query q = em.createNativeQuery(
            "SELECT COALESCE(SUM(ie.monto),0) FROM ingreso_egreso ie"
          + " WHERE ie.tipo = :tipo AND ie.estado='CANCELADO' AND ie.fecha BETWEEN :d AND :h" + suc);
        q.setParameter("tipo", tipo).setParameter("d", r.desde).setParameter("h", r.hasta);
        if (sucursal != null) q.setParameter("suc", sucursal);
        return dec(q);
    }

    // ── Ocupacion / vacancia (universo alquilable por tenant; sin sucursal, obs 273; ver REQ-0072) ──

    /** Activos alquilables: con precio_alquiler > 0 y no vendidos. El activo no tiene sucursal. */
    private long alquilables() {
        Query q = em.createNativeQuery(
            "SELECT COUNT(*) FROM activo WHERE precio_alquiler > 0 AND estado <> 'VENDIDA'");
        return num(q);
    }

    /** Ocupados a la fecha: activos con una operacion de ALQUILER VIGENTE que cubre esa fecha (obs 279). */
    private long ocupados(LocalDate fecha) {
        Query q = em.createNativeQuery(
            "SELECT COUNT(DISTINCT o.activo) FROM operacion o"
          + " WHERE o.tipo_operacion='ALQUILER' AND o.estado='VIGENTE' AND o.fecha_inicio_contrato <= :f"
          + " AND (o.fecha_finalizacion IS NULL OR o.fecha_finalizacion > :f)"
          + " AND o.activo IN (SELECT a.activo FROM activo a WHERE a.precio_alquiler > 0 AND a.estado <> 'VENDIDA')");
        q.setParameter("f", fecha);
        return num(q);
    }

    private BigDecimal ocupacionPct(LocalDate fecha) {
        long alq = alquilables();
        if (alq == 0) return BigDecimal.ZERO;
        long ocu = ocupados(fecha);
        return BigDecimal.valueOf(ocu).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(alq), 2, RoundingMode.HALF_UP);
    }

    private long vacantes(LocalDate fecha) {
        return Math.max(0, alquilables() - ocupados(fecha));
    }

    private long contratosNuevos(Rango r, Long sucursal) {
        String suc = sucursal != null ? " AND o.sucursal = :suc" : "";
        Query q = em.createNativeQuery(
            "SELECT COUNT(*) FROM operacion o WHERE o.fecha_operacion BETWEEN :d AND :h" + suc);
        q.setParameter("d", r.desde).setParameter("h", r.hasta);
        if (sucursal != null) q.setParameter("suc", sucursal);
        return num(q);
    }

    private long contratosFinalizados(Rango r, Long sucursal) {
        String suc = sucursal != null ? " AND o.sucursal = :suc" : "";
        Query q = em.createNativeQuery(
            "SELECT COUNT(*) FROM operacion o WHERE o.fecha_finalizacion BETWEEN :d AND :h" + suc);
        q.setParameter("d", r.desde).setParameter("h", r.hasta);
        if (sucursal != null) q.setParameter("suc", sucursal);
        return num(q);
    }

    // ── Utilidades ─────────────────────────────────────────────────────────────

    private long num(Query q) { Object r = q.getSingleResult(); return r == null ? 0 : ((Number) r).longValue(); }
    private BigDecimal dec(Query q) { Object r = q.getSingleResult(); return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString()); }

    // ── Modelo de periodos ─────────────────────────────────────────────────────

    /** Rango de fechas inclusivo. */
    public record Rango(LocalDate desde, LocalDate hasta) { }

    /** Punto de una serie mensual: etiqueta MM/yyyy + valor. */
    public static final class Punto {
        public final LocalDate mes; public final BigDecimal valor; public final String etiqueta;
        public Punto(LocalDate mes, BigDecimal valor) {
            this.mes = mes; this.valor = valor == null ? BigDecimal.ZERO : valor;
            this.etiqueta = String.format("%02d/%04d", mes.getMonthValue(), mes.getYear());
        }
        public LocalDate getMes() { return mes; }
        public BigDecimal getValor() { return valor; }
        public String getEtiqueta() { return etiqueta; }
    }

    /**
     * Conjunto de periodos comparables para un mes de referencia. Si el mes de referencia es el mes en
     * curso, los comparativos usan la MISMA cantidad de dias transcurridos (mes-a-mes justo); si es un
     * mes cerrado, se comparan meses completos. El YTD compara el mismo rango calendario ano contra ano.
     */
    public static final class Periodos {
        public Rango actual, anterior, mismoMesAnioAnterior, ytd, ytdAnterior;
        public boolean enCurso;

        public static Periodos para(LocalDate mesRef, LocalDate hoy) {
            Periodos p = new Periodos();
            LocalDate inicio = mesRef.withDayOfMonth(1);
            boolean enCurso = mesRef.getYear() == hoy.getYear() && mesRef.getMonthValue() == hoy.getMonthValue();
            p.enCurso = enCurso;
            LocalDate finActual = enCurso ? hoy : inicio.withDayOfMonth(inicio.lengthOfMonth());
            p.actual = new Rango(inicio, finActual);
            int dias = finActual.getDayOfMonth();   // dias transcurridos del mes (1..31)

            // Mes anterior: mismo tramo de dias si el actual esta en curso; si no, mes completo.
            LocalDate inicioAnt = inicio.minusMonths(1);
            p.anterior = tramo(inicioAnt, enCurso, dias);
            // Mismo mes del anio anterior.
            LocalDate inicioAA = inicio.minusYears(1);
            p.mismoMesAnioAnterior = tramo(inicioAA, enCurso, dias);
            // YTD: 1-ene del anio de ref hasta el fin del periodo actual; y su equivalente el anio anterior.
            p.ytd = new Rango(inicio.withDayOfYear(1), finActual);
            LocalDate finAA = finActual.minusYears(1);
            p.ytdAnterior = new Rango(finAA.withDayOfYear(1), finAA);
            return p;
        }

        /** Tramo de un mes: [inicio, inicio+dias-1] acotado al fin de mes; o mes completo si !enCurso. */
        private static Rango tramo(LocalDate inicioMes, boolean enCurso, int dias) {
            LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());
            if (!enCurso) return new Rango(inicioMes, finMes);
            LocalDate hasta = inicioMes.withDayOfMonth(Math.min(dias, inicioMes.lengthOfMonth()));
            return new Rango(inicioMes, hasta);
        }
    }

    // ── Variacion semantica ─────────────────────────────────────────────────────

    public enum Direccion { MEJORA, EMPEORA, NEUTRO, NA }

    public static final class Variacion {
        public BigDecimal absoluta = BigDecimal.ZERO;
        public BigDecimal porcentual;   // null = NA (sin base comparable)
        public Direccion direccion = Direccion.NA;

        static Variacion entre(BigDecimal actual, BigDecimal base, boolean masEsMejor) {
            Variacion v = new Variacion();
            if (actual == null) actual = BigDecimal.ZERO;
            if (base == null) base = BigDecimal.ZERO;
            v.absoluta = actual.subtract(base);
            int signo = v.absoluta.signum();
            if (base.signum() == 0) {
                v.porcentual = null;   // NA: sin base comparable (no infinito ni cero falso)
                v.direccion = signo == 0 ? Direccion.NEUTRO
                        : (signo > 0 == masEsMejor ? Direccion.MEJORA : Direccion.EMPEORA);
                return v;
            }
            v.porcentual = v.absoluta.multiply(BigDecimal.valueOf(100))
                    .divide(base.abs(), 2, RoundingMode.HALF_UP);
            v.direccion = signo == 0 ? Direccion.NEUTRO
                    : (signo > 0 == masEsMejor ? Direccion.MEJORA : Direccion.EMPEORA);
            return v;
        }

        public BigDecimal getAbsoluta() { return absoluta; }
        public BigDecimal getPorcentual() { return porcentual; }
        public String getDireccion() { return direccion.name(); }
        public boolean isAplica() { return porcentual != null; }
    }

    // ── DTO de salida ─────────────────────────────────────────────────────────

    public static class Comparativo {
        public String indicador, etiqueta, periodoDesc, drillKey, detalleClave;
        public boolean monetario, aplicable;
        public Long monedaId, sucursalId;
        public BigDecimal actual = BigDecimal.ZERO, periodoAnterior = BigDecimal.ZERO,
                mismoMesAnioAnterior = BigDecimal.ZERO, ytd = BigDecimal.ZERO, ytdAnterior = BigDecimal.ZERO;
        public Variacion mom, yoy, ytdVar;

        public String getIndicador() { return indicador; }
        public String getEtiqueta() { return etiqueta; }
        public String getPeriodoDesc() { return periodoDesc; }
        public String getDrillKey() { return drillKey; }
        public String getDetalleClave() { return detalleClave; }
        public boolean isNavegable() { return detalleClave != null; }
        public boolean isMonetario() { return monetario; }
        public boolean isAplicable() { return aplicable; }
        public Long getMonedaId() { return monedaId; }
        public Long getSucursalId() { return sucursalId; }
        public BigDecimal getActual() { return actual; }
        public BigDecimal getPeriodoAnterior() { return periodoAnterior; }
        public BigDecimal getMismoMesAnioAnterior() { return mismoMesAnioAnterior; }
        public BigDecimal getYtd() { return ytd; }
        public BigDecimal getYtdAnterior() { return ytdAnterior; }
        public Variacion getMom() { return mom; }
        public Variacion getYoy() { return yoy; }
        public Variacion getYtdVar() { return ytdVar; }
    }
}
