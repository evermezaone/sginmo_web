package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;

import java.util.List;

/**
 * ABM de las listas configurables (tabla generica `entidad`): agregar una opcion
 * a un combo del sistema es un alta aca, sin tocar codigo. PK compuesta (lista+codigo):
 * el codigo es INMUTABLE al editar (los datos historicos lo referencian).
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class ListaService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    /** Aislamiento por tenant (F4): opciones visibles = globales (-1) + del tenant; se edita lo propio. */
    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    /** Editable: el propio tenant, o -1 solo por SUPERADMIN. */
    private boolean editable(Long t) {
        return t != null && (t.equals(tenant.actual())
                || (py.com.pysistemas.sginmo.web.TenantContext.GLOBAL.equals(t) && tenant.esSuperadmin()));
    }

    /** Nombres de listas existentes visibles al tenant (para el combo selector). */
    public List<String> listas() {
        return em.createQuery(
                "SELECT DISTINCT e.lista FROM Entidad e WHERE e.tenant = -1 OR e.tenant = :t ORDER BY e.lista", String.class)
            .setParameter("t", tenant.actual()).getResultList();
    }

    public List<Entidad> opcionesDe(String lista, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        return em.createQuery(
                "SELECT e FROM Entidad e WHERE e.lista = :lista AND (e.tenant = -1 OR e.tenant = :t)"
                + " AND (:f = '' OR lower(e.codigo) LIKE :like OR lower(e.descripcion) LIKE :like) ORDER BY e.descripcion",
                Entidad.class)
            .setParameter("lista", lista).setParameter("t", tenant.actual())
            .setParameter("f", f).setParameter("like", "%" + f + "%")
            .getResultList();
    }

    @Transactional
    public Entidad guardar(Entidad opcion, boolean esNueva) {
        autorizacion.exigir("listas", esNueva ? "CREAR" : "EDITAR");
        if (opcion.getLista() == null || opcion.getLista().isBlank()
                || opcion.getCodigo() == null || opcion.getCodigo().isBlank()) {
            throw new NegocioException("Lista y código son obligatorios");
        }
        if (opcion.getDescripcion() == null || opcion.getDescripcion().isBlank()) {
            throw new NegocioException("La descripción es obligatoria");
        }
        try {
            if (esNueva) {
                opcion.setLista(opcion.getLista().trim().toUpperCase());
                opcion.setCodigo(opcion.getCodigo().trim());
                // El alta pertenece al tenant del usuario (SUPERADMIN crea -1 globales).
                opcion.setTenant(tenant.actual());
                Long dup = em.createQuery(
                        "SELECT COUNT(e) FROM Entidad e WHERE e.lista = :l AND e.codigo = :c AND e.tenant = :t", Long.class)
                    .setParameter("l", opcion.getLista()).setParameter("c", opcion.getCodigo())
                    .setParameter("t", opcion.getTenant()).getSingleResult();
                if (dup > 0) {
                    throw new NegocioException("Ya existe el código '" + opcion.getCodigo() + "' en la lista " + opcion.getLista());
                }
                em.persist(opcion);
            } else {
                Entidad enBd = em.find(Entidad.class, opcion.getId());
                if (enBd == null) throw new NegocioException("La opción no existe");
                if (!editable(enBd.getTenant())) throw new NegocioException("La opción pertenece a otra empresa");
                opcion.setTenant(enBd.getTenant());   // el tenant no se cambia por edicion
                opcion = em.merge(opcion);
            }
            em.flush();
            return opcion;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La opción fue modificada por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        autorizacion.exigir("listas", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        Entidad e = em.find(Entidad.class, id);
        if (e == null) throw new NegocioException("La opción no existe");
        if (!editable(e.getTenant())) throw new NegocioException("La opción pertenece a otra empresa");
        e.setEstado(estadoNuevo);
    }
}
