package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REQ-0067 - Auditoria funcional visible: registra y consulta el historial de cambios de registros
 * sensibles (quien, que, valor anterior/nuevo, cuando, modulo, motivo). Es de NEGOCIO -> @AislarTenant
 * para que la RLS (V46) impida ver cambios de otra empresa. La auditoria es inmutable (sin UPDATE/DELETE).
 *
 * Seguridad: NUNCA guarda secretos. Los campos cuyo nombre sugiere credencial (password/hash/token/
 * clave/secret/salt) se enmascaran a "***" antes de persistir el valor.
 *
 * Uso desde los servicios de negocio (ejemplos ya cableados: desbloqueo de usuario). El patron para
 * un ABM sensible es: capturar el estado "antes" (Map campo->valor), aplicar el cambio, y llamar a
 * registrarCambios(entidad, id, modulo, motivo, antes, despues).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class AuditoriaFuncionalService {

    /** Acciones validas (deben coincidir con el CHECK de V46). */
    public static final String CREAR = "CREAR", EDITAR = "EDITAR", INACTIVAR = "INACTIVAR",
            REACTIVAR = "REACTIVAR", ANULAR = "ANULAR", COBRAR = "COBRAR", DESCUENTO = "DESCUENTO",
            LIQUIDAR = "LIQUIDAR", REGENERAR = "REGENERAR", DESBLOQUEAR = "DESBLOQUEAR", OTRO = "OTRO";

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private TenantContext tenant;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;

    // ── Registro ──────────────────────────────────────────────────────────────

    /** Registra una accion sobre un registro sin diff de campo (alta, inactivacion, cobro, etc.). */
    public void registrar(String entidad, Object registroId, String accion, String modulo, String motivo) {
        insertar(entidad, registroId, accion, null, null, null, modulo, motivo);
    }

    /** Registra el alta de un registro sensible. */
    public void registrarAlta(String entidad, Object registroId, String modulo) {
        insertar(entidad, registroId, CREAR, null, null, null, modulo, null);
    }

    /** Registra una inactivacion; el motivo es obligatorio (regla de negocio de maestros sensibles). */
    public void registrarInactivacion(String entidad, Object registroId, String modulo, String motivo) {
        if (motivo == null || motivo.isBlank()) throw new NegocioException("El motivo de inactivacion es obligatorio");
        insertar(entidad, registroId, INACTIVAR, null, null, null, modulo, motivo.trim());
    }

    /** Registra una reactivacion. */
    public void registrarReactivacion(String entidad, Object registroId, String modulo, String motivo) {
        insertar(entidad, registroId, REACTIVAR, null, null, null, modulo, motivo);
    }

    /**
     * Compara dos snapshots (campo -> valor) y registra una fila EDITAR por cada campo que cambio.
     * Solo audita campos presentes en ambos mapas; enmascara los sensibles. Devuelve la cantidad de
     * cambios auditados. Si no hubo cambios, no escribe nada.
     */
    public int registrarCambios(String entidad, Object registroId, String modulo, String motivo,
                                Map<String, ?> antes, Map<String, ?> despues) {
        if (antes == null || despues == null) return 0;
        int n = 0;
        for (Map.Entry<String, ?> e : despues.entrySet()) {
            String campo = e.getKey();
            if (!antes.containsKey(campo)) continue;
            Object vAntes = antes.get(campo);
            Object vNuevo = e.getValue();
            if (Objects.equals(vAntes, vNuevo)) continue;
            insertar(entidad, registroId, EDITAR, campo,
                    valorSeguro(campo, vAntes), valorSeguro(campo, vNuevo), modulo, motivo);
            n++;
        }
        return n;
    }

    /** Inserta una fila de auditoria (fail-fast: la auditoria funcional es parte de la transaccion de negocio). */
    private void insertar(String entidad, Object registroId, String accion, String campo,
                          String valorAnterior, String valorNuevo, String modulo, String motivo) {
        Long emp = tenant.actual();
        long t = (emp == null) ? TenantContext.GLOBAL : emp;   // acciones sin empresa -> GLOBAL (-1)
        em.createNativeQuery(
            "INSERT INTO auditoria_funcional (tenant, entidad, registro_id, accion, campo,"
          + " valor_anterior, valor_nuevo, modulo, motivo, usuario_codigo, fecha)"
          + " VALUES (:t,:ent,:rid,:acc,:campo,:va,:vn,:mod,:mot,:usr, now())")
            .setParameter("t", t)
            .setParameter("ent", recorta(entidad, 60))
            .setParameter("rid", registroId == null ? null : recorta(registroId.toString(), 60))
            .setParameter("acc", accion)
            .setParameter("campo", recorta(campo, 60))
            .setParameter("va", valorAnterior)
            .setParameter("vn", valorNuevo)
            .setParameter("mod", recorta(modulo, 60))
            .setParameter("mot", recorta(motivo, 300))
            .setParameter("usr", usuario())
            .executeUpdate();
    }

    // ── Consulta (permiso separado: auditoria/VER) ─────────────────────────────

    /** Historial de un registro puntual (para el boton/pestaña "Historial" de un ABM). */
    public List<Fila> historial(String entidad, Object registroId) {
        autorizacion.exigir("auditoria", "VER");
        Query q = em.createNativeQuery(
            "SELECT fecha, usuario_codigo, entidad, registro_id, accion, campo, valor_anterior,"
          + " valor_nuevo, modulo, motivo FROM auditoria_funcional"
          + " WHERE entidad=:ent AND registro_id=:rid ORDER BY fecha DESC, auditoria_funcional DESC")
            .setParameter("ent", entidad)
            .setParameter("rid", registroId == null ? null : registroId.toString())
            .setMaxResults(500);
        return mapear(q);
    }

    /**
     * Consulta filtrable de la auditoria (pantalla Auditoria). Filtros opcionales: fecha desde/hasta
     * (yyyy-MM-dd), usuario, accion, campo, entidad. La RLS ya limita al tenant activo.
     */
    public List<Fila> consultar(String fechaDesde, String fechaHasta, String usuario,
                                String accion, String campo, String entidad) {
        autorizacion.exigir("auditoria", "VER");
        StringBuilder sql = new StringBuilder(
            "SELECT fecha, usuario_codigo, entidad, registro_id, accion, campo, valor_anterior,"
          + " valor_nuevo, modulo, motivo FROM auditoria_funcional WHERE 1=1");
        Map<String, Object> p = new LinkedHashMap<>();
        if (noBlank(fechaDesde)) { sql.append(" AND fecha >= CAST(:fd AS date)"); p.put("fd", fechaDesde.trim()); }
        if (noBlank(fechaHasta)) { sql.append(" AND fecha < CAST(:fh AS date) + INTERVAL '1 day'"); p.put("fh", fechaHasta.trim()); }
        if (noBlank(usuario))    { sql.append(" AND usuario_codigo ILIKE :usr"); p.put("usr", "%" + usuario.trim() + "%"); }
        if (noBlank(accion))     { sql.append(" AND accion = :acc"); p.put("acc", accion.trim()); }
        if (noBlank(campo))      { sql.append(" AND campo ILIKE :campo"); p.put("campo", "%" + campo.trim() + "%"); }
        if (noBlank(entidad))    { sql.append(" AND entidad ILIKE :ent"); p.put("ent", "%" + entidad.trim() + "%"); }
        sql.append(" ORDER BY fecha DESC, auditoria_funcional DESC");
        Query q = em.createNativeQuery(sql.toString());
        for (Map.Entry<String, Object> e : p.entrySet()) q.setParameter(e.getKey(), e.getValue());
        q.setMaxResults(500);
        return mapear(q);
    }

    /** Entidades sensibles con auditoria registrada (para poblar el combo de filtro). */
    public List<String> entidadesConAuditoria() {
        autorizacion.exigir("auditoria", "VER");
        @SuppressWarnings("unchecked")
        List<String> l = em.createNativeQuery(
            "SELECT DISTINCT entidad FROM auditoria_funcional ORDER BY entidad").getResultList();
        return l;
    }

    // ── Utilidades ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Fila> mapear(Query q) {
        List<Object[]> rows = q.getResultList();
        List<Fila> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            out.add(new Fila(fmt(r[0]), str(r[1]), str(r[2]), str(r[3]), str(r[4]),
                    str(r[5]), str(r[6]), str(r[7]), str(r[8]), str(r[9])));
        }
        return out;
    }

    private String usuario() {
        try {
            String u = sesion.codigoUsuario();
            return (u == null || u.isBlank()) ? "sistema" : u;
        } catch (RuntimeException sinSesion) {
            return "sistema";
        }
    }

    /** Enmascara valores de campos sensibles; convierte el resto a texto. */
    private static String valorSeguro(String campo, Object valor) {
        if (esSensible(campo)) return "***";
        return valor == null ? null : recorta(valor.toString(), 4000);
    }

    /** Un campo es sensible si su nombre sugiere credencial/secreto. */
    private static boolean esSensible(String campo) {
        if (campo == null) return false;
        String c = campo.toLowerCase(java.util.Locale.ROOT);
        return c.contains("password") || c.contains("passwd") || c.contains("hash")
            || c.contains("token") || c.contains("clave") || c.contains("secret") || c.contains("salt");
    }

    private static boolean noBlank(String s) { return s != null && !s.isBlank(); }
    private static String str(Object o) { return o == null ? "" : o.toString(); }
    private static String recorta(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String fmt(Object o) {
        if (o == null) return "";
        java.time.LocalDateTime dt;
        if (o instanceof java.time.OffsetDateTime odt) dt = odt.toLocalDateTime();
        else if (o instanceof java.sql.Timestamp ts) dt = ts.toLocalDateTime();
        else if (o instanceof java.time.LocalDateTime l) dt = l;
        else if (o instanceof java.time.Instant ins) dt = java.time.LocalDateTime.ofInstant(ins, java.time.ZoneId.systemDefault());
        else return o.toString();
        return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    // ── DTO ─────────────────────────────────────────────────────────────────────

    /** Fila de auditoria para las grillas. */
    public static class Fila {
        private final String fecha, usuario, entidad, registro, accion, campo, valorAnterior, valorNuevo, modulo, motivo;
        public Fila(String fecha, String usuario, String entidad, String registro, String accion,
                    String campo, String valorAnterior, String valorNuevo, String modulo, String motivo) {
            this.fecha = fecha; this.usuario = usuario; this.entidad = entidad; this.registro = registro;
            this.accion = accion; this.campo = campo; this.valorAnterior = valorAnterior;
            this.valorNuevo = valorNuevo; this.modulo = modulo; this.motivo = motivo;
        }
        public String getFecha() { return fecha; }
        public String getUsuario() { return usuario; }
        public String getEntidad() { return entidad; }
        public String getRegistro() { return registro; }
        public String getAccion() { return accion; }
        public String getCampo() { return campo; }
        public String getValorAnterior() { return valorAnterior; }
        public String getValorNuevo() { return valorNuevo; }
        public String getModulo() { return modulo; }
        public String getMotivo() { return motivo; }
    }
}
