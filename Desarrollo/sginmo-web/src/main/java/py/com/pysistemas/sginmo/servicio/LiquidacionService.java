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

    @jakarta.inject.Inject
    private py.com.one.core.UsuarioActual usuarioActual;

    /** Usuario autenticado para las escrituras nativas (obs 232); fallback 'sistema' solo sin sesion. */
    private String usuarioAuditoria() {
        try {
            String u = usuarioActual.codigoUsuario();
            return (u == null || u.isBlank()) ? py.com.one.core.UsuarioActual.SISTEMA : u;
        } catch (RuntimeException sinContexto) {
            return py.com.one.core.UsuarioActual.SISTEMA;
        }
    }

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

    /**
     * Plantilla de gastos (obs 231, RN-PLANT-001/002): categorias precargadas desde la
     * operacion, con calculo automatico de alquileres pendientes (saldo de cuotas
     * PENDIENTE) y mora acumulada (f_mora_cuota a hoy). Solo filas con monto > 0.
     */
    public List<LiquidacionGasto> plantillaDe(Long operacionId) {
        List<LiquidacionGasto> plantilla = new java.util.ArrayList<>();
        if (operacionId == null) return plantilla;
        Object pend = em.createNativeQuery(
                "SELECT COALESCE(SUM(saldo),0) FROM cronograma_cuota WHERE operacion = :op AND estado = 'PENDIENTE'")
            .setParameter("op", operacionId).getSingleResult();
        Object mora = em.createNativeQuery(
                "SELECT COALESCE(SUM(f_mora_cuota(cronograma_cuota, current_date)),0)"
                + " FROM cronograma_cuota WHERE operacion = :op AND estado = 'PENDIENTE'")
            .setParameter("op", operacionId).getSingleResult();
        agregarItemPlantilla(plantilla, "ALQUILERES_PENDIENTES", new BigDecimal(pend.toString()));
        agregarItemPlantilla(plantilla, "MORA", new BigDecimal(mora.toString()));
        return plantilla;
    }

    private void agregarItemPlantilla(List<LiquidacionGasto> plantilla, String aplicacion, BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) return;
        var art = em.createQuery(
                "SELECT a.id, a.descripcion FROM Articulo a WHERE a.aplicacion = :apl AND a.estado = 'ACTIVO' ORDER BY a.id",
                Object[].class)
            .setParameter("apl", aplicacion).setMaxResults(1).getResultList();
        if (art.isEmpty()) return;   // sin articulo configurado no se puede precargar la categoria
        var g = new LiquidacionGasto();
        g.setArticulo((Long) art.get(0)[0]);
        g.setConcepto((String) art.get(0)[1]);
        g.setMonto(monto);
        plantilla.add(g);
    }

    @Transactional
    public Liquidacion guardar(Liquidacion liq, List<LiquidacionGasto> gastos) {
        boolean esNueva = liq.getId() == null;
        autorizacion.exigir("liquidaciones", esNueva ? "CREAR" : "EDITAR");
        if (liq.getOperacion() == null) throw new NegocioException("La operación es obligatoria");
        // RN-LIQ-003/004 (obs 230): el motivo de la liquidacion es obligatorio
        if (liq.getMotivoCodigo() == null || liq.getMotivoCodigo().isBlank()) {
            throw new NegocioException("El motivo de la liquidación es obligatorio");
        }
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
        String usr = usuarioAuditoria();
        try {
            // Cierre transaccional (obs 229): al liquidar, la operacion pasa a FINALIZADO
            // (con fecha) y el activo vuelve a LIBRE salvo venta consumada. Misma transaccion
            // que la liquidacion: o se hace todo, o nada.
            var op = em.find(py.com.pysistemas.sginmo.dominio.operacion.Operacion.class,
                    liq.getOperacion(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (op == null) throw new NegocioException("La operación no existe");
            if (esNueva && "VIGENTE".equals(op.getEstado())) {
                op.setEstado("FINALIZADO");
                op.setFechaFinalizacion(java.time.LocalDate.now());
            }
            var activo = em.find(py.com.pysistemas.sginmo.dominio.activo.Activo.class, op.getActivo());
            if (activo != null && !"VENDIDA".equals(activo.getEstado())) {
                activo.setEstado("LIBRE");
            }

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
                        + " VALUES (:l, :n, :art, :monto, :usr, now())")
                        .setParameter("l", r.getId()).setParameter("n", item++)
                        .setParameter("art", g.getArticulo()).setParameter("monto", g.getMonto())
                        .setParameter("usr", usr)
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
