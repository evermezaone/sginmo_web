package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.agenda.AgendaEvento;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.util.List;
import java.util.Map;

/**
 * REQ-0052 - Agenda operativa: tareas manuales + eventos automaticos de vencimiento.
 * @AislarTenant + @Transactional (F5) para que el interceptor fije app.tenant y RLS (V33)
 * filtre por empresa. Los eventos automaticos se generan on-demand con dedup por indice
 * unico (no duplican al reabrir); no hay scheduler EJB en el proyecto (mejora futura).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class AgendaService {

    /** Ventana de anticipacion (dias) para generar vencimientos. Se movera a parametros (REQ-0060). */
    private static final int DIAS_ALERTA = 30;

    private static final Map<String, String> ORDEN = Map.of(
        "fechaEvento", "e.fechaEvento", "prioridad", "e.prioridad",
        "estado", "e.estado", "tipo", "e.tipo", "titulo", "e.titulo");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private TenantContext tenant;

    @jakarta.inject.Inject
    private ParametroConfig parametros;   // REQ-0060: dias de alerta desde parametros, no constante

    // ── Lectura (LazyDataModel) ──

    private StringBuilder base(String filtro, String tipo, String estado, String responsable) {
        StringBuilder jpql = new StringBuilder(" FROM AgendaEvento e WHERE 1=1");
        if (filtro != null && !filtro.isBlank()) {
            jpql.append(" AND (LOWER(e.titulo) LIKE :f OR LOWER(e.descripcion) LIKE :f)");
        }
        if (tipo != null && !tipo.isBlank()) jpql.append(" AND e.tipo = :tipo");
        if (estado != null && !estado.isBlank()) jpql.append(" AND e.estado = :estado");
        if (responsable != null && !responsable.isBlank()) jpql.append(" AND e.responsable = :resp");
        return jpql;
    }

    private void bind(jakarta.persistence.Query q, String filtro, String tipo, String estado, String responsable) {
        if (filtro != null && !filtro.isBlank()) q.setParameter("f", "%" + filtro.toLowerCase() + "%");
        if (tipo != null && !tipo.isBlank()) q.setParameter("tipo", tipo);
        if (estado != null && !estado.isBlank()) q.setParameter("estado", estado);
        if (responsable != null && !responsable.isBlank()) q.setParameter("resp", responsable);
    }

    public long contar(String filtro, String tipo, String estado, String responsable) {
        var q = em.createQuery("SELECT COUNT(e)" + base(filtro, tipo, estado, responsable), Long.class);
        bind(q, filtro, tipo, estado, responsable);
        return q.getSingleResult();
    }

    public List<AgendaEvento> listar(int first, int size, String filtro, String tipo, String estado,
                                     String responsable, String orden, boolean asc) {
        // Map.of(...) es inmutable y lanza NPE con clave null; guardamos el null (sin orden).
        String col = orden == null ? "e.fechaEvento" : ORDEN.getOrDefault(orden, "e.fechaEvento");
        var q = em.createQuery("SELECT e" + base(filtro, tipo, estado, responsable)
                + " ORDER BY " + col + (asc ? " ASC" : " DESC"), AgendaEvento.class);
        bind(q, filtro, tipo, estado, responsable);
        q.setFirstResult(first);
        q.setMaxResults(size);
        return q.getResultList();
    }

    // ── Escrituras ──

    @Transactional
    public AgendaEvento guardar(AgendaEvento e) {
        boolean nuevo = e.getId() == null;
        autorizacion.exigir("agenda", nuevo ? "CREAR" : "EDITAR");
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) {
            throw new NegocioException("Seleccione una empresa para administrar la agenda");
        }
        if (e.getTitulo() == null || e.getTitulo().isBlank()) {
            throw new NegocioException("El titulo es obligatorio");
        }
        if (e.getFechaEvento() == null) {
            throw new NegocioException("La fecha es obligatoria");
        }
        if (e.getTipo() == null || e.getTipo().isBlank()) e.setTipo("TAREA");
        if (e.getEstado() == null || e.getEstado().isBlank()) e.setEstado("PENDIENTE");
        if (e.getPrioridad() == null || e.getPrioridad().isBlank()) e.setPrioridad("MEDIA");
        e.setTenant(emp);
        try {
            if (nuevo) { em.persist(e); } else { e = em.merge(e); }
            em.flush();
            return e;
        } catch (RuntimeException ex) {
            throw ErroresBd.traducir(ex);
        }
    }

    /** Cambia el estado del evento (cerrar/reabrir/en curso), con permiso EDITAR. */
    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        autorizacion.exigir("agenda", "EDITAR");
        AgendaEvento e = em.find(AgendaEvento.class, id);
        if (e == null) throw new NegocioException("El evento no existe");
        e.setEstado(nuevoEstado);
        em.merge(e);
    }

    /** Reasigna el responsable, con permiso EDITAR. */
    @Transactional
    public void reasignar(Long id, String responsable) {
        autorizacion.exigir("agenda", "EDITAR");
        AgendaEvento e = em.find(AgendaEvento.class, id);
        if (e == null) throw new NegocioException("El evento no existe");
        e.setResponsable(responsable);
        em.merge(e);
    }

    /**
     * Genera/actualiza eventos automaticos de vencimiento del tenant actual. Idempotente:
     * el indice unico parcial (tenant, tipo, origen_tabla, origen_id) evita duplicar al reabrir.
     * En contexto global (-1) no genera (veria todas las empresas). Promesas: pendiente REQ-0057.
     */
    @Transactional
    public void generarAutomaticos() {
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) return;
        int diasAlerta = parametros.entero("AGENDA_DIAS_ALERTA", DIAS_ALERTA);   // REQ-0060

        // Cuotas PENDIENTE por vencer o vencidas (la cuota se aisla por JOIN a operacion).
        em.createNativeQuery(
            "INSERT INTO agenda_evento (tenant, tipo, titulo, descripcion, fecha_evento, prioridad, "
          + "  estado, origen_tabla, origen_id, operacion, usuario_creacion, fecha_creacion) "
          + "SELECT o.tenant, 'VENCIMIENTO', 'Cuota ' || cc.numero_cuota || ' por vencer', "
          + "  'Operacion ' || o.operacion || ' - vence ' || cc.fecha_vencimiento, cc.fecha_vencimiento, "
          + "  CASE WHEN cc.fecha_vencimiento < current_date THEN 'ALTA' ELSE 'MEDIA' END, "
          + "  'PENDIENTE', 'cronograma_cuota', cc.cronograma_cuota, o.operacion, 'sistema', now() "
          + "FROM cronograma_cuota cc JOIN operacion o ON o.operacion = cc.operacion "
          + "WHERE cc.estado = 'PENDIENTE' AND cc.fecha_vencimiento <= current_date + :dias "
          + "ON CONFLICT (tenant, tipo, origen_tabla, origen_id) WHERE origen_id IS NOT NULL DO NOTHING")
          .setParameter("dias", diasAlerta).executeUpdate();

        // Contratos VIGENTE proximos a vencer.
        em.createNativeQuery(
            "INSERT INTO agenda_evento (tenant, tipo, titulo, descripcion, fecha_evento, prioridad, "
          + "  estado, origen_tabla, origen_id, operacion, usuario_creacion, fecha_creacion) "
          + "SELECT o.tenant, 'VENCIMIENTO', 'Contrato por vencer (op. ' || o.operacion || ')', "
          + "  'Fin de contrato el ' || o.fecha_fin_contrato, o.fecha_fin_contrato, 'MEDIA', "
          + "  'PENDIENTE', 'operacion', o.operacion, o.operacion, 'sistema', now() "
          + "FROM operacion o WHERE o.estado = 'VIGENTE' AND o.fecha_fin_contrato IS NOT NULL "
          + "  AND o.fecha_fin_contrato BETWEEN current_date AND current_date + :dias "
          + "ON CONFLICT (tenant, tipo, origen_tabla, origen_id) WHERE origen_id IS NOT NULL DO NOTHING")
          .setParameter("dias", diasAlerta).executeUpdate();

        // REQ-0057: promesas de pago incumplidas (PENDIENTE con fecha vencida) -> alerta en la agenda.
        // Idempotente (dedup por origen). Solo lee promesa_pago; no modifica la promesa ni la cuota.
        em.createNativeQuery(
            "INSERT INTO agenda_evento (tenant, tipo, titulo, descripcion, fecha_evento, prioridad, "
          + "  estado, origen_tabla, origen_id, operacion, usuario_creacion, fecha_creacion) "
          + "SELECT pp.tenant, 'PROMESA', 'Promesa de pago incumplida', "
          + "  'Promesa por ' || pp.monto || ' vencio el ' || pp.fecha_promesa, pp.fecha_promesa, 'ALTA', "
          + "  'PENDIENTE', 'promesa_pago', pp.promesa_pago, pp.operacion, 'sistema', now() "
          + "FROM promesa_pago pp WHERE pp.estado = 'PENDIENTE' AND pp.fecha_promesa < current_date "
          + "ON CONFLICT (tenant, tipo, origen_tabla, origen_id) WHERE origen_id IS NOT NULL DO NOTHING")
          .executeUpdate();
    }
}
