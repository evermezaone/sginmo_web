package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import py.com.pysistemas.sginmo.dominio.catalogo.Entidad;
import py.com.pysistemas.sginmo.dominio.catalogo.Impuesto;

import java.util.List;

/** Lecturas de catalogos para combos: listas de `entidad` e impuestos activos. */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class CatalogoService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Aislamiento multiempresa (F4): catalogos visibles = globales (-1) + del tenant actual. */
    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    private Long t() { return tenant.actual(); }

    /** Opciones ACTIVO de una lista configurable, visibles al tenant (-1 global + propio). */
    public List<Entidad> opciones(String lista) {
        return em.createQuery(
                "SELECT e FROM Entidad e WHERE e.lista = :lista AND e.estado = 'ACTIVO'"
                + " AND (e.tenant = -1 OR e.tenant = :t) ORDER BY e.descripcion",
                Entidad.class)
            .setParameter("lista", lista).setParameter("t", t())
            .getResultList();
    }

    // ── Resolver id <-> codigo de las opciones de catalogo (V26: las referencias
    //    guardan el id numerico de entidad). TODO(F4): filtrar por tenant IN(-1,:t);
    //    hoy todas las semillas viven en -1, asi que (lista,codigo) es univoco.

    /** Id de la opcion (lista, codigo) visible al tenant (-1 global + propio; prefiere el propio). */
    public Long idOpcion(String lista, String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        var r = em.createQuery(
                "SELECT e.id FROM Entidad e WHERE e.lista = :l AND e.codigo = :c"
                + " AND (e.tenant = -1 OR e.tenant = :t) ORDER BY e.tenant DESC", Long.class)
            .setParameter("l", lista).setParameter("c", codigo).setParameter("t", t())
            .setMaxResults(1).getResultList();
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
                "SELECT i FROM Impuesto i WHERE i.estado = 'ACTIVO' AND (i.tenant = -1 OR i.tenant = :t)"
                + " ORDER BY i.descripcion", Impuesto.class)
            .setParameter("t", t()).getResultList();
    }

    /** Formas de pago ACTIVO y HABILITADO para nuevos cobros (caja). */
    public List<py.com.pysistemas.sginmo.dominio.catalogo.FormaPago> formasHabilitadas() {
        return em.createQuery(
                "SELECT fp FROM FormaPago fp WHERE fp.estado = 'ACTIVO' AND fp.habilitado = true"
                + " AND (fp.tenant = -1 OR fp.tenant = :t) ORDER BY fp.descripcion",
                py.com.pysistemas.sginmo.dominio.catalogo.FormaPago.class)
            .setParameter("t", t()).getResultList();
    }

    /** Articulos ACTIVOS (para combos de concepto en ingresos/egresos y liquidaciones). */
    public List<py.com.pysistemas.sginmo.dominio.catalogo.Articulo> articulosActivos() {
        return em.createQuery(
                "SELECT a FROM Articulo a WHERE a.estado = 'ACTIVO' AND (a.tenant = -1 OR a.tenant = :t)"
                + " ORDER BY a.descripcion",
                py.com.pysistemas.sginmo.dominio.catalogo.Articulo.class)
            .setParameter("t", t()).getResultList();
    }

    /** Id de la moneda LOCAL (guarani) para cobros en moneda base; primera moneda si no hay LOCAL. */
    public Long monedaLocalId() {
        var locales = em.createQuery(
                "SELECT m.id FROM Moneda m WHERE m.tipoMoneda = 'LOCAL' AND m.estado = 'ACTIVO'"
                + " AND (m.tenant = -1 OR m.tenant = :t) ORDER BY m.id", Long.class)
            .setParameter("t", t()).setMaxResults(1).getResultList();
        if (!locales.isEmpty()) {
            return locales.get(0);
        }
        var cualquiera = em.createQuery(
                "SELECT m.id FROM Moneda m WHERE (m.tenant = -1 OR m.tenant = :t) ORDER BY m.id", Long.class)
            .setParameter("t", t()).setMaxResults(1).getResultList();
        return cualquiera.isEmpty() ? null : cualquiera.get(0);
    }
}
