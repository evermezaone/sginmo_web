package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.dominio.persona.PersonaEmpresa;
import py.com.pysistemas.sginmo.dominio.persona.PersonaJuridica;
import py.com.pysistemas.sginmo.dominio.persona.PersonaRol;
import py.com.pysistemas.sginmo.dominio.persona.Sucursal;

import java.util.List;

/**
 * ABM de empresas (REQ-0009): empresa = persona juridica con rol EMPRESA activo.
 * Administra en una sola transaccion persona + persona_juridica + rol, y las
 * sucursales de cada empresa ("por defecto" unica por empresa).
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class EmpresaService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    /** Resuelve id de opciones de catalogo (V26: PersonaRol.rol es id de entidad). */
    @jakarta.inject.Inject
    private CatalogoService catalogoService;

    /** Id de la opcion EMPRESA en la lista ROLES_PERSONA (identitaria, vive en -1). */
    private Long rolEmpresaId() { return catalogoService.idOpcion("ROLES_PERSONA", "EMPRESA"); }

    // ── Consultas ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(pj) FROM PersonaJuridica pj WHERE EXISTS (" + subRol() + ")"
            + " AND (:f = '' OR lower(pj.razonSocial) LIKE :like OR pj.persona.numeroDocumento LIKE :like)",
            Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<PersonaJuridica> listar(int primero, int cantidad, String filtro) {
        var q = em.createQuery(
            "SELECT pj FROM PersonaJuridica pj WHERE EXISTS (" + subRol() + ")"
            + " AND (:f = '' OR lower(pj.razonSocial) LIKE :like OR pj.persona.numeroDocumento LIKE :like)"
            + " ORDER BY pj.razonSocial",
            PersonaJuridica.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private String subRol() {
        return "SELECT 1 FROM PersonaRol r WHERE r.persona = pj.id AND r.rol = " + rolEmpresaId();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    public PersonaJuridica buscar(Long personaId) {
        return personaId == null ? null : em.find(PersonaJuridica.class, personaId);
    }

    // ── Escrituras ──

    @Transactional
    /** Datos comerciales propios de la empresa (persona_empresa con tenant = ella misma). */
    public PersonaEmpresa datosDe(Long empresaId) {
        if (empresaId == null) return null;
        var r = em.createQuery(
                "SELECT pe FROM PersonaEmpresa pe WHERE pe.persona = :p AND pe.tenant = :p", PersonaEmpresa.class)
            .setParameter("p", empresaId).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    private void guardarDatosEmpresa(Long empresaId, PersonaEmpresa datos) {
        if (datos == null || empresaId == null) return;
        datos.setPersona(empresaId);
        datos.setTenant(empresaId);          // la empresa es su propio tenant
        if (datos.getEstado() == null) datos.setEstado("ACTIVO");
        if (datos.getId() == null) em.persist(datos); else em.merge(datos);
    }

    public PersonaJuridica guardar(PersonaJuridica empresa, PersonaEmpresa datos) {
        boolean esNueva = empresa.getId() == null;
        autorizacion.exigir("empresas", esNueva ? "CREAR" : "EDITAR");
        Persona p = empresa.getPersona();
        if (empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank()) {
            throw new NegocioException("La razón social es obligatoria");
        }
        if (p.getNumeroDocumento() == null || p.getNumeroDocumento().isBlank()) {
            throw new NegocioException("El RUC / documento es obligatorio");
        }
        Long repetidos = em.createQuery(
                "SELECT COUNT(x) FROM Persona x WHERE x.numeroDocumento = :doc AND (:id IS NULL OR x.id <> :id)",
                Long.class)
            .setParameter("doc", p.getNumeroDocumento().trim())
            .setParameter("id", empresa.getId())
            .getSingleResult();
        if (repetidos > 0) {
            throw new NegocioException("Ya existe una persona con el documento '" + p.getNumeroDocumento() + "'");
        }
        p.setTipoPersoneria("JURIDICA");
        p.setNombre(empresa.getRazonSocial());   // nombre visible = razon social
        try {
            PersonaJuridica resultado;
            if (esNueva) {
                em.persist(empresa);             // cascade PERSIST crea persona (PK compartida)
                em.flush();
                var rol = new PersonaRol();
                rol.setPersona(empresa.getId());
                rol.setRol(rolEmpresaId());
                em.persist(rol);
                resultado = empresa;
            } else {
                resultado = em.merge(empresa);
            }
            em.flush();
            guardarDatosEmpresa(resultado.getId(), datos);
            em.flush();
            return resultado;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La empresa fue modificada por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    /** Baja/alta logica sobre persona.estado (la empresa desaparece de selecciones nuevas). */
    @Transactional
    public void cambiarEstado(Long personaId, String estadoNuevo) {
        autorizacion.exigir("empresas", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        PersonaJuridica pj = em.find(PersonaJuridica.class, personaId);
        if (pj == null) {
            throw new NegocioException("La empresa no existe");
        }
        pj.getPersona().setEstado(estadoNuevo);
    }

    // ── Sucursales ──

    public List<Sucursal> sucursalesDe(Long personaJuridica) {
        return em.createQuery(
                "SELECT s FROM Sucursal s WHERE s.personaJuridica = :pj ORDER BY s.porDefecto DESC, s.descripcion",
                Sucursal.class)
            .setParameter("pj", personaJuridica)
            .getResultList();
    }

    public List<Sucursal> sucursalesActivasDe(Long personaJuridica) {
        return em.createQuery(
                "SELECT s FROM Sucursal s WHERE s.personaJuridica = :pj AND s.estado = 'ACTIVO'"
                + " ORDER BY s.porDefecto DESC, s.descripcion",
                Sucursal.class)
            .setParameter("pj", personaJuridica)
            .getResultList();
    }

    @Transactional
    public Sucursal guardarSucursal(Sucursal sucursal) {
        autorizacion.exigir("empresas", sucursal.getId() == null ? "CREAR" : "EDITAR");
        if (sucursal.getPersonaJuridica() == null) {
            throw new NegocioException("Guarde la empresa antes de cargar sucursales");
        }
        if (sucursal.getDescripcion() == null || sucursal.getDescripcion().isBlank()) {
            throw new NegocioException("La descripción de la sucursal es obligatoria");
        }
        if (sucursal.getDireccion() == null || sucursal.getDireccion().isBlank()) {
            throw new NegocioException("La dirección es obligatoria");
        }
        if (sucursal.getTelefono() == null || sucursal.getTelefono().isBlank()) {
            sucursal.setTelefono("-");
        }
        // V26: la sucursal pertenece al tenant de su empresa (persona_juridica).
        sucursal.setTenant(sucursal.getPersonaJuridica());
        try {
            if (sucursal.isPorDefecto()) {
                em.createQuery("UPDATE Sucursal x SET x.porDefecto = false WHERE x.personaJuridica = :pj"
                        + " AND x.porDefecto = true AND (:id IS NULL OR x.id <> :id)")
                    .setParameter("pj", sucursal.getPersonaJuridica())
                    .setParameter("id", sucursal.getId())
                    .executeUpdate();
            }
            Sucursal r = sucursal.getId() == null ? persistir(sucursal) : em.merge(sucursal);
            em.flush();
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La sucursal fue modificada por otro usuario. Reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private Sucursal persistir(Sucursal s) { em.persist(s); return s; }

    @Transactional
    public void cambiarEstadoSucursal(Long id, String estadoNuevo) {
        autorizacion.exigir("empresas", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        Sucursal s = em.find(Sucursal.class, id);
        if (s == null) {
            throw new NegocioException("La sucursal no existe");
        }
        s.setEstado(estadoNuevo);
    }
}
