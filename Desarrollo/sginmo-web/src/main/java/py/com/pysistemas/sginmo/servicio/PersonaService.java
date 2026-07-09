package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
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
public class PersonaService {

    private static final Map<String, String> ORDEN = Map.of(
        "nombre", "p.nombre", "numeroDocumento", "p.numeroDocumento",
        "tipoPersoneria", "p.tipoPersoneria", "estado", "p.estado");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    /** Resuelve id de opciones de catalogo (V26: PersonaRol.rol es id de entidad). */
    @jakarta.inject.Inject
    private CatalogoService catalogoService;

    // ── Consultas ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(p) FROM Persona p WHERE (:f = '' OR lower(p.nombre) LIKE :like OR p.numeroDocumento LIKE :like)",
            Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<Persona> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery(
            "SELECT p FROM Persona p WHERE (:f = '' OR lower(p.nombre) LIKE :like OR p.numeroDocumento LIKE :like) ORDER BY "
            + (ruta == null ? "p.nombre" : ruta) + (asc ? " ASC" : " DESC"), Persona.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    /** Personas activas con un rol dado (para combos de clientes, proveedores, propietarios...). */
    public List<Persona> porRol(String rolCodigo) {
        Long rolId = catalogoService.idOpcion("ROLES_PERSONA", rolCodigo);
        if (rolId == null) return java.util.List.of();
        return em.createQuery(
                "SELECT p FROM Persona p WHERE p.estado = 'ACTIVO' AND EXISTS "
                + "(SELECT 1 FROM PersonaRol r WHERE r.persona = p.id AND r.rol = :rol AND r.estado = 'ACTIVO')"
                + " ORDER BY p.nombre", Persona.class)
            .setParameter("rol", rolId)
            .getResultList();
    }

    public Persona buscar(Long id) { return id == null ? null : em.find(Persona.class, id); }

    public PersonaFisica fisicaDe(Long id) { return em.find(PersonaFisica.class, id); }
    public PersonaJuridica juridicaDe(Long id) { return em.find(PersonaJuridica.class, id); }

    /** Roles ACTIVOS de la persona (los INACTIVOS quedan como historial, no se listan). */
    public List<PersonaRol> rolesDe(Long personaId) {
        return em.createQuery("SELECT r FROM PersonaRol r WHERE r.persona = :p AND r.estado = 'ACTIVO' ORDER BY r.rol", PersonaRol.class)
            .setParameter("p", personaId).getResultList();
    }

    public boolean existeDocumento(String doc, Long exceptoId) {
        if (doc == null || doc.isBlank()) return false;
        return em.createQuery("SELECT COUNT(p) FROM Persona p WHERE p.numeroDocumento = :d AND (:id IS NULL OR p.id <> :id)", Long.class)
            .setParameter("d", doc.trim()).setParameter("id", exceptoId).getSingleResult() > 0;
    }

    // ── Escrituras ──

    /** Alta/edicion de persona fisica (persona + persona_fisica en una transaccion). */
    @Transactional
    public Persona guardarFisica(Persona persona, PersonaFisica fisica) {
        autorizacion.exigir("personas", persona.getId() == null ? "CREAR" : "EDITAR");
        validarComun(persona);
        if (fisica.getNombres() == null || fisica.getNombres().isBlank()
                || fisica.getApellidos() == null || fisica.getApellidos().isBlank()) {
            throw new NegocioException("Nombres y apellidos son obligatorios");
        }
        persona.setTipoPersoneria("FISICA");
        persona.setNombre((fisica.getNombres() + " " + fisica.getApellidos()).trim());
        return persistirSubtipo(persona, fisica, null);
    }

    /** Alta/edicion de persona juridica (persona + persona_juridica en una transaccion). */
    @Transactional
    public Persona guardarJuridica(Persona persona, PersonaJuridica juridica) {
        autorizacion.exigir("personas", persona.getId() == null ? "CREAR" : "EDITAR");
        validarComun(persona);
        if (juridica.getRazonSocial() == null || juridica.getRazonSocial().isBlank()) {
            throw new NegocioException("La razón social es obligatoria");
        }
        persona.setTipoPersoneria("JURIDICA");
        persona.setNombre(juridica.getRazonSocial());
        return persistirSubtipo(persona, null, juridica);
    }

    private Persona persistirSubtipo(Persona persona, PersonaFisica fisica, PersonaJuridica juridica) {
        try {
            // El subtipo debe apuntar a la MISMA instancia de persona que trae los datos
            // comunes editados. Si no, el cascade MERGE del subtipo re-mergea una persona
            // distinta (cargada aparte) y pisa los campos comunes recien editados.
            if (fisica != null) fisica.setPersona(persona);
            if (juridica != null) juridica.setPersona(persona);
            boolean esNueva = persona.getId() == null;
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
        autorizacion.exigir("personas", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        Persona p = em.find(Persona.class, id);
        if (p == null) throw new NegocioException("La persona no existe");
        p.setEstado(estadoNuevo);
    }

    // ── Roles ──

    @Transactional
    public void agregarRol(Long personaId, String rolCodigo) {
        autorizacion.exigir("personas", "EDITAR");
        if (rolCodigo == null || rolCodigo.isBlank()) throw new NegocioException("Elija el rol");
        Long rolId = catalogoService.idOpcion("ROLES_PERSONA", rolCodigo);
        if (rolId == null) throw new NegocioException("El rol '" + rolCodigo + "' no existe en el catalogo");
        // Si ya existe (activo o inactivo) NO se duplica: activo -> error; inactivo -> se reactiva.
        var existentes = em.createQuery(
                "SELECT r FROM PersonaRol r WHERE r.persona = :p AND r.rol = :r", PersonaRol.class)
            .setParameter("p", personaId).setParameter("r", rolId).getResultList();
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
        em.persist(rol);
    }

    @Transactional
    public void quitarRol(Long personaRolId) {
        autorizacion.exigir("personas", "EDITAR");
        var r = em.find(PersonaRol.class, personaRolId);
        if (r == null) throw new NegocioException("El rol no existe");
        // Baja LOGICA: preserva la trazabilidad historica del rol (operaciones, cobros,
        // activos y reportes que ya lo referencian). Se puede reactivar con agregarRol.
        r.setEstado("INACTIVO");
    }
}
