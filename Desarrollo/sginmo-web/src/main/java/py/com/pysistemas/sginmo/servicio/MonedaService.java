package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.Moneda;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ABM de monedas (contrato estandar). */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class MonedaService {

    private static final Map<String, String> ORDEN = Map.of(
        "descripcion", "m.descripcion", "simbolo", "m.simbolo",
        "tipoMoneda", "m.tipoMoneda", "estado", "m.estado");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private AuditoriaFuncionalService auditoria;   // obs 271: auditoria funcional visible

    public long contar(String filtro) {
        var q = em.createQuery("SELECT COUNT(m) FROM Moneda m WHERE (:f = '' OR lower(m.descripcion) LIKE :like OR lower(m.simbolo) LIKE :like)", Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<Moneda> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery("SELECT m FROM Moneda m WHERE (:f = '' OR lower(m.descripcion) LIKE :like OR lower(m.simbolo) LIKE :like) ORDER BY "
                + (ruta == null ? "m.descripcion" : ruta) + (asc ? " ASC" : " DESC"), Moneda.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    public boolean existeDescripcion(String descripcion, Long exceptoId) {
        if (descripcion == null || descripcion.isBlank()) return false;
        return em.createQuery("SELECT COUNT(m) FROM Moneda m WHERE lower(m.descripcion) = :d AND (:id IS NULL OR m.id <> :id)", Long.class)
            .setParameter("d", descripcion.trim().toLowerCase()).setParameter("id", exceptoId)
            .getSingleResult() > 0;
    }

    @Transactional
    public Moneda guardar(Moneda moneda) {
        boolean nuevo = moneda.getId() == null;
        autorizacion.exigir("monedas", nuevo ? "CREAR" : "EDITAR");
        validar(moneda);
        Map<String, Object> antes = null;   // obs 271: snapshot para el diff de auditoria
        if (!nuevo) {
            Moneda o = em.find(Moneda.class, moneda.getId());
            if (o != null) antes = snap(o);
        }
        try {
            Moneda r = nuevo ? persistir(moneda) : em.merge(moneda);
            em.flush();
            if (nuevo) auditoria.registrarAlta("moneda", r.getDescripcion(), "monedas");
            else if (antes != null) auditoria.registrarCambios("moneda", r.getDescripcion(), "monedas", null, antes, snap(r));
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La moneda fue modificada por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private Moneda persistir(Moneda m) { em.persist(m); return m; }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        boolean reactivar = "ACTIVO".equals(estadoNuevo);
        autorizacion.exigir("monedas", reactivar ? "REACTIVAR" : "INACTIVAR");
        Moneda m = em.find(Moneda.class, id);
        if (m == null) throw new NegocioException("La moneda no existe");
        if (reactivar) validar(m);
        String estadoAnterior = m.getEstado();
        m.setEstado(estadoNuevo);
        auditoria.registrar("moneda", m.getDescripcion(),
                reactivar ? AuditoriaFuncionalService.REACTIVAR : AuditoriaFuncionalService.INACTIVAR,
                "monedas", "estado " + estadoAnterior + " -> " + estadoNuevo);
    }

    /** Snapshot de campos auditables (obs 271). */
    private static Map<String, Object> snap(Moneda m) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("descripcion", m.getDescripcion());
        x.put("simbolo", m.getSimbolo());
        x.put("estado", m.getEstado());
        return x;
    }

    private void validar(Moneda m) {
        if (existeDescripcion(m.getDescripcion(), m.getId())) {
            throw new NegocioException("Ya existe una moneda '" + m.getDescripcion() + "'");
        }
        if (m.getPrecisionDecimales() == null || m.getPrecisionDecimales() < 0 || m.getPrecisionDecimales() > 6) {
            throw new NegocioException("La precisión decimal debe estar entre 0 y 6");
        }
    }
}
