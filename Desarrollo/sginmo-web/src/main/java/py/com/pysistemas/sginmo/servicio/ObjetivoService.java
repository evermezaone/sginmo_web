package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0073 - Objetivos gerenciales BSC/BCM: metas configurables con calculo AUTOMATICO de valor actual,
 * brecha, % de cumplimiento y semaforo. @AislarTenant + RLS (V50). El calculo esta centralizado aca
 * (no en XHTML/Bean) reutilizando el motor de metricas / ocupacion / rentabilidad. ABM con permisos
 * separados (objetivos/*) y auditoria funcional (REQ-0067). Baja logica (no se borran).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class ObjetivoService {

    public static final String PANTALLA = "objetivos";

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;
    @jakarta.inject.Inject
    private AuditoriaFuncionalService auditoria;
    @jakarta.inject.Inject
    private DashboardMetricasService metricas;
    @jakarta.inject.Inject
    private OcupacionService ocupacion;
    @jakarta.inject.Inject
    private RentabilidadService rentabilidad;

    // ── Consulta con calculo automatico ───────────────────────────────────────

    public List<Objetivo> listar(boolean soloActivos) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Objetivo> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        Query q = em.createNativeQuery(
            "SELECT objetivo_gerencial, indicador, descripcion, meta, unidad, sentido, periodo, alcance,"
          + " alcance_ref, moneda, umbral_adv, vigencia_desde, vigencia_hasta, estado"
          + " FROM objetivo_gerencial" + (soloActivos ? " WHERE estado='ACTIVO'" : "")
          + " ORDER BY estado, indicador");
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) {
            Objetivo o = mapear(f);
            calcular(o);   // valor actual + brecha + cumplimiento + semaforo
            out.add(o);
        }
        return out;
    }

    public Objetivo porId(Long id) {
        autorizacion.exigir(PANTALLA, "VER");
        Query q = em.createNativeQuery(
            "SELECT objetivo_gerencial, indicador, descripcion, meta, unidad, sentido, periodo, alcance,"
          + " alcance_ref, moneda, umbral_adv, vigencia_desde, vigencia_hasta, estado"
          + " FROM objetivo_gerencial WHERE objetivo_gerencial = :id").setParameter("id", id);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        if (rows.isEmpty()) return null;
        Objetivo o = mapear(rows.get(0));
        calcular(o);
        return o;
    }

    /** Calcula el valor actual segun el indicador y deriva brecha/cumplimiento/semaforo. */
    public void calcular(Objetivo o) {
        o.valorActual = valorActual(o);
        BigDecimal meta = o.meta == null ? BigDecimal.ZERO : o.meta;
        BigDecimal adv = o.umbralAdv != null ? o.umbralAdv
                : ("MINIMO".equals(o.sentido) ? meta.multiply(new BigDecimal("0.90"))
                                              : meta.multiply(new BigDecimal("1.10")));
        boolean minimo = "MINIMO".equals(o.sentido);
        // Brecha (cuanto falta / cuanto sobra).
        o.brecha = minimo ? meta.subtract(o.valorActual).max(BigDecimal.ZERO)
                          : o.valorActual.subtract(meta).max(BigDecimal.ZERO);
        // Cumplimiento %.
        if (meta.signum() == 0) {
            o.cumplimientoPct = null;
        } else if (minimo) {
            o.cumplimientoPct = o.valorActual.multiply(CIEN).divide(meta, 2, RoundingMode.HALF_UP);
        } else {
            o.cumplimientoPct = o.valorActual.signum() <= 0 ? CIEN
                    : meta.multiply(CIEN).divide(o.valorActual, 2, RoundingMode.HALF_UP).min(new BigDecimal("999.99"));
        }
        // Semaforo.
        if (minimo) {
            o.semaforo = o.valorActual.compareTo(meta) >= 0 ? "OK"
                    : (o.valorActual.compareTo(adv) >= 0 ? "ADVERTENCIA" : "CRITICO");
        } else {
            o.semaforo = o.valorActual.compareTo(meta) <= 0 ? "OK"
                    : (o.valorActual.compareTo(adv) <= 0 ? "ADVERTENCIA" : "CRITICO");
        }
        // Para ocupacion: cuantas unidades faltan alquilar (evidencia directa).
        if ("ocupacion".equals(o.indicador)) {
            o.faltanUnidades = ocupacion.resumen().getBrecha();
        }
        o.drillClave = drillDe(o.indicador);   // obs 286: evidencia por indicador
        // obs 293: el enlace de evidencia debe usar el MISMO rango que el calculo del objetivo.
        LocalDate[] rp = rangoPeriodo(o);
        o.rangoDesde = rp[0].toString();
        o.rangoHasta = rp[1].toString();
    }

    private BigDecimal valorActual(Objetivo o) {
        LocalDate[] r = rangoPeriodo(o);   // obs 283: rango segun el periodo del objetivo
        LocalDate d = r[0], h = r[1];
        return switch (o.indicador) {
            case "ocupacion" -> ocupacion.resumen().getOcupacionPct();          // snapshot (a hoy)
            case "vacancia_maxima" -> BigDecimal.valueOf(ocupacion.resumen().getVacantes());
            case "cobro_mensual" -> metricas.valorEnRango(DashboardMetricasService.COBROS, d, h, o.moneda, o.alcanceSucursal());
            case "mora_maxima" -> metricas.valorEnRango(DashboardMetricasService.MORA, d, h, o.moneda, o.alcanceSucursal());
            case "contratos_nuevos" -> metricas.valorEnRango(DashboardMetricasService.CONTRATOS_NUEVOS, d, h, null, o.alcanceSucursal());
            case "rentabilidad_minima" -> rentabilidad.resumen(d, h).getNeto();
            case "egresos_maximos" -> rentabilidad.resumen(d, h).getTotalEgresos();
            default -> BigDecimal.ZERO;
        };
    }

    /** obs 283: rango [desde, hasta] segun el periodo del objetivo (MENSUAL/TRIMESTRAL/ANUAL/PERSONALIZADO). */
    private LocalDate[] rangoPeriodo(Objetivo o) {
        LocalDate hoy = LocalDate.now();
        String per = o.periodo == null ? "MENSUAL" : o.periodo;
        LocalDate desde = switch (per) {
            case "TRIMESTRAL" -> hoy.withDayOfMonth(1).minusMonths((hoy.getMonthValue() - 1) % 3);
            case "ANUAL" -> hoy.withDayOfYear(1);
            case "PERSONALIZADO" -> o.vigenciaDesde != null ? o.vigenciaDesde : hoy.withDayOfMonth(1);
            default -> hoy.withDayOfMonth(1);   // MENSUAL
        };
        LocalDate hasta = "PERSONALIZADO".equals(per) && o.vigenciaHasta != null && o.vigenciaHasta.isBefore(hoy)
                ? o.vigenciaHasta : hoy;
        return new LocalDate[]{desde, hasta};
    }

    /** obs 286: clave de evidencia (whitelist REQ-0074) por indicador; null si no tiene detalle. */
    private static String drillDe(String indicador) {
        return switch (indicador) {
            case "ocupacion" -> "ocupacion";
            case "vacancia_maxima" -> "vacancia";
            case "cobro_mensual" -> "cobros";
            case "mora_maxima" -> "mora";
            case "egresos_maximos" -> "egresos";
            default -> null;   // rentabilidad_minima / contratos_nuevos: sin detalle directo
        };
    }

    /** obs 285: historial de mediciones del objetivo. */
    public List<Medicion> mediciones(Long id) {
        autorizacion.exigir(PANTALLA, "VER");
        List<Medicion> out = new ArrayList<>();
        Query q = em.createNativeQuery(
            "SELECT periodo_desde, periodo_hasta, valor, cumplimiento_pct, semaforo, fecha"
          + " FROM objetivo_medicion WHERE objetivo=:o ORDER BY fecha DESC").setParameter("o", id).setMaxResults(60);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) {
            Medicion m = new Medicion();
            m.periodoDesde = f[0] == null ? null : ((java.sql.Date) f[0]).toLocalDate();
            m.periodoHasta = f[1] == null ? null : ((java.sql.Date) f[1]).toLocalDate();
            m.valor = dec(f[2]); m.cumplimientoPct = f[3] == null ? null : dec(f[3]); m.semaforo = str(f[4]);
            m.fecha = f[5] == null ? "" : fmtTs(f[5]);
            out.add(m);
        }
        return out;
    }

    /** obs 284: sucursales del tenant para el alcance SUCURSAL del ABM. */
    public List<Suc> sucursales() {
        autorizacion.exigir(PANTALLA, "VER");
        List<Suc> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT sucursal, descripcion FROM sucursal WHERE estado='ACTIVO' AND tenant=:t ORDER BY descripcion")
            .setParameter("t", emp).getResultList();
        for (Object[] f : rows) out.add(new Suc(((Number) f[0]).longValue(), str(f[1])));
        return out;
    }

    private static String fmtTs(Object o) {
        java.time.LocalDateTime dt;
        if (o instanceof java.time.OffsetDateTime odt) dt = odt.toLocalDateTime();
        else if (o instanceof java.sql.Timestamp ts) dt = ts.toLocalDateTime();
        else if (o instanceof java.time.LocalDateTime l) dt = l;
        else return o.toString();
        return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // ── ABM (permisos separados + auditoria) ───────────────────────────────────

    public Long guardar(Objetivo o) {
        boolean nuevo = o.id == null;
        autorizacion.exigir(PANTALLA, nuevo ? "CREAR" : "EDITAR");
        validar(o);
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp))
            throw new NegocioException("Seleccione una empresa");
        if (nuevo) {
            Object idNuevo = em.createNativeQuery(
                "INSERT INTO objetivo_gerencial (tenant, indicador, descripcion, meta, unidad, sentido, periodo,"
              + " alcance, alcance_ref, moneda, umbral_adv, vigencia_desde, vigencia_hasta, estado, usuario_creacion, fecha_creacion)"
              + " VALUES (:t,:ind,:desc,:meta,:uni,:sen,:per,:alc,:aref,:mon,:adv,:vd,:vh,'ACTIVO',:usr, now())"
              + " RETURNING objetivo_gerencial")
                .setParameter("t", emp).setParameter("ind", o.indicador).setParameter("desc", o.descripcion)
                .setParameter("meta", o.meta).setParameter("uni", o.unidad).setParameter("sen", o.sentido)
                .setParameter("per", o.periodo == null ? "MENSUAL" : o.periodo).setParameter("alc", o.alcance == null ? "EMPRESA" : o.alcance)
                .setParameter("aref", o.alcanceRef).setParameter("mon", o.moneda).setParameter("adv", o.umbralAdv)
                .setParameter("vd", o.vigenciaDesde == null ? LocalDate.now() : o.vigenciaDesde).setParameter("vh", o.vigenciaHasta)
                .setParameter("usr", sesion.codigoUsuario()).getSingleResult();
            o.id = ((Number) idNuevo).longValue();
            auditoria.registrarAlta("objetivo_gerencial", o.id, PANTALLA);
        } else {
            int n = em.createNativeQuery(
                "UPDATE objetivo_gerencial SET descripcion=:desc, meta=:meta, unidad=:uni, sentido=:sen, periodo=:per,"
              + " alcance=:alc, alcance_ref=:aref, moneda=:mon, umbral_adv=:adv, vigencia_desde=:vd, vigencia_hasta=:vh,"
              + " usuario_modificacion=:usr, fecha_modificacion=now() WHERE objetivo_gerencial=:id")
                .setParameter("desc", o.descripcion).setParameter("meta", o.meta).setParameter("uni", o.unidad)
                .setParameter("sen", o.sentido).setParameter("per", o.periodo).setParameter("alc", o.alcance)
                .setParameter("aref", o.alcanceRef).setParameter("mon", o.moneda).setParameter("adv", o.umbralAdv)
                .setParameter("vd", o.vigenciaDesde).setParameter("vh", o.vigenciaHasta)
                .setParameter("usr", sesion.codigoUsuario()).setParameter("id", o.id).executeUpdate();
            if (n == 0) throw new NegocioException("El objetivo no existe o no pertenece a su empresa");
            auditoria.registrar("objetivo_gerencial", o.id, AuditoriaFuncionalService.EDITAR, PANTALLA, "edicion de objetivo");
        }
        return o.id;
    }

    public void cambiarEstado(Long id, String estadoNuevo) {
        boolean reactivar = "ACTIVO".equals(estadoNuevo);
        autorizacion.exigir(PANTALLA, reactivar ? "REACTIVAR" : "INACTIVAR");
        int n = em.createNativeQuery(
            "UPDATE objetivo_gerencial SET estado=:e, usuario_modificacion=:u, fecha_modificacion=now() WHERE objetivo_gerencial=:id")
            .setParameter("e", estadoNuevo).setParameter("u", sesion.codigoUsuario()).setParameter("id", id).executeUpdate();
        if (n == 0) throw new NegocioException("El objetivo no existe o no pertenece a su empresa");
        auditoria.registrar("objetivo_gerencial", id,
                reactivar ? AuditoriaFuncionalService.REACTIVAR : AuditoriaFuncionalService.INACTIVAR, PANTALLA, "cambio de estado a " + estadoNuevo);
    }

    /** Snapshot de la medicion actual (historial de evolucion). Permiso EDITAR. */
    public void registrarMedicion(Long id) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        Objetivo o = porId(id);
        if (o == null) throw new NegocioException("El objetivo no existe");
        LocalDate[] r = rangoPeriodo(o);   // obs 283: el historial usa el rango del periodo del objetivo
        em.createNativeQuery(
            "INSERT INTO objetivo_medicion (tenant, objetivo, periodo_desde, periodo_hasta, valor, cumplimiento_pct, semaforo, fecha)"
          + " VALUES (:t,:o,:pd,:ph,:val,:cmp,:sem, now())")
            .setParameter("t", tenant.actual()).setParameter("o", id)
            .setParameter("pd", r[0]).setParameter("ph", r[1])
            .setParameter("val", o.valorActual).setParameter("cmp", o.cumplimientoPct).setParameter("sem", o.semaforo)
            .executeUpdate();
    }

    private void validar(Objetivo o) {
        if (o.indicador == null || o.indicador.isBlank()) throw new NegocioException("El indicador es obligatorio");
        if (o.descripcion == null || o.descripcion.isBlank()) throw new NegocioException("La descripcion es obligatoria");
        if (o.meta == null) throw new NegocioException("La meta es obligatoria");
        if (o.unidad == null) throw new NegocioException("La unidad es obligatoria");
        if ("PORCENTAJE".equals(o.unidad) && (o.meta.signum() < 0 || o.meta.compareTo(CIEN) > 0))
            throw new NegocioException("Un porcentaje debe estar entre 0 y 100");
        if (!"PORCENTAJE".equals(o.unidad) && o.meta.signum() < 0)
            throw new NegocioException("La meta no puede ser negativa");
        if ("MONTO".equals(o.unidad) && o.moneda == null)
            throw new NegocioException("Para una meta en monto debe elegir la moneda (no se mezclan monedas)");
        if (o.sentido == null) throw new NegocioException("Indique el sentido (llegar a minimo / no superar maximo)");
        // obs 294: un objetivo PERSONALIZADO exige rango de vigencia coherente.
        if ("PERSONALIZADO".equals(o.periodo)) {
            if (o.vigenciaDesde == null || o.vigenciaHasta == null)
                throw new NegocioException("Para un periodo personalizado indique vigencia desde y hasta");
            if (o.vigenciaHasta.isBefore(o.vigenciaDesde))
                throw new NegocioException("La vigencia hasta no puede ser anterior a la vigencia desde");
        }
    }

    private Objetivo mapear(Object[] f) {
        Objetivo o = new Objetivo();
        o.id = ((Number) f[0]).longValue();
        o.indicador = str(f[1]); o.descripcion = str(f[2]);
        o.meta = dec(f[3]); o.unidad = str(f[4]); o.sentido = str(f[5]); o.periodo = str(f[6]);
        o.alcance = str(f[7]); o.alcanceRef = f[8] == null ? null : ((Number) f[8]).longValue();
        o.moneda = f[9] == null ? null : ((Number) f[9]).longValue();
        o.umbralAdv = f[10] == null ? null : dec(f[10]);
        o.vigenciaDesde = f[11] == null ? null : ((java.sql.Date) f[11]).toLocalDate();
        o.vigenciaHasta = f[12] == null ? null : ((java.sql.Date) f[12]).toLocalDate();
        o.estado = str(f[13]);
        return o;
    }

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static LocalDate inicioMes() { return LocalDate.now().withDayOfMonth(1); }
    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static BigDecimal dec(Object o) { return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString()); }

    /** Indicadores disponibles (para el combo del ABM). */
    public static List<String> indicadores() {
        return List.of("ocupacion", "cobro_mensual", "mora_maxima", "rentabilidad_minima",
                "contratos_nuevos", "egresos_maximos", "vacancia_maxima");
    }

    /** obs 284: alcances IMPLEMENTADOS (no se ofrecen tipo/zona/propietario/responsable, que no tienen efecto). */
    public static List<String> alcances() { return List.of("EMPRESA", "SUCURSAL"); }

    /** DTO de medicion historica (obs 285). */
    public static class Medicion {
        public LocalDate periodoDesde, periodoHasta;
        public BigDecimal valor = BigDecimal.ZERO, cumplimientoPct;
        public String semaforo, fecha;
        public LocalDate getPeriodoDesde() { return periodoDesde; }
        public LocalDate getPeriodoHasta() { return periodoHasta; }
        public BigDecimal getValor() { return valor; }
        public BigDecimal getCumplimientoPct() { return cumplimientoPct; }
        public String getSemaforo() { return semaforo; }
        public String getFecha() { return fecha; }
    }

    /** DTO de sucursal para el ABM (obs 284). */
    public static class Suc {
        public final Long id; public final String nombre;
        public Suc(Long id, String nombre) { this.id = id; this.nombre = nombre; }
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
    }

    // ── DTO ──

    public static class Objetivo {
        public Long id, alcanceRef, moneda;
        public String indicador, descripcion, unidad, sentido, periodo, alcance, estado;
        public BigDecimal meta, umbralAdv;
        public LocalDate vigenciaDesde, vigenciaHasta;
        // Calculados:
        public BigDecimal valorActual = BigDecimal.ZERO, brecha = BigDecimal.ZERO, cumplimientoPct;
        public String semaforo = "OK", drillClave;
        public String rangoDesde, rangoHasta;   // obs 293: rango real (ISO) para el enlace de evidencia
        public long faltanUnidades;
        public String getDrillClave() { return drillClave; }
        public boolean isTieneEvidencia() { return drillClave != null; }
        public String getRangoDesde() { return rangoDesde; }
        public String getRangoHasta() { return rangoHasta; }

        public Long alcanceSucursal() { return "SUCURSAL".equals(alcance) ? alcanceRef : null; }

        public Long getId() { return id; }
        public String getIndicador() { return indicador; }
        public void setIndicador(String v) { indicador = v; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String v) { descripcion = v; }
        public BigDecimal getMeta() { return meta; }
        public void setMeta(BigDecimal v) { meta = v; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String v) { unidad = v; }
        public String getSentido() { return sentido; }
        public void setSentido(String v) { sentido = v; }
        public String getPeriodo() { return periodo; }
        public void setPeriodo(String v) { periodo = v; }
        public String getAlcance() { return alcance; }
        public void setAlcance(String v) { alcance = v; }
        public Long getAlcanceRef() { return alcanceRef; }
        public void setAlcanceRef(Long v) { alcanceRef = v; }
        public Long getMoneda() { return moneda; }
        public void setMoneda(Long v) { moneda = v; }
        public BigDecimal getUmbralAdv() { return umbralAdv; }
        public void setUmbralAdv(BigDecimal v) { umbralAdv = v; }
        public LocalDate getVigenciaDesde() { return vigenciaDesde; }
        public void setVigenciaDesde(LocalDate v) { vigenciaDesde = v; }
        public LocalDate getVigenciaHasta() { return vigenciaHasta; }
        public void setVigenciaHasta(LocalDate v) { vigenciaHasta = v; }
        public String getEstado() { return estado; }
        public BigDecimal getValorActual() { return valorActual; }
        public BigDecimal getBrecha() { return brecha; }
        public BigDecimal getCumplimientoPct() { return cumplimientoPct; }
        public String getSemaforo() { return semaforo; }
        public long getFaltanUnidades() { return faltanUnidades; }
    }
}
