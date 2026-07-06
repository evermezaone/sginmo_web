package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.operacion.Liquidacion;
import py.com.pysistemas.sginmo.dominio.operacion.LiquidacionGasto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Liquidacion de operaciones (REQ-0025): al finalizar un alquiler se liquida la garantia
 * menos los gastos. saldo = total_garantia - total_gastos (positivo: a devolver al inquilino;
 * negativo: el inquilino debe). Una liquidacion por operacion (UNIQUE).
 */
@ApplicationScoped
public class LiquidacionService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(l) FROM Liquidacion l, Operacion o, Persona p"
            + " WHERE o.id = l.operacion AND p.id = o.cliente"
            + " AND (:f = '' OR lower(p.nombre) LIKE :like)", Long.class);
        aplicar(q, filtro);
        return q.getSingleResult();
    }

    /** Filas: [liquidacion, nombreCliente, operacionId]. */
    public List<Object[]> listar(int primero, int cantidad, String filtro) {
        var q = em.createQuery(
            "SELECT l, p.nombre, o.id FROM Liquidacion l, Operacion o, Persona p"
            + " WHERE o.id = l.operacion AND p.id = o.cliente"
            + " AND (:f = '' OR lower(p.nombre) LIKE :like) ORDER BY l.id DESC", Object[].class);
        aplicar(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void aplicar(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    /** Operaciones de alquiler que aun no fueron liquidadas (para el combo de alta). */
    public List<Object[]> operacionesLiquidables() {
        return em.createQuery(
            "SELECT o.id, p.nombre, o.garantia FROM Operacion o, Persona p"
            + " WHERE p.id = o.cliente AND o.tipoOperacion = 'ALQUILER'"
            + " AND NOT EXISTS (SELECT 1 FROM Liquidacion l WHERE l.operacion = o.id)"
            + " ORDER BY o.id DESC", Object[].class).getResultList();
    }

    public BigDecimal garantiaDe(Long operacionId) {
        var r = em.createQuery("SELECT o.garantia FROM Operacion o WHERE o.id = :o", BigDecimal.class)
            .setParameter("o", operacionId).getResultList();
        return r.isEmpty() ? BigDecimal.ZERO : r.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> gastosDe(Long liquidacionId) {
        return em.createNativeQuery(
            "SELECT ld.liquidacion_detalle, ld.articulo, a.descripcion, ld.monto"
            + " FROM liquidacion_detalle ld JOIN articulo a ON a.articulo = ld.articulo"
            + " WHERE ld.liquidacion = :l ORDER BY ld.numero_item")
            .setParameter("l", liquidacionId).getResultList();
    }

    @Transactional
    public Liquidacion guardar(Liquidacion liq, List<LiquidacionGasto> gastos) {
        boolean esNueva = liq.getId() == null;
        autorizacion.exigir("liquidaciones", esNueva ? "CREAR" : "EDITAR");
        if (liq.getOperacion() == null) throw new NegocioException("La operación es obligatoria");
        BigDecimal totalGastos = BigDecimal.ZERO;
        if (gastos != null) {
            for (var g : gastos) {
                if (g.getArticulo() == null) throw new NegocioException("Cada gasto necesita un concepto");
                if (g.getMonto() == null || g.getMonto().signum() < 0) {
                    throw new NegocioException("El monto de un gasto no puede ser negativo");
                }
                totalGastos = totalGastos.add(g.getMonto());
            }
        }
        liq.setTotalGastos(totalGastos);
        liq.setSaldo(liq.getTotalGarantia().subtract(totalGastos));
        try {
            Liquidacion r = esNueva ? persistir(liq) : em.merge(liq);
            em.flush();
            // reescribe los detalles (borra y reinserta: simple y correcto)
            em.createNativeQuery("DELETE FROM liquidacion_detalle WHERE liquidacion = :l")
                .setParameter("l", r.getId()).executeUpdate();
            int item = 1;
            if (gastos != null) {
                for (var g : gastos) {
                    em.createNativeQuery(
                        "INSERT INTO liquidacion_detalle (liquidacion, numero_item, articulo, monto, usuario_creacion, fecha_creacion)"
                        + " VALUES (:l, :n, :art, :monto, 'sistema', now())")
                        .setParameter("l", r.getId()).setParameter("n", item++)
                        .setParameter("art", g.getArticulo()).setParameter("monto", g.getMonto())
                        .executeUpdate();
                }
            }
            em.flush();
            return r;
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);   // UNIQUE(operacion) -> ya liquidada
        }
    }

    private Liquidacion persistir(Liquidacion l) { em.persist(l); return l; }
}
