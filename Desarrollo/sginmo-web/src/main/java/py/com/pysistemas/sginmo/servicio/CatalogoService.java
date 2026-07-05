package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.catalogo.Impuesto;

import java.util.List;

/** Lecturas de catalogos para combos: listas de `entidad` e impuestos activos. */
@ApplicationScoped
public class CatalogoService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Opciones ACTIVO de una lista configurable (el campo lista va fijo, patron del diseño). */
    public List<Entidad> opciones(String lista) {
        return em.createQuery(
                "SELECT e FROM Entidad e WHERE e.entidad = :lista AND e.estado = 'ACTIVO' ORDER BY e.descripcion",
                Entidad.class)
            .setParameter("lista", lista)
            .getResultList();
    }

    public Impuesto buscarImpuesto(Long id) {
        return em.find(Impuesto.class, id);
    }

    public List<Impuesto> impuestosActivos() {
        return em.createQuery(
                "SELECT i FROM Impuesto i WHERE i.estado = 'ACTIVO' ORDER BY i.descripcion", Impuesto.class)
            .getResultList();
    }
}
