package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.Impuesto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * ABM de impuestos (contrato estandar). Modo SIMPLIFICADO (parametro
 * IMPUESTOS_MODO_AVANZADO='NO', decision del usuario): el administrador carga solo
 * descripcion y porcentaje; los factores se calculan solos (base gravada 100%).
 * Modo avanzado: se editan factores y porcentaje de base gravada (regimenes 20%/30%).
 */
@ApplicationScoped
public class ImpuestoService {

    private static final Map<String, String> ORDEN = Map.of(
        "descripcion", "i.descripcion", "porcentajeImpuesto", "i.porcentajeImpuesto", "estado", "i.estado");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    public long contar(String filtro) {
        var q = em.createQuery("SELECT COUNT(i) FROM Impuesto i WHERE (:f = '' OR lower(i.descripcion) LIKE :like)", Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<Impuesto> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery("SELECT i FROM Impuesto i WHERE (:f = '' OR lower(i.descripcion) LIKE :like) ORDER BY "
                + (ruta == null ? "i.descripcion" : ruta) + (asc ? " ASC" : " DESC"), Impuesto.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    public boolean modoAvanzado() {
        var filas = em.createNativeQuery("SELECT valor FROM parametro_sistema WHERE clave = 'IMPUESTOS_MODO_AVANZADO'").getResultList();
        return !filas.isEmpty() && "SI".equalsIgnoreCase(String.valueOf(filas.get(0)));
    }

    @Transactional
    public Impuesto guardar(Impuesto impuesto, boolean avanzado) {
        autorizacion.exigir("impuestos", impuesto.getId() == null ? "CREAR" : "EDITAR");
        if (impuesto.getDescripcion() == null || impuesto.getDescripcion().isBlank()) {
            throw new NegocioException("La descripción es obligatoria");
        }
        Long repetidos = em.createQuery("SELECT COUNT(i) FROM Impuesto i WHERE lower(i.descripcion) = :d AND (:id IS NULL OR i.id <> :id)", Long.class)
            .setParameter("d", impuesto.getDescripcion().trim().toLowerCase()).setParameter("id", impuesto.getId())
            .getSingleResult();
        if (repetidos > 0) throw new NegocioException("Ya existe el impuesto '" + impuesto.getDescripcion() + "'");
        BigDecimal p = impuesto.getPorcentajeImpuesto();
        if (p == null || p.signum() < 0) throw new NegocioException("El porcentaje no puede ser negativo");
        if (!avanzado) {
            // Modo simplificado: base gravada 100% y factores derivados con la semantica
            // del seed V2 / Gestion (obs 204 de Codex): factor_DISCRIMINADO = divisor del
            // total para obtener el impuesto (IVA 10 -> 11; IVA 5 -> 21) y factor_IMPUESTO
            // = multiplicador neto->total (IVA 10 -> 1.10). Exenta: 0 / 1.00.
            impuesto.setPorcentajeBaseGravada(new BigDecimal("100"));
            if (p.signum() == 0) {           // exenta (seed: discriminado 0, impuesto 1.00)
                impuesto.setFactorDiscriminado(BigDecimal.ZERO);
                impuesto.setFactorImpuesto(BigDecimal.ONE);
            } else {
                var cien = new BigDecimal("100");
                impuesto.setFactorDiscriminado(cien.add(p).divide(p, 2, RoundingMode.HALF_UP));
                impuesto.setFactorImpuesto(cien.add(p).divide(cien, 2, RoundingMode.HALF_UP));
            }
        } else {
            if (impuesto.getFactorImpuesto() == null || impuesto.getFactorDiscriminado() == null
                    || impuesto.getPorcentajeBaseGravada() == null) {
                throw new NegocioException("En modo avanzado los factores y la base gravada son obligatorios");
            }
        }
        try {
            Impuesto r = impuesto.getId() == null ? persistir(impuesto) : em.merge(impuesto);
            em.flush();
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("El impuesto fue modificado por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private Impuesto persistir(Impuesto i) { em.persist(i); return i; }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        autorizacion.exigir("impuestos", "ACTIVO".equals(estadoNuevo) ? "REACTIVAR" : "INACTIVAR");
        Impuesto i = em.find(Impuesto.class, id);
        if (i == null) throw new NegocioException("El impuesto no existe");
        i.setEstado(estadoNuevo);
    }
}
