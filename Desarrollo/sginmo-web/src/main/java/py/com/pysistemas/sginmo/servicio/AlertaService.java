package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0075 - Alertas gerenciales: detecta desviaciones/brechas accionables desde los objetivos (REQ-0073)
 * y reglas configurables (parametros), sin numeros hardcodeados. @AislarTenant + RLS. Cada alerta trae
 * causa, impacto, accion sugerida, prioridad y enlace a evidencia (REQ-0074). Dedup por (tenant, hash) con
 * alerta ABIERTA; se pueden marcar revisadas/descartadas con motivo y usuario (auditado).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class AlertaService {

    public static final String PANTALLA = "alertas";

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
    private ObjetivoService objetivos;
    @jakarta.inject.Inject
    private RentabilidadService rentabilidad;
    @jakarta.inject.Inject
    private ParametroConfig parametros;

    /** Recalcula las alertas (idempotente por dedup). Requiere ver el modulo. */
    public int generar() {
        autorizacion.exigir(PANTALLA, "VER");
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return 0;
        String periodo = LocalDate.now().withDayOfMonth(1).toString().substring(0, 7);   // YYYY-MM
        int n = 0;

        // 1) Objetivos en riesgo/incumplidos (REQ-0073).
        for (ObjetivoService.Objetivo o : objetivos.listar(true)) {
            if ("OK".equals(o.getSemaforo())) continue;
            boolean critico = "CRITICO".equals(o.getSemaforo());
            String causa = "Objetivo '" + o.getDescripcion() + "': actual " + o.getValorActual()
                    + " vs meta " + o.getMeta() + " (" + o.getSemaforo() + ")";
            String impacto = "ocupacion".equals(o.getIndicador()) && o.getFaltanUnidades() > 0
                    ? "Faltan " + o.getFaltanUnidades() + " propiedades para el objetivo" : "Brecha: " + o.getBrecha();
            String accion = accionObjetivo(o.getIndicador());
            String drill = drillDeIndicador(o.getIndicador());
            n += upsert("OBJETIVO", o.getIndicador(), critico ? "CRITICA" : "ALTA", causa, impacto, accion,
                    drill, null, "OBJETIVO|" + o.getId() + "|" + periodo);
        }

        // 2) Rentabilidad negativa del mes (regla fija).
        BigDecimal neto = rentabilidad.resumen(LocalDate.now().withDayOfMonth(1), LocalDate.now()).getNeto();
        if (neto.signum() < 0) {
            n += upsert("RENTABILIDAD", "rentabilidad", "CRITICA",
                    "Rentabilidad neta negativa en el mes: " + neto.toPlainString(),
                    "Los egresos superan a los ingresos operativos", "Revisar egresos y cobros del periodo",
                    "egresos", null, "RENTABILIDAD|neto|" + periodo);
        }

        // 3) Contratos por vencer en los proximos N dias (parametro configurable).
        int dias = Math.max(1, parametros.entero("CONTRATOS_AVISO_DIAS", 30));
        Number porVencer = (Number) em.createNativeQuery(
            "SELECT COUNT(*) FROM operacion WHERE estado='VIGENTE' AND fecha_fin_contrato IS NOT NULL"
          + " AND fecha_fin_contrato BETWEEN current_date AND current_date + (:d || ' days')::interval")
            .setParameter("d", dias).getSingleResult();
        if (porVencer != null && porVencer.longValue() > 0) {
            n += upsert("CONTRATOS", "contratos_por_vencer", "MEDIA",
                    porVencer.longValue() + " contrato(s) vencen en los proximos " + dias + " dias",
                    "Riesgo de vacancia si no se renuevan", "Gestionar renovacion o reposicion",
                    null, null, "CONTRATOS|porvencer|" + periodo);
        }
        return n;
    }

    /** Inserta la alerta si no existe una ABIERTA con el mismo hash (dedup). Devuelve 1 si inserto. */
    private int upsert(String tipo, String indicador, String prioridad, String causa, String impacto,
                       String accion, String drill, Long drillRef, String hash) {
        long existe = ((Number) em.createNativeQuery(
            "SELECT COUNT(*) FROM alerta_gerencial WHERE hash_dedup=:h AND estado='ABIERTA'")
            .setParameter("h", hash).getSingleResult()).longValue();
        if (existe > 0) return 0;
        em.createNativeQuery(
            "INSERT INTO alerta_gerencial (tenant, tipo, indicador, prioridad, causa, impacto, accion_sugerida,"
          + " drill_clave, drill_ref, hash_dedup, estado, fecha) VALUES"
          + " (:t,:tipo,:ind,:pri,:causa,:imp,:acc,:drill,:dref,:hash,'ABIERTA', now())")
            .setParameter("t", tenant.actual()).setParameter("tipo", tipo).setParameter("ind", indicador)
            .setParameter("pri", prioridad).setParameter("causa", recorta(causa, 300)).setParameter("imp", recorta(impacto, 300))
            .setParameter("acc", recorta(accion, 300)).setParameter("drill", drill).setParameter("dref", drillRef)
            .setParameter("hash", hash).executeUpdate();
        return 1;
    }

    /** Alertas abiertas ordenadas por prioridad. */
    public List<Alerta> listar() {
        autorizacion.exigir(PANTALLA, "VER");
        List<Alerta> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(emp)) return out;
        Query q = em.createNativeQuery(
            "SELECT alerta_gerencial, tipo, indicador, prioridad, causa, impacto, accion_sugerida, drill_clave, drill_ref, fecha"
          + " FROM alerta_gerencial WHERE estado='ABIERTA'"
          + " ORDER BY CASE prioridad WHEN 'CRITICA' THEN 1 WHEN 'ALTA' THEN 2 WHEN 'MEDIA' THEN 3 ELSE 4 END, fecha DESC");
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] f : rows) {
            Alerta a = new Alerta();
            a.id = ((Number) f[0]).longValue();
            a.tipo = s(f[1]); a.indicador = s(f[2]); a.prioridad = s(f[3]);
            a.causa = s(f[4]); a.impacto = s(f[5]); a.accion = s(f[6]);
            a.drillClave = s(f[7]); a.drillRef = f[8] == null ? null : ((Number) f[8]).longValue();
            out.add(a);
        }
        return out;
    }

    public void cerrar(Long id, String estado, String motivo) {
        autorizacion.exigir(PANTALLA, "EDITAR");
        if (!"REVISADA".equals(estado) && !"DESCARTADA".equals(estado)) throw new NegocioException("Estado invalido");
        if ("DESCARTADA".equals(estado) && (motivo == null || motivo.isBlank()))
            throw new NegocioException("Indique el motivo para descartar la alerta");
        int n = em.createNativeQuery(
            "UPDATE alerta_gerencial SET estado=:e, motivo_cierre=:m, usuario_cierre=:u, fecha_cierre=now() WHERE alerta_gerencial=:id AND estado='ABIERTA'")
            .setParameter("e", estado).setParameter("m", motivo).setParameter("u", sesion.codigoUsuario()).setParameter("id", id).executeUpdate();
        if (n == 0) throw new NegocioException("La alerta no existe o ya fue cerrada");
        auditoria.registrar("alerta_gerencial", id,
                "DESCARTADA".equals(estado) ? AuditoriaFuncionalService.INACTIVAR : AuditoriaFuncionalService.EDITAR,
                PANTALLA, estado + (motivo == null ? "" : ": " + motivo));
    }

    private static String accionObjetivo(String indicador) {
        return switch (indicador) {
            case "ocupacion" -> "Publicar y gestionar las propiedades vacantes prioritarias";
            case "cobro_mensual" -> "Intensificar gestion de cobranza del periodo";
            case "mora_maxima" -> "Gestionar la cartera vencida y promesas de pago";
            case "rentabilidad_minima" -> "Revisar egresos y mejorar cobros";
            case "egresos_maximos" -> "Controlar y reducir egresos";
            case "contratos_nuevos" -> "Impulsar nuevas operaciones";
            case "vacancia_maxima" -> "Reducir vacancia colocando propiedades";
            default -> "Revisar el indicador";
        };
    }
    private static String drillDeIndicador(String indicador) {
        return switch (indicador) {
            case "ocupacion" -> "ocupacion";
            case "vacancia_maxima" -> "vacancia";
            case "cobro_mensual" -> "cobros";
            case "mora_maxima" -> "mora";
            case "egresos_maximos", "rentabilidad_minima" -> "egresos";
            default -> null;
        };
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }
    private static String recorta(String s, int max) { return s == null ? null : (s.length() <= max ? s : s.substring(0, max)); }

    public static class Alerta {
        public Long id, drillRef;
        public String tipo, indicador, prioridad, causa, impacto, accion, drillClave;
        public Long getId() { return id; }
        public Long getDrillRef() { return drillRef; }
        public String getTipo() { return tipo; }
        public String getIndicador() { return indicador; }
        public String getPrioridad() { return prioridad; }
        public String getCausa() { return causa; }
        public String getImpacto() { return impacto; }
        public String getAccion() { return accion; }
        public String getDrillClave() { return drillClave; }
        public boolean isTieneEvidencia() { return drillClave != null && !drillClave.isBlank(); }
    }
}
