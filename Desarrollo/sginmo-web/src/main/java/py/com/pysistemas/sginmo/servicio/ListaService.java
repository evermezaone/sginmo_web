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
public class ListaService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    /** Nombres de listas existentes (para el combo selector de la pantalla). */
    public List<String> listas() {
        return em.createQuery("SELECT DISTINCT e.lista FROM Entidad e ORDER BY e.lista", String.class)
            .getResultList();
    }

    public List<Entidad> opcionesDe(String lista, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        return em.createQuery(
                "SELECT e FROM Entidad e WHERE e.lista = :lista AND (:f = '' OR lower(e.codigo) LIKE :like OR lower(e.descripcion) LIKE :like) ORDER BY e.descripcion",
                Entidad.class)
            .setParameter("lista", lista).setParameter("f", f).setParameter("like", "%" + f + "%")
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
                // TODO(F6): el tenant sale del contexto (SUPERADMIN edita -1; ADMIN su tenant).
                if (opcion.getTenant() == null) opcion.setTenant(-1L);
                Long dup = em.createQuery(
                        "SELECT COUNT(e) FROM Entidad e WHERE e.lista = :l AND e.codigo = :c AND e.tenant = :t", Long.class)
                    .setParameter("l", opcion.getLista()).setParameter("c", opcion.getCodigo())
                    .setParameter("t", opcion.getTenant()).getSingleResult();
                if (dup > 0) {
                    throw new NegocioException("Ya existe el código '" + opcion.getCodigo() + "' en la lista " + opcion.getLista());
                }
                em.persist(opcion);
            } else {
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
        e.setEstado(estadoNuevo);
    }
}
