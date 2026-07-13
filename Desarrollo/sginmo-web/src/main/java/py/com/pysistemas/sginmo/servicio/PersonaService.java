package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.dominio.persona.PersonaEmpresa;
import py.com.pysistemas.sginmo.dominio.persona.PersonaFisica;
import py.com.pysistemas.sginmo.dominio.persona.PersonaJuridica;
import py.com.pysistemas.sginmo.dominio.persona.PersonaRol;

import java.util.List;
import java.util.Map;

/**
 * ABM de personas / socios de negocios (REQ-0012): una misma persona (fisica o juridica)
 * puede tener varios roles (CLIENTE, PROVEEDOR, PROPIETARIO, INQUILINO, ...). Documento
 * unico global. El detalle fisica/juridica se guarda en la subtabla correspondiente con
 * PK compartida (@MapsId).
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class PersonaService {

    private static final Map<String, String> ORDEN = Map.of(
        "nombre", "p.nombre", "numeroDocumento", "p.numeroDocumento",
        "tipoPersoneria", "p.tipoPersoneria", "estado", "p.estado");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private AuditoriaFuncionalService auditoria;   // obs 271: auditoria funcional visible

    /** Resuelve id de opciones de catalogo (V26: PersonaRol.rol es id de entidad). */
    @jakarta.inject.Inject
    private CatalogoService catalogoService;

    /** Aislamiento por tenant (F4): la cartera de personas es por tenant (persona_rol/persona_empresa). */
    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    // ── Consultas ──

    /** Cartera del tenant: la identidad es global, pero una persona "pertenece" a un tenant si
     *  tiene algun rol o datos comerciales (persona_empresa) en el; excluye el sentinel -1. */
    private static final String CARTERA =
        " p.id <> -1 AND (EXISTS (SELECT 1 FROM PersonaRol r WHERE r.persona = p.id AND r.tenant = :t)"
        + " OR EXISTS (SELECT 1 FROM PersonaEmpresa pe WHERE pe.persona = p.id AND pe.tenant = :t))";

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(p) FROM Persona p WHERE" + CARTERA
            + " AND (:f = '' OR lower(p.nombre) LIKE :like OR p.numeroDocumento LIKE :like)",
            Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<Persona> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery(
            "SELECT p FROM Persona p WHERE" + CARTERA
            + " AND (:f = '' OR lower(p.nombre) LIKE :like OR p.numeroDocumento LIKE :like) ORDER BY "
            + (ruta == null ? "p.nombre" : ruta) + (asc ? " ASC" : " DESC"), Persona.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%").setParameter("t", tenant.actual());
    }

    /** Personas activas con un rol dado (para combos de clientes, proveedores, propietarios...). */
    public List<Persona> porRol(String rolCodigo) {
        Long rolId = catalogoService.idOpcion("ROLES_PERSONA", rolCodigo);
        if (rolId == null) return java.util.List.of();
        // Aislamiento (obs 250): el combo solo expone personas con ese rol EN EL TENANT actual.
        return em.createQuery(
                "SELECT p FROM Persona p WHERE p.estado = 'ACTIVO' AND EXISTS "
                + "(SELECT 1 FROM PersonaRol r WHERE r.persona = p.id AND r.rol = :rol AND r.estado = 'ACTIVO' AND r.tenant = :t)"
                + " ORDER BY p.nombre", Persona.class)
            .setParameter("rol", rolId).setParameter("t", tenant.actual())
            .getResultList();
    }

    /** La persona pertenece a la cartera del tenant actual (rol o datos comerciales en el);
     *  el SUPERADMIN no tiene restriccion. Centraliza la guarda de las operaciones por id (obs 250). */
    private boolean perteneceAlTenant(Long personaId) {
        if (personaId == null) return false;
        if (tenant.esSuperadmin()) return true;
        Long n = em.createQuery("SELECT COUNT(p) FROM Persona p WHERE p.id = :id AND" + CARTERA, Long.class)
            .setParameter("id", personaId).setParameter("t", tenant.actual()).getSingleResult();
        return n > 0;
    }

    // Detalle por id SOLO si la persona esta en la cartera del tenant (obs 254); si no, null.
    public Persona buscar(Long id) {
        return (id == null || !perteneceAlTenant(id)) ? null : em.find(Persona.class, id);
    }

    public PersonaFisica fisicaDe(Long id) {
        return (id == null || !perteneceAlTenant(id)) ? null : em.find(PersonaFisica.class, id);
    }
    public PersonaJuridica juridicaDe(Long id) {
        return (id == null || !perteneceAlTenant(id)) ? null : em.find(PersonaJuridica.class, id);
    }

    /** Roles ACTIVOS de la persona EN EL TENANT actual (obs 250); los INACTIVOS son historial. */
    public List<PersonaRol> rolesDe(Long personaId) {
        return em.createQuery("SELECT r FROM PersonaRol r WHERE r.persona = :p AND r.estado = 'ACTIVO'"
                + " AND (:sa = TRUE OR r.tenant = :t) ORDER BY r.rol", PersonaRol.class)
            .setParameter("p", personaId)
            .setParameter("sa", tenant.esSuperadmin()).setParameter("t", tenant.actual())
            .getResultList();
    }

    public boolean existeDocumento(String doc, Long exceptoId) {
        if (doc == null || doc.isBlank()) return false;
        return em.createQuery("SELECT COUNT(p) FROM Persona p WHERE p.numeroDocumento = :d AND (:id IS NULL OR p.id <> :id)", Long.class)
            .setParameter("d", doc.trim()).setParameter("id", exceptoId).getSingleResult() > 0;
    }

    // ── Escrituras ──

    /** Datos comerciales de la persona en un tenant (persona_empresa), o null si no hay. */
    public PersonaEmpresa datosEmpresaDe(Long personaId, Long tenant) {
        if (personaId == null || tenant == null) return null;
        var r = em.createQuery(
                "SELECT pe FROM PersonaEmpresa pe WHERE pe.persona = :p AND pe.tenant = :t", PersonaEmpresa.class)
            .setParameter("p", personaId).setParameter("t", tenant).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Alta/edicion de persona fisica (persona + persona_fisica + persona_empresa del tenant). */
    @Transactional
    public Persona guardarFisica(Persona persona, PersonaFisica fisica, PersonaEmpresa datos, Long tenant) {
        autorizacion.exigir("personas", persona.getId() == null ? "CREAR" : "EDITAR");
        validarComun(persona);
        if (fisica.getNombres() == null || fisica.getNombres().isBlank()
                || fisica.getApellidos() == null || fisica.getApellidos().isBlank()) {
            throw new NegocioException("Nombres y apellidos son obligatorios");
        }
        persona.setTipoPersoneria("FISICA");
        persona.setNombre((fisica.getNombres() + " " + fisica.getApellidos()).trim());
        return persistirSubtipo(persona, fisica, null, datos, tenant);
    }

    /** Alta/edicion de persona juridica (persona + persona_juridica + persona_empresa del tenant). */
    @Transactional
    public Persona guardarJuridica(Persona persona, PersonaJuridica juridica, PersonaEmpresa datos, Long tenant) {
        autorizacion.exigir("personas", persona.getId() == null ? "CREAR" : "EDITAR");
        validarComun(persona);
        if (juridica.getRazonSocial() == null || juridica.getRazonSocial().isBlank()) {
            throw new NegocioException("La razón social es obligatoria");
        }
        persona.setTipoPersoneria("JURIDICA");
        persona.setNombre(juridica.getRazonSocial());
        return persistirSubtipo(persona, null, juridica, datos, tenant);
    }

    /** Upsert de los datos comerciales de la persona en el tenant del contexto (V26). */
    private void guardarDatosEmpresa(Persona persona, PersonaEmpresa datos, Long tenant) {
        if (datos == null || tenant == null) return;
        datos.setPersona(persona.getId());
        datos.setTenant(tenant);
        if (datos.getEstado() == null) datos.setEstado("ACTIVO");
        if (datos.getId() == null) em.persist(datos); else em.merge(datos);
    }

    private Persona persistirSubtipo(Persona persona, PersonaFisica fisica, PersonaJuridica juridica,
                                     PersonaEmpresa datos, Long tenant) {
        try {
            // El subtipo debe apuntar a la MISMA instancia de persona que trae los datos
            // comunes editados. Si no, el cascade MERGE del subtipo re-mergea una persona
            // distinta (cargada aparte) y pisa los campos comunes recien editados.
            if (fisica != null) fisica.setPersona(persona);
            if (juridica != null) juridica.setPersona(persona);
            boolean esNueva = persona.getId() == null;
            Map<String, Object> antes = null;   // obs 271: snapshot para el diff de auditoria
            if (!esNueva) {
                Persona o = em.find(Persona.class, persona.getId());
                if (o != null) antes = snapP(o);
            }
            if (esNueva) {
                em.persist(persona);
                em.flush();
                if (fisica != null) em.persist(fisica);
                if (juridica != null) em.persist(juridica);
            } else {
                em.merge(persona);
                if (fisica != null) em.merge(fisica);
                if (juridica != null) em.merge(juridica);
            }
            em.flush();
            guardarDatosEmpresa(persona, datos, tenant);
            em.flush();
            if (esNueva) auditoria.registrarAlta("persona", persona.getNumeroDocumento(), "personas");
            else if (antes != null) auditoria.registrarCambios("persona", persona.getNumeroDocumento(), "personas", null, antes, snapP(persona));
            return persona;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La persona fue modificada por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private void validarComun(Persona p) {
        if (p.getNumeroDocumento() == null || p.getNumeroDocumento().isBlank()) {
            throw new NegocioException("El documento / RUC es obligatorio");
        }
        if (existeDocumento(p.getNumeroDocumento(), p.getId())) {
            throw new NegocioException("Ya existe una persona con el documento '" + p.getNumeroDocumento() + "'");
        }
    }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        boolean reactivar = "ACTIVO".equals(estadoNuevo);
        autorizacion.exigir("personas", reactivar ? "REACTIVAR" : "INACTIVAR");
        Persona p = em.find(Persona.class, id);
        if (p == null) throw new NegocioException("La persona no existe");
        // Pertenencia (obs 250): solo se opera sobre personas de la cartera del tenant.
        if (!perteneceAlTenant(id)) throw new NegocioException("La persona pertenece a otra empresa");
        String estadoAnterior = p.getEstado();
        p.setEstado(estadoNuevo);
        auditoria.registrar("persona", p.getNumeroDocumento(),
                reactivar ? AuditoriaFuncionalService.REACTIVAR : AuditoriaFuncionalService.INACTIVAR,
                "personas", "estado " + estadoAnterior + " -> " + estadoNuevo);
    }

    /** Snapshot de campos auditables de la persona (obs 271). */
    private static Map<String, Object> snapP(Persona p) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("nombre", p.getNombre());
        m.put("numeroDocumento", p.getNumeroDocumento());
        m.put("tipoPersoneria", p.getTipoPersoneria());
        m.put("estado", p.getEstado());
        return m;
    }

    // ── Roles ──

    @Transactional
    public void agregarRol(Long personaId, String rolCodigo) {
        autorizacion.exigir("personas", "EDITAR");
        if (rolCodigo == null || rolCodigo.isBlank()) throw new NegocioException("Elija el rol");
        // Pertenencia (obs 252): no se vincula por id una identidad ajena al tenant; la persona
        // ya debe estar en la cartera (el alta crea persona_empresa). Vincular una identidad
        // global no asociada seria un flujo explicito aparte, no un agregar-rol por id.
        if (!perteneceAlTenant(personaId)) {
            throw new NegocioException("La persona no pertenece a la cartera de la empresa");
        }
        Long rolId = catalogoService.idOpcion("ROLES_PERSONA", rolCodigo);
        if (rolId == null) throw new NegocioException("El rol '" + rolCodigo + "' no existe en el catalogo");
        // Si ya existe (activo o inactivo) EN ESTE TENANT no se duplica (obs 250: el rol es por
        // tenant; una misma persona puede tener el rol en otra empresa): activo -> error; inactivo -> reactiva.
        var existentes = em.createQuery(
                "SELECT r FROM PersonaRol r WHERE r.persona = :p AND r.rol = :r AND r.tenant = :t", PersonaRol.class)
            .setParameter("p", personaId).setParameter("r", rolId).setParameter("t", tenant.actual()).getResultList();
        for (var r : existentes) {
            if ("ACTIVO".equals(r.getEstado())) throw new NegocioException("La persona ya tiene ese rol");
        }
        if (!existentes.isEmpty()) {
            existentes.get(0).setEstado("ACTIVO");   // reactiva preservando el historial
            return;
        }
        var rol = new PersonaRol();
        rol.setPersona(personaId);
        rol.setRol(rolId);
        rol.setTenant(tenant.actual());   // V26: el rol pertenece al tenant del contexto (NOT NULL)
        em.persist(rol);
    }

    @Transactional
    public void quitarRol(Long personaRolId) {
        autorizacion.exigir("personas", "EDITAR");
        var r = em.find(PersonaRol.class, personaRolId);
        if (r == null) throw new NegocioException("El rol no existe");
        // Pertenencia (obs 250): no se baja por id un rol de otra empresa.
        if (!tenant.esSuperadmin() && !tenant.actual().equals(r.getTenant())) {
            throw new NegocioException("El rol pertenece a otra empresa");
        }
        // Baja LOGICA: preserva la trazabilidad historica del rol (operaciones, cobros,
        // activos y reportes que ya lo referencian). Se puede reactivar con agregarRol.
        r.setEstado("INACTIVO");
    }

    /**
     * REQ-0089: reconcilia los roles ACTIVOS de la persona EN EL TENANT actual contra la lista deseada
     * (los ids de rol que quedaron en el ABM al guardar). Inserta/reactiva los que faltan y da de baja
     * logica (INACTIVO) los activos que ya no estan. Preserva el historial y respeta pertenencia/tenant.
     */
    @Transactional
    public void reconciliarRoles(Long personaId, java.util.List<Long> rolesDeseados) {
        autorizacion.exigir("personas", "EDITAR");
        if (personaId == null) return;
        if (!perteneceAlTenant(personaId)) {
            throw new NegocioException("La persona no pertenece a la cartera de la empresa");
        }
        Long t = tenant.actual();
        java.util.Set<Long> deseados = rolesDeseados == null ? java.util.Set.of()
                : new java.util.HashSet<>(rolesDeseados);
        // Roles existentes (activos e inactivos) de la persona en este tenant.
        List<PersonaRol> actuales = em.createQuery(
                "SELECT r FROM PersonaRol r WHERE r.persona = :p AND r.tenant = :t", PersonaRol.class)
            .setParameter("p", personaId).setParameter("t", t).getResultList();
        java.util.Set<Long> yaExisten = new java.util.HashSet<>();
        for (PersonaRol r : actuales) {
            yaExisten.add(r.getRol());
            boolean debeEstar = deseados.contains(r.getRol());
            if (debeEstar && !"ACTIVO".equals(r.getEstado())) {
                r.setEstado("ACTIVO");                 // reactiva preservando historial
            } else if (!debeEstar && "ACTIVO".equals(r.getEstado())) {
                r.setEstado("INACTIVO");               // baja logica de los desmarcados
            }
        }
        // Inserta los roles nuevos que no tenia ningun registro previo en este tenant.
        for (Long rolId : deseados) {
            if (rolId == null || yaExisten.contains(rolId)) continue;
            var nr = new PersonaRol();
            nr.setPersona(personaId);
            nr.setRol(rolId);
            nr.setTenant(t);
            nr.setEstado("ACTIVO");
            em.persist(nr);
        }
    }
}
