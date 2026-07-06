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

    /** Nombres de listas existentes (para el combo selector de la pantalla). */
    public List<String> listas() {
        return em.createQuery("SELECT DISTINCT e.entidad FROM Entidad e ORDER BY e.entidad", String.class)
            .getResultList();
    }

    public List<Entidad> opcionesDe(String lista, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        return em.createQuery(
                "SELECT e FROM Entidad e WHERE e.entidad = :lista AND (:f = '' OR lower(e.codigo) LIKE :like OR lower(e.descripcion) LIKE :like) ORDER BY e.descripcion",
                Entidad.class)
            .setParameter("lista", lista).setParameter("f", f).setParameter("like", "%" + f + "%")
            .getResultList();
    }

    @Transactional
    public Entidad guardar(Entidad opcion, boolean esNueva) {
        if (opcion.getEntidad() == null || opcion.getEntidad().isBlank()
                || opcion.getCodigo() == null || opcion.getCodigo().isBlank()) {
            throw new NegocioException("Lista y código son obligatorios");
        }
        if (opcion.getDescripcion() == null || opcion.getDescripcion().isBlank()) {
            throw new NegocioException("La descripción es obligatoria");
        }
        try {
            if (esNueva) {
                opcion.setEntidad(opcion.getEntidad().trim().toUpperCase());
                opcion.setCodigo(opcion.getCodigo().trim());
                var pk = new py.com.pysistemas.sginmo.dominio.catalogo.EntidadId(opcion.getEntidad(), opcion.getCodigo());
                if (em.find(Entidad.class, pk) != null) {
                    throw new NegocioException("Ya existe el código '" + opcion.getCodigo() + "' en la lista " + opcion.getEntidad());
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
    public void cambiarEstado(String lista, String codigo, String estadoNuevo) {
        var pk = new py.com.pysistemas.sginmo.dominio.catalogo.EntidadId(lista, codigo);
        Entidad e = em.find(Entidad.class, pk);
        if (e == null) throw new NegocioException("La opción no existe");
        e.setEstado(estadoNuevo);
    }
}
