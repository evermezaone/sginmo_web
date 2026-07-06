package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.operacion.IngresoEgreso;

import java.math.BigDecimal;
import java.util.List;

/** ABM de ingresos/egresos de caja (REQ-0024): gastos y otros ingresos. */
@ApplicationScoped
public class IngresoEgresoService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    public long contar(String filtro, String tipo) {
        var q = em.createQuery(
            "SELECT COUNT(ie) FROM IngresoEgreso ie WHERE (:t = '' OR ie.tipo = :t)"
            + " AND (:f = '' OR lower(ie.observacion) LIKE :like)", Long.class);
        q.setParameter("t", tipo == null ? "" : tipo);
        aplicar(q, filtro);
        return q.getSingleResult();
    }

    public List<IngresoEgreso> listar(int primero, int cantidad, String filtro, String tipo) {
        var q = em.createQuery(
            "SELECT ie FROM IngresoEgreso ie WHERE (:t = '' OR ie.tipo = :t)"
            + " AND (:f = '' OR lower(ie.observacion) LIKE :like) ORDER BY ie.id DESC", IngresoEgreso.class);
        q.setParameter("t", tipo == null ? "" : tipo);
        aplicar(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void aplicar(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    /** Concepto (nombre del articulo) para mostrar en la grilla. */
    public String conceptoDe(Long articuloId) {
        var r = em.createQuery("SELECT a.descripcion FROM Articulo a WHERE a.id = :id", String.class)
            .setParameter("id", articuloId).getResultList();
        return r.isEmpty() ? "" : r.get(0);
    }

    @Transactional
    public IngresoEgreso guardar(IngresoEgreso ie) {
        boolean esNuevo = ie.getId() == null;
        autorizacion.exigir("ingresos-egresos", esNuevo ? "CREAR" : "EDITAR");
        if (ie.getArticulo() == null) throw new NegocioException("El concepto (artículo) es obligatorio");
        if (ie.getMonto() == null || ie.getMonto().signum() <= 0) {
            throw new NegocioException("El monto debe ser mayor a cero");
        }
        if (ie.getEmpresa() == null) throw new NegocioException("Falta el contexto de empresa");
        if (ie.getEstado() == null) ie.setEstado("CANCELADO");
        // contado: saldo 0; a credito quedaria PENDIENTE con saldo=monto (simple: contado)
        if ("CANCELADO".equals(ie.getEstado())) ie.setSaldo(BigDecimal.ZERO);
        try {
            IngresoEgreso r = esNuevo ? persistir(ie) : em.merge(ie);
            em.flush();
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("El movimiento fue modificado por otro usuario. Reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private IngresoEgreso persistir(IngresoEgreso ie) { em.persist(ie); return ie; }

    @Transactional
    public void anular(Long id) {
        autorizacion.exigir("ingresos-egresos", "INACTIVAR");
        IngresoEgreso ie = em.find(IngresoEgreso.class, id);
        if (ie == null) throw new NegocioException("El movimiento no existe");
        if ("ANULADO".equals(ie.getEstado())) throw new NegocioException("Ya está anulado");
        ie.setEstado("ANULADO");
    }
}
