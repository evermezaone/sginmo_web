package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;
import py.com.one.core.NegocioException;
import py.com.one.security.servicio.CorreoService;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-0078 - Autenticacion del portal externo de socios (CI/RUC + OTP + password de persona).
 *
 * NO usa {@code @AislarTenant}: el tenant proviene de la empresa elegida en el login publico y se
 * fija explicitamente ({@link #fijarTenant}) para que la RLS (V28) acote todas las consultas a esa
 * empresa. La identidad efectiva del portal se toma de la sesion creada tras OTP/password, nunca de
 * un id de persona enviado en el request.
 *
 * Seguridad: OTP y password se guardan hasheados con bcrypt (jamas en texto plano); mensajes
 * genericos que no revelan si el documento existe; limite de intentos y bloqueo temporal contra
 * fuerza bruta; auditoria de cada evento (solicitud/validacion OTP, login, cambio de clave, logout).
 */
@ApplicationScoped
@Transactional
public class PortalAuthService {

    private static final SecureRandom RND = new SecureRandom();
    /** Mensaje unico para toda falla de acceso: no revela si el documento existe. */
    private static final String GENERICO =
        "No pudimos validar los datos. Verifique la empresa y el documento e intente nuevamente.";
    /** Mensaje unico para toda falla de validacion de OTP (obs 303): no distingue documento
     *  inexistente de codigo vencido/erroneo/sin-otp; el motivo real queda solo en auditoria. */
    private static final String OTP_GENERICO =
        "El codigo no es valido o expiro. Si lo necesita, solicite uno nuevo e intente otra vez.";

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @Inject
    private CorreoService correo;

    // ── API publica ──────────────────────────────────────────────────────────

    // ── REQ-0102: acceso por documento SIN exponer la lista de empresas ─────────────────────────

    /** Tenants donde la persona (por documento) es elegible para el portal (CLIENTE/PROPIETARIO). */
    private List<Long> tenantsElegibles(String documento) {
        fijarTenant(-1L);
        @SuppressWarnings("unchecked")
        List<Object> rows = em.createNativeQuery(
            "SELECT DISTINCT pr.tenant FROM persona p"
          + " JOIN persona_rol pr ON pr.persona = p.persona AND pr.estado = 'ACTIVO'"
          + " JOIN entidad e ON e.entidad = pr.rol AND e.lista = 'ROLES_PERSONA'"
          + "   AND e.codigo IN ('CLIENTE','PROPIETARIO') AND e.tenant IN (-1, pr.tenant)"
          + " WHERE p.numero_documento = :doc AND p.estado = 'ACTIVO'")
            .setParameter("doc", documento.trim()).getResultList();
        List<Long> out = new ArrayList<>();
        for (Object r : rows) if (r != null) out.add(((Number) r).longValue());
        return out;
    }

    /**
     * REQ-0102: login por documento + password, resolviendo la(s) empresa(s) automaticamente (sin
     * exponer la lista). Devuelve un acceso por cada empresa donde el documento es elegible, tiene
     * credencial y la clave coincide. La contrasena se comparte entre las empresas del socio.
     */
    public List<Acceso> loginPasswordMulti(String documento, String password, String ip, String ua) {
        if (documento == null || documento.isBlank() || password == null || password.isBlank())
            throw new NegocioException(GENERICO);
        String doc = documento.trim();
        fijarTenant(-1L);   // superadmin: ver credenciales de todas las empresas (RLS)
        @SuppressWarnings("unchecked")
        List<Object[]> cands = em.createNativeQuery(
            "SELECT c.tenant, emp.nombre, c.password_hash, (c.bloqueado_hasta > now()) AS bloqueado, p.persona"
          + " FROM persona p"
          + " JOIN persona_portal_credencial c ON c.persona = p.persona"
          + " JOIN persona emp ON emp.persona = c.tenant"
          + " WHERE p.numero_documento = :doc AND p.estado = 'ACTIVO'"
          + "   AND EXISTS (SELECT 1 FROM persona_rol pr JOIN entidad e ON e.entidad = pr.rol"
          + "               WHERE pr.persona = p.persona AND pr.tenant = c.tenant AND pr.estado = 'ACTIVO'"
          + "                 AND e.lista = 'ROLES_PERSONA' AND e.codigo IN ('CLIENTE','PROPIETARIO') AND e.tenant IN (-1, c.tenant))"
          + " ORDER BY emp.nombre")
            .setParameter("doc", doc).getResultList();
        if (cands.isEmpty()) { auditar(null, null, "LOGIN_FALLIDO", "doc:" + doc, ip, ua); throw new NegocioException(GENERICO); }

        List<Acceso> ok = new ArrayList<>();
        boolean bloqueadoAlguno = false;
        for (Object[] r : cands) {
            Long tn = ((Number) r[0]).longValue();
            String empNombre = (String) r[1];
            String hash = (String) r[2];
            boolean bloq = Boolean.TRUE.equals(r[3]);
            Long personaId = ((Number) r[4]).longValue();
            if (bloq) { bloqueadoAlguno = true; auditar(tn, personaId, "LOGIN_FALLIDO", "bloqueado", ip, ua); continue; }
            if (hash != null && BCrypt.checkpw(password.trim(), hash)) {
                em.createNativeQuery("UPDATE persona_portal_credencial SET intentos_fallidos = 0, bloqueado_hasta = NULL"
                  + " WHERE tenant = :t AND persona = :p").setParameter("t", tn).setParameter("p", personaId).executeUpdate();
                fijarTenant(tn);
                Persona pe = buscarElegible(tn, doc);
                fijarTenant(-1L);
                if (pe != null) {
                    Acceso a = new Acceso();
                    a.tenant = tn; a.empresa = empNombre; a.identidad = identidadDe(pe, tn);
                    ok.add(a);
                    auditar(tn, personaId, "LOGIN", null, ip, ua);
                }
            } else {
                registrarFallo(tn, personaId);
                auditar(tn, personaId, "LOGIN_FALLIDO", "password-incorrecta", ip, ua);
            }
        }
        if (ok.isEmpty()) {
            if (bloqueadoAlguno) throw new NegocioException("Acceso bloqueado temporalmente por intentos fallidos. Reintente mas tarde.");
            throw new NegocioException(GENERICO);
        }
        return ok;
    }

    /** REQ-0102: primer ingreso / recuperacion por documento: envia OTP en cada empresa elegible del socio. */
    public void solicitarOtpDoc(String documento, String proposito, String ip, String ua) {
        if (documento == null || documento.isBlank()) return;   // comportamiento uniforme (no revela)
        for (Long t : tenantsElegibles(documento)) {
            try { solicitarOtp(t, documento.trim(), proposito, ip, ua); } catch (RuntimeException ignore) { }
        }
    }

    /** REQ-0102: valida el OTP por documento probando cada empresa elegible; la primera que valida da la identidad. */
    public Identidad validarOtpDoc(String documento, String codigo, String ip, String ua) {
        if (documento == null || documento.isBlank()) throw new NegocioException(OTP_GENERICO);
        for (Long t : tenantsElegibles(documento)) {
            try { return validarOtp(t, documento.trim(), codigo, ip, ua); } catch (NegocioException ignore) { }
        }
        throw new NegocioException(OTP_GENERICO);
    }

    /** REQ-0102: define la MISMA contrasena para todas las empresas elegibles del socio (identidad unica de acceso). */
    public void definirPasswordDoc(String documento, String nueva, String ip, String ua) {
        if (documento == null || documento.isBlank()) throw new NegocioException(GENERICO);
        List<Long> tenants = tenantsElegibles(documento);
        if (tenants.isEmpty()) throw new NegocioException(GENERICO);
        fijarTenant(-1L);
        Object personaObj;
        try {
            personaObj = em.createNativeQuery("SELECT persona FROM persona WHERE numero_documento = :doc AND estado='ACTIVO'")
                .setParameter("doc", documento.trim()).getSingleResult();
        } catch (RuntimeException e) { throw new NegocioException(GENERICO); }
        Long persona = ((Number) personaObj).longValue();
        for (Long t : tenants) definirPassword(t, persona, nueva, ip, ua);
    }

    /** REQ-0102: un acceso resuelto (empresa + identidad) tras login por documento. */
    public static class Acceso implements Serializable {
        public Long tenant; public String empresa; public Identidad identidad;
        public Long getTenant() { return tenant; }
        public String getEmpresa() { return empresa; }
        public Identidad getIdentidad() { return identidad; }
    }

    /** Empresas activas para el selector del login publico (no es dato sensible). */
    public List<Empresa> empresas() {
        fijarTenant(-1L);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
            "SELECT pj.persona, p.nombre FROM persona_juridica pj JOIN persona p ON p.persona = pj.persona"
          + " WHERE p.estado = 'ACTIVO' AND pj.persona <> -1 ORDER BY p.nombre").getResultList();
        List<Empresa> out = new ArrayList<>();
        for (Object[] r : rows) out.add(new Empresa(((Number) r[0]).longValue(), (String) r[1]));
        return out;
    }

    /**
     * Genera y envia un OTP al socio identificado por (empresa, documento). Comportamiento uniforme:
     * exista o no una persona elegible, no se revela nada al llamador. Devuelve el canal usado (o null).
     */
    public String solicitarOtp(Long tenant, String documento, String proposito, String ip, String ua) {
        if (tenant == null || documento == null || documento.isBlank()) throw new NegocioException(GENERICO);
        fijarTenant(tenant);
        Persona p = buscarElegible(tenant, documento.trim());
        if (p == null) {
            auditar(tenant, null, "SOLICITUD_OTP", "doc:" + documento.trim(), ip, ua);
            return null;   // silencioso: no revela inexistencia
        }
        // obs 301: sin email ni telefono no hay canal de entrega; se audita y NO se genera un OTP
        // que el socio nunca podria recibir (ademas evita OTPs "usables" no entregados).
        String canal = (p.email != null && !p.email.isBlank()) ? "EMAIL"
                     : (p.telefono != null && !p.telefono.isBlank()) ? "SMS" : null;
        if (canal == null) {
            auditar(tenant, p.id, "SOLICITUD_OTP", "sin-canal", ip, ua);
            return null;   // comportamiento uniforme con "no encontrado" (no revela)
        }
        // Invalida OTP previos no usados (uno activo a la vez).
        em.createNativeQuery("UPDATE portal_otp SET usado = true WHERE tenant = :t AND persona = :p AND usado = false")
            .setParameter("t", tenant).setParameter("p", p.id).executeUpdate();

        int largo = Math.max(4, Math.min(8, paramInt(tenant, "PORTAL_OTP_LARGO", 6)));
        int expira = paramInt(tenant, "PORTAL_OTP_EXPIRA_MIN", 10);
        String codigo = generarCodigo(largo);

        em.createNativeQuery(
            "INSERT INTO portal_otp (tenant, persona, codigo_hash, proposito, canal, expira_en)"
          + " VALUES (:t, :p, :h, :prop, :c, now() + (:min || ' minutes')::interval)")
            .setParameter("t", tenant).setParameter("p", p.id)
            .setParameter("h", BCrypt.hashpw(codigo, BCrypt.gensalt(10)))
            .setParameter("prop", "RECUPERACION".equals(proposito) ? "RECUPERACION" : "LOGIN")
            .setParameter("c", canal).setParameter("min", String.valueOf(expira))
            .executeUpdate();

        enviarCodigo(p, codigo, expira, canal);
        auditar(tenant, p.id, "SOLICITUD_OTP", "canal:" + canal, ip, ua);
        return canal;
    }

    /**
     * Valida el OTP. Si es correcto devuelve la identidad (para crear la sesion de portal o para
     * habilitar la definicion de clave). Mensaje generico ante cualquier fallo.
     */
    public Identidad validarOtp(Long tenant, String documento, String codigo, String ip, String ua) {
        if (tenant == null || documento == null || codigo == null || codigo.isBlank())
            throw new NegocioException(GENERICO);
        fijarTenant(tenant);
        // obs 303: mensaje externo uniforme en todas las ramas (documento inexistente, sin OTP,
        // vencido, erroneo, max intentos); el motivo real solo va a auditoria.
        Persona p = buscarElegible(tenant, documento.trim());
        if (p == null) { auditar(tenant, null, "OTP_FALLIDO", "sin-persona", ip, ua); throw new NegocioException(OTP_GENERICO); }

        Object[] otp;
        try {
            otp = (Object[]) em.createNativeQuery(
                "SELECT portal_otp, codigo_hash, intentos FROM portal_otp"
              + " WHERE tenant = :t AND persona = :p AND usado = false AND expira_en > now()"
              + " ORDER BY creado_en DESC LIMIT 1")
                .setParameter("t", tenant).setParameter("p", p.id).getSingleResult();
        } catch (NoResultException e) {
            auditar(tenant, p.id, "OTP_FALLIDO", "sin-otp-vigente", ip, ua);
            throw new NegocioException(OTP_GENERICO);
        }
        long otpId = ((Number) otp[0]).longValue();
        String hash = (String) otp[1];
        int intentos = ((Number) otp[2]).intValue() + 1;
        int maxInt = paramInt(tenant, "PORTAL_OTP_MAX_INTENTOS", 5);

        if (intentos > maxInt) {
            em.createNativeQuery("UPDATE portal_otp SET usado = true, intentos = :i WHERE portal_otp = :id")
                .setParameter("i", intentos).setParameter("id", otpId).executeUpdate();
            auditar(tenant, p.id, "OTP_FALLIDO", "max-intentos", ip, ua);
            throw new NegocioException(OTP_GENERICO);
        }
        if (!BCrypt.checkpw(codigo.trim(), hash)) {
            em.createNativeQuery("UPDATE portal_otp SET intentos = :i WHERE portal_otp = :id")
                .setParameter("i", intentos).setParameter("id", otpId).executeUpdate();
            auditar(tenant, p.id, "OTP_FALLIDO", "codigo-incorrecto", ip, ua);
            throw new NegocioException(OTP_GENERICO);
        }
        em.createNativeQuery("UPDATE portal_otp SET usado = true, intentos = :i WHERE portal_otp = :id")
            .setParameter("i", intentos).setParameter("id", otpId).executeUpdate();
        auditar(tenant, p.id, "VALIDACION_OTP", null, ip, ua);
        p.tienePassword = tienePassword(tenant, p.id);
        return identidadDe(p, tenant);
    }

    /** Define o cambia la password de portal de la persona (bcrypt). Requiere identidad ya validada. */
    public void definirPassword(Long tenant, Long persona, String nueva, String ip, String ua) {
        if (tenant == null || persona == null) throw new NegocioException(GENERICO);
        if (nueva == null || nueva.trim().length() < 8)
            throw new NegocioException("La contrasena debe tener al menos 8 caracteres.");
        fijarTenant(tenant);
        String hash = BCrypt.hashpw(nueva.trim(), BCrypt.gensalt(10));
        int upd = em.createNativeQuery(
            "UPDATE persona_portal_credencial SET password_hash = :h, ultimo_cambio = now(),"
          + " estado = 'ACTIVO', intentos_fallidos = 0, bloqueado_hasta = NULL"
          + " WHERE tenant = :t AND persona = :p")
            .setParameter("h", hash).setParameter("t", tenant).setParameter("p", persona).executeUpdate();
        if (upd == 0) {
            em.createNativeQuery(
                "INSERT INTO persona_portal_credencial (tenant, persona, password_hash, ultimo_cambio, estado)"
              + " VALUES (:t, :p, :h, now(), 'ACTIVO')")
                .setParameter("t", tenant).setParameter("p", persona).setParameter("h", hash).executeUpdate();
        }
        auditar(tenant, persona, "CAMBIO_PASSWORD", null, ip, ua);
    }

    /** Login por (empresa, documento, password). Cuenta intentos y bloquea temporalmente. */
    public Identidad loginPassword(Long tenant, String documento, String password, String ip, String ua) {
        if (tenant == null || documento == null || password == null || password.isBlank())
            throw new NegocioException(GENERICO);
        fijarTenant(tenant);
        Persona p = buscarElegible(tenant, documento.trim());
        if (p == null) { auditar(tenant, null, "LOGIN_FALLIDO", "sin-persona", ip, ua); throw new NegocioException(GENERICO); }

        Object[] cred;
        try {
            cred = (Object[]) em.createNativeQuery(
                "SELECT password_hash, estado, intentos_fallidos, (bloqueado_hasta > now()) AS bloqueado"
              + " FROM persona_portal_credencial WHERE tenant = :t AND persona = :p")
                .setParameter("t", tenant).setParameter("p", p.id).getSingleResult();
        } catch (NoResultException e) {
            // obs 299: no revelar que el documento existe pero no tiene credencial; el detalle
            // queda solo en auditoria. El usuario tiene el enlace "Primer ingreso" siempre visible.
            auditar(tenant, p.id, "LOGIN_FALLIDO", "sin-credencial", ip, ua);
            throw new NegocioException(GENERICO);
        }
        String hash = (String) cred[0];
        boolean bloqueado = Boolean.TRUE.equals(cred[3]);
        if (bloqueado) {
            auditar(tenant, p.id, "LOGIN_FALLIDO", "bloqueado", ip, ua);
            throw new NegocioException("Acceso bloqueado temporalmente por intentos fallidos. Reintente mas tarde.");
        }
        if (hash == null || !BCrypt.checkpw(password.trim(), hash)) {
            registrarFallo(tenant, p.id);
            auditar(tenant, p.id, "LOGIN_FALLIDO", "password-incorrecta", ip, ua);
            throw new NegocioException(GENERICO);
        }
        em.createNativeQuery(
            "UPDATE persona_portal_credencial SET intentos_fallidos = 0, bloqueado_hasta = NULL"
          + " WHERE tenant = :t AND persona = :p")
            .setParameter("t", tenant).setParameter("p", p.id).executeUpdate();
        auditar(tenant, p.id, "LOGIN", null, ip, ua);
        return identidadDe(p, tenant);
    }

    /** Auditoria de cierre de sesion del portal. */
    public void auditarLogout(Long tenant, Long persona, String ip, String ua) {
        if (tenant == null || persona == null) return;
        fijarTenant(tenant);
        auditar(tenant, persona, "LOGOUT", null, ip, ua);
    }

    // ── internos ─────────────────────────────────────────────────────────────

    private void registrarFallo(Long tenant, Long persona) {
        int max = paramInt(tenant, "PORTAL_LOGIN_MAX_INTENTOS", 5);
        int bloqueoMin = paramInt(tenant, "PORTAL_BLOQUEO_MIN", 15);
        em.createNativeQuery(
            "UPDATE persona_portal_credencial SET intentos_fallidos = intentos_fallidos + 1,"
          + " bloqueado_hasta = CASE WHEN intentos_fallidos + 1 >= :max"
          + "   THEN now() + (:min || ' minutes')::interval ELSE bloqueado_hasta END"
          + " WHERE tenant = :t AND persona = :p")
            .setParameter("max", max).setParameter("min", String.valueOf(bloqueoMin))
            .setParameter("t", tenant).setParameter("p", persona).executeUpdate();
    }

    /** Persona ACTIVA del tenant con rol CLIENTE y/o PROPIETARIO ACTIVO; null si no elegible. */
    private Persona buscarElegible(Long tenant, String documento) {
        List<?> rows = em.createNativeQuery(
            "SELECT p.persona, p.nombre, pe.email, pe.telefono,"
          + "  EXISTS(SELECT 1 FROM persona_rol pr JOIN entidad e ON e.entidad = pr.rol"
          + "         WHERE pr.persona = p.persona AND pr.tenant = :t AND pr.estado = 'ACTIVO'"
          + "           AND e.lista = 'ROLES_PERSONA' AND e.codigo = 'CLIENTE' AND e.tenant IN (-1, :t)) AS es_cliente,"
          + "  EXISTS(SELECT 1 FROM persona_rol pr JOIN entidad e ON e.entidad = pr.rol"
          + "         WHERE pr.persona = p.persona AND pr.tenant = :t AND pr.estado = 'ACTIVO'"
          + "           AND e.lista = 'ROLES_PERSONA' AND e.codigo = 'PROPIETARIO' AND e.tenant IN (-1, :t)) AS es_prop"
          + " FROM persona p"
          + " LEFT JOIN persona_empresa pe ON pe.persona = p.persona AND pe.tenant = :t"
          + " WHERE p.numero_documento = :doc AND p.estado = 'ACTIVO' LIMIT 1")
            .setParameter("t", tenant).setParameter("doc", documento).getResultList();
        if (rows.isEmpty()) return null;
        Object[] r = (Object[]) rows.get(0);
        boolean cli = Boolean.TRUE.equals(r[4]);
        boolean prop = Boolean.TRUE.equals(r[5]);
        if (!cli && !prop) return null;   // pertenece pero sin rol habilitado para portal
        Persona p = new Persona();
        p.id = ((Number) r[0]).longValue();
        p.nombre = (String) r[1];
        p.email = (String) r[2];
        p.telefono = (String) r[3];
        p.documento = documento;
        p.esCliente = cli;
        p.esPropietario = prop;
        return p;
    }

    private boolean tienePassword(Long tenant, Long persona) {
        try {
            Object h = em.createNativeQuery(
                "SELECT password_hash FROM persona_portal_credencial WHERE tenant = :t AND persona = :p")
                .setParameter("t", tenant).setParameter("p", persona).getSingleResult();
            return h != null;
        } catch (NoResultException e) {
            return false;
        }
    }

    private Identidad identidadDe(Persona p, Long tenant) {
        Identidad id = new Identidad();
        id.tenant = tenant; id.persona = p.id; id.nombre = p.nombre;
        id.documento = p.documento; id.esCliente = p.esCliente; id.esPropietario = p.esPropietario;
        id.tienePassword = p.tienePassword;
        return id;
    }

    private void enviarCodigo(Persona p, String codigo, int expira, String canal) {
        if (!"EMAIL".equals(canal) || p.email == null) return;   // SMS: sin gateway, queda auditado
        String cuerpo = "Su codigo de acceso al portal es: " + codigo
              + "\n\nVence en " + expira + " minutos. Si no solicito este codigo, ignore este mensaje.";
        correo.enviarAsync(p.email, "Codigo de acceso al portal", cuerpo);
    }

    private static String generarCodigo(int largo) {
        StringBuilder sb = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) sb.append(RND.nextInt(10));
        return sb.toString();
    }

    private void auditar(Long tenant, Long persona, String accion, String recurso, String ip, String ua) {
        try {
            em.createNativeQuery(
                "INSERT INTO portal_acceso (tenant, usuario_codigo, persona, accion, recurso, ip, user_agent)"
              + " VALUES (:t, 'portal', :p, :a, :r, :ip, :ua)")
                .setParameter("t", tenant).setParameter("p", persona).setParameter("a", accion)
                .setParameter("r", recurso == null ? null : recurso.length() > 160 ? recurso.substring(0, 160) : recurso)
                .setParameter("ip", ip).setParameter("ua", ua == null ? null : ua.length() > 250 ? ua.substring(0, 250) : ua)
                .executeUpdate();
        } catch (RuntimeException ignore) {
            /* la auditoria nunca bloquea el flujo de acceso */
        }
    }

    private int paramInt(Long tenant, String clave, int def) {
        try {
            Object v = em.createNativeQuery(
                "SELECT valor FROM parametro_sistema WHERE clave = :c AND tenant IN (:t, -1) ORDER BY tenant DESC LIMIT 1")
                .setParameter("c", clave).setParameter("t", tenant).getSingleResult();
            return v == null ? def : Integer.parseInt(v.toString().trim());
        } catch (RuntimeException e) {
            return def;
        }
    }

    private void fijarTenant(Long t) {
        em.createNativeQuery("SELECT set_config('app.tenant', :t, true)")
            .setParameter("t", String.valueOf(t)).getSingleResult();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    private static class Persona {
        Long id; String nombre, email, telefono, documento; boolean esCliente, esPropietario, tienePassword;
    }
    public static class Empresa implements Serializable {
        public final Long id; public final String nombre;
        public Empresa(Long id, String nombre) { this.id = id; this.nombre = nombre; }
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
    }
    public static class Identidad implements Serializable {
        public Long tenant, persona; public String nombre, documento;
        public boolean esCliente, esPropietario, tienePassword;
        public Long getTenant() { return tenant; }
        public Long getPersona() { return persona; }
        public String getNombre() { return nombre; }
        public boolean isEsCliente() { return esCliente; }
        public boolean isEsPropietario() { return esPropietario; }
        public boolean isTienePassword() { return tienePassword; }
    }
}
