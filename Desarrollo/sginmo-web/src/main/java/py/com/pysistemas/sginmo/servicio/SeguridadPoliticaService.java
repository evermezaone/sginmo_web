package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;

import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0064 - Vista/gestion de politicas de seguridad (admin-only): muestra las politicas configurables,
 * los usuarios bloqueados (con desbloqueo auditado) y la auditoria de accesos. Las contrasenas se
 * almacenan solo con hash bcrypt (SeguridadService); nunca reversibles. No mezcla tenant (seguridad global).
 */
@ApplicationScoped
@Transactional
public class SeguridadPoliticaService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;

    /** Politicas de seguridad configurables (parametros del grupo Seguridad). */
    public List<Fila> politicas() {
        List<Fila> out = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> f = em.createNativeQuery(
            "SELECT clave, valor, descripcion FROM parametro_sistema"
          + " WHERE (grupo='Seguridad' OR clave LIKE 'LOGIN%') AND tenant=-1 ORDER BY clave")
            .getResultList();
        for (Object[] r : f) out.add(new Fila(r[0], r[1], r[2], null, null));
        return out;
    }

    /** Usuarios bloqueados o con intentos fallidos acumulados. */
    public List<Fila> usuariosBloqueados() {
        List<Fila> out = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> f = em.createNativeQuery(
            "SELECT usuario, codigo_usuario, bloqueado_hasta, intentos_fallidos FROM usuario"
          + " WHERE (bloqueado_hasta IS NOT NULL AND bloqueado_hasta > now()) OR COALESCE(intentos_fallidos,0) > 0"
          + " ORDER BY codigo_usuario").getResultList();
        for (Object[] r : f) out.add(new Fila(r[0], r[1], fmt(r[2]), r[3], null));
        return out;
    }

    /** Desbloquea un usuario (permiso usuarios/EDITAR) y audita la accion. */
    @Transactional
    public void desbloquear(Long usuarioId) {
        autorizacion.exigir("usuarios", "EDITAR");
        Object cod = em.createNativeQuery("SELECT codigo_usuario FROM usuario WHERE usuario=:id")
                .setParameter("id", usuarioId).getResultStream().findFirst().orElse(null);
        if (cod == null) throw new NegocioException("El usuario no existe");
        em.createNativeQuery("UPDATE usuario SET intentos_fallidos=0, bloqueado_hasta=NULL WHERE usuario=:id")
                .setParameter("id", usuarioId).executeUpdate();
        em.createNativeQuery(
            "INSERT INTO login_evento (usuario_codigo, exito, causa, fecha) VALUES (:u, true, :c, now())")
            .setParameter("u", cod.toString())
            .setParameter("c", "desbloqueo por " + sesion.codigoUsuario()).executeUpdate();
    }

    /** Auditoria de accesos reciente (cap 100). */
    public List<Fila> eventosRecientes() {
        List<Fila> out = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> f = em.createNativeQuery(
            "SELECT fecha, usuario_codigo, exito, causa FROM login_evento ORDER BY login_evento DESC")
            .setMaxResults(100).getResultList();
        for (Object[] r : f) out.add(new Fila(fmt(r[0]), r[1], r[2], r[3], null));
        return out;
    }

    /** Formatea un timestamp de query nativa (OffsetDateTime/Timestamp/LocalDateTime) a texto local. */
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

    /** Fila generica para las 3 grillas (se interpretan las columnas segun el contexto). */
    public static class Fila {
        private final Object a, b, c, d, e;
        public Fila(Object a, Object b, Object c, Object d, Object e) { this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; }
        public Object getA() { return a; }
        public Object getB() { return b; }
        public Object getC() { return c; }
        public Object getD() { return d; }
        public Object getE() { return e; }
        public Long getId() { return a == null ? null : ((Number) a).longValue(); }
    }
}
