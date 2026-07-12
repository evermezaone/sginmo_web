package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.FormaPago;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ABM de formas de pago (contrato estandar) con habilitado y "por defecto" unico. */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class FormaPagoService {

    private static final Map<String, String> ORDEN = Map.of(
        "codigo", "fp.codigo", "descripcion", "fp.descripcion", "estado", "fp.estado");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Enforcement de permisos en la capa de servicio (obs 203 de Codex). */
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private AuditoriaFuncionalService auditoria;   // obs 269: auditoria funcional visible

    public long contar(String filtro) {
        var q = em.createQuery("SELECT COUNT(fp) FROM FormaPago fp WHERE (:f = '' OR lower(fp.codigo) LIKE :like OR lower(fp.descripcion) LIKE :like)", Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<FormaPago> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery("SELECT fp FROM FormaPago fp WHERE (:f = '' OR lower(fp.codigo) LIKE :like OR lower(fp.descripcion) LIKE :like) ORDER BY "
                + (ruta == null ? "fp.descripcion" : ruta) + (asc ? " ASC" : " DESC"), FormaPago.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    public boolean existeCodigo(String codigo, Long exceptoId) {
        if (codigo == null || codigo.isBlank()) return false;
        return em.createQuery("SELECT COUNT(fp) FROM FormaPago fp WHERE lower(fp.codigo) = :c AND (:id IS NULL OR fp.id <> :id)", Long.class)
            .setParameter("c", codigo.trim().toLowerCase()).setParameter("id", exceptoId)
            .getSingleResult() > 0;
    }

    @Transactional
    public FormaPago guardar(FormaPago fp) {
        boolean nuevo = fp.getId() == null;
        autorizacion.exigir("formas-pago", nuevo ? "CREAR" : "EDITAR");
        validar(fp);
        Map<String, Object> antes = null;   // obs 269: snapshot para el diff de auditoria
        if (!nuevo) {
            FormaPago o = em.find(FormaPago.class, fp.getId());
            if (o != null) antes = snap(o);
        }
        try {
            if (fp.isPorDefecto()) {
                // "por defecto" es unico: se apaga en las demas
                em.createQuery("UPDATE FormaPago x SET x.porDefecto = false WHERE x.porDefecto = true AND (:id IS NULL OR x.id <> :id)")
                    .setParameter("id", fp.getId()).executeUpdate();
            }
            FormaPago r = nuevo ? persistir(fp) : em.merge(fp);
            em.flush();
            if (nuevo) auditoria.registrarAlta("forma_pago", r.getCodigo(), "formas-pago");
            else if (antes != null) auditoria.registrarCambios("forma_pago", r.getCodigo(), "formas-pago", null, antes, snap(r));
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("La forma de pago fue modificada por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private FormaPago persistir(FormaPago fp) { em.persist(fp); return fp; }

    @Transactional
    public void cambiarEstado(Long id, String estadoNuevo) {
        boolean reactivar = "ACTIVO".equals(estadoNuevo);
        autorizacion.exigir("formas-pago", reactivar ? "REACTIVAR" : "INACTIVAR");
        FormaPago fp = em.find(FormaPago.class, id);
        if (fp == null) throw new NegocioException("La forma de pago no existe");
        if (reactivar) validar(fp);
        String estadoAnterior = fp.getEstado();
        fp.setEstado(estadoNuevo);
        auditoria.registrar("forma_pago", fp.getCodigo(),
                reactivar ? AuditoriaFuncionalService.REACTIVAR : AuditoriaFuncionalService.INACTIVAR,
                "formas-pago", "estado " + estadoAnterior + " -> " + estadoNuevo);
    }

    /** Snapshot de campos auditables (obs 269). */
    private static Map<String, Object> snap(FormaPago fp) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", fp.getCodigo());
        m.put("descripcion", fp.getDescripcion());
        m.put("estado", fp.getEstado());
        m.put("porDefecto", fp.isPorDefecto());
        m.put("dias", fp.getDias());
        return m;
    }

    private void validar(FormaPago fp) {
        if (existeCodigo(fp.getCodigo(), fp.getId())) {
            throw new NegocioException("Ya existe una forma de pago con el código '" + fp.getCodigo() + "'");
        }
        if (fp.getDias() != null && fp.getDias() < 0) {
            throw new NegocioException("Los días no pueden ser negativos");
        }
    }
}
