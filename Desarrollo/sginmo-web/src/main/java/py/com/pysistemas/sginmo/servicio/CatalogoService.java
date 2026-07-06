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

    /** Formas de pago ACTIVO y HABILITADO para nuevos cobros (caja). */
    public List<py.com.pysistemas.sginmo.dominio.catalogo.FormaPago> formasHabilitadas() {
        return em.createQuery(
                "SELECT fp FROM FormaPago fp WHERE fp.estado = 'ACTIVO' AND fp.habilitado = true ORDER BY fp.descripcion",
                py.com.pysistemas.sginmo.dominio.catalogo.FormaPago.class)
            .getResultList();
    }

    /** Articulos ACTIVOS (para combos de concepto en ingresos/egresos y liquidaciones). */
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Articulo> articulosActivos() {
        return em.createQuery(
                "SELECT a FROM Articulo a WHERE a.estado = 'ACTIVO' ORDER BY a.descripcion",
                py.com.pysistemas.sginmo.dominio.catalogo.Articulo.class)
            .getResultList();
    }

    /** Id de la moneda LOCAL (guarani) para cobros en moneda base; primera moneda si no hay LOCAL. */
    public Long monedaLocalId() {
        var locales = em.createQuery(
                "SELECT m.id FROM Moneda m WHERE m.tipoMoneda = 'LOCAL' AND m.estado = 'ACTIVO' ORDER BY m.id", Long.class)
            .setMaxResults(1).getResultList();
        if (!locales.isEmpty()) {
            return locales.get(0);
        }
        var cualquiera = em.createQuery("SELECT m.id FROM Moneda m ORDER BY m.id", Long.class)
            .setMaxResults(1).getResultList();
        return cualquiera.isEmpty() ? null : cualquiera.get(0);
    }
}
