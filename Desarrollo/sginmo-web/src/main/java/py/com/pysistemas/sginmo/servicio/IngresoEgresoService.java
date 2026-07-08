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

    /** Aislamiento multiempresa (obs 228): la grilla SOLO ve movimientos de la empresa del contexto. */
    public long contar(Long empresa, String filtro, String tipo) {
        if (empresa == null) return 0;
        var q = em.createQuery(
            "SELECT COUNT(ie) FROM IngresoEgreso ie WHERE ie.empresa = :emp AND (:t = '' OR ie.tipo = :t)"
            + " AND (:f = '' OR lower(ie.observacion) LIKE :like)", Long.class);
        q.setParameter("emp", empresa).setParameter("t", tipo == null ? "" : tipo);
        aplicar(q, filtro);
        return q.getSingleResult();
    }

    public List<IngresoEgreso> listar(Long empresa, int primero, int cantidad, String filtro, String tipo) {
        if (empresa == null) return java.util.List.of();
        var q = em.createQuery(
            "SELECT ie FROM IngresoEgreso ie WHERE ie.empresa = :emp AND (:t = '' OR ie.tipo = :t)"
            + " AND (:f = '' OR lower(ie.observacion) LIKE :like) ORDER BY ie.id DESC", IngresoEgreso.class);
        q.setParameter("emp", empresa).setParameter("t", tipo == null ? "" : tipo);
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
    public IngresoEgreso guardar(IngresoEgreso ie, Long empresaContexto) {
        boolean esNuevo = ie.getId() == null;
        autorizacion.exigir("ingresos-egresos", esNuevo ? "CREAR" : "EDITAR");
        if (ie.getArticulo() == null) throw new NegocioException("El concepto (artículo) es obligatorio");
        if (ie.getMonto() == null || ie.getMonto().signum() <= 0) {
            throw new NegocioException("El monto debe ser mayor a cero");
        }
        if (ie.getEmpresa() == null) throw new NegocioException("Falta el contexto de empresa");
        // Aislamiento (obs 228): el movimiento debe ser de la empresa del contexto; en edicion,
        // ademas, la fila existente en BD debe pertenecerle (no se editan movimientos ajenos).
        if (empresaContexto == null || !empresaContexto.equals(ie.getEmpresa())) {
            throw new NegocioException("El movimiento no pertenece a la empresa del contexto");
        }
        if (!esNuevo) {
            IngresoEgreso enBd = em.find(IngresoEgreso.class, ie.getId());
            if (enBd == null) throw new NegocioException("El movimiento no existe");
            if (!empresaContexto.equals(enBd.getEmpresa())) {
                throw new NegocioException("El movimiento pertenece a otra empresa");
            }
        }
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
    public void anular(Long id, Long empresaContexto) {
        autorizacion.exigir("ingresos-egresos", "INACTIVAR");
        IngresoEgreso ie = em.find(IngresoEgreso.class, id);
        if (ie == null) throw new NegocioException("El movimiento no existe");
        // Aislamiento (obs 228): no se anulan movimientos de otra empresa
        if (empresaContexto == null || !empresaContexto.equals(ie.getEmpresa())) {
            throw new NegocioException("El movimiento pertenece a otra empresa");
        }
        if ("ANULADO".equals(ie.getEstado())) throw new NegocioException("Ya está anulado");
        ie.setEstado("ANULADO");
    }
}
