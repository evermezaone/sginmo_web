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
                "SELECT e FROM Entidad e WHERE e.lista = :lista AND e.estado = 'ACTIVO' ORDER BY e.descripcion",
                Entidad.class)
            .setParameter("lista", lista)
            .getResultList();
    }

    // ── Resolver id <-> codigo de las opciones de catalogo (V26: las referencias
    //    guardan el id numerico de entidad). TODO(F4): filtrar por tenant IN(-1,:t);
    //    hoy todas las semillas viven en -1, asi que (lista,codigo) es univoco.

    /** Id de la opcion (lista, codigo), o null si el codigo es vacio o no existe. */
    public Long idOpcion(String lista, String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        var r = em.createQuery(
                "SELECT e.id FROM Entidad e WHERE e.lista = :l AND e.codigo = :c", Long.class)
            .setParameter("l", lista).setParameter("c", codigo).setMaxResults(1).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Codigo de una opcion por su id (para reglas de negocio basadas en el codigo). */
    public String codigoOpcion(Long id) {
        if (id == null) return null;
        var r = em.createQuery("SELECT e.codigo FROM Entidad e WHERE e.id = :id", String.class)
            .setParameter("id", id).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Descripcion de una opcion por su id (para mostrar en grillas/combos). */
    public String descripcionOpcion(Long id) {
        if (id == null) return null;
        var r = em.createQuery("SELECT e.descripcion FROM Entidad e WHERE e.id = :id", String.class)
            .setParameter("id", id).getResultList();
        return r.isEmpty() ? null : r.get(0);
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
