package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REQ-0060 - Lectura de parametros desde los servicios (no constantes). Resuelve el valor efectivo:
 * el valor de la empresa (tenant) tiene prioridad sobre el default global (-1). Cachea por
 * (tenant, clave); ParametroService.guardar invalida la cache. Un parametro ausente o mal tipado
 * cae al valor por defecto pasado por el llamador (no rompe cobros/montos/documentos).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class ParametroConfig {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private TenantContext tenant;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** Valor efectivo del parametro (empresa sobre global). null si no existe en ningun nivel. */
    public String texto(String clave, String porDefecto) {
        Long emp = tenant.actual();
        String key = emp + ":" + clave;
        String v = cache.computeIfAbsent(key, k -> leer(clave, emp));
        return (v == null || "\0NULL".equals(v)) ? porDefecto : v;
    }

    public int entero(String clave, int porDefecto) {
        try { String v = texto(clave, null); return v == null ? porDefecto : Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return porDefecto; }
    }

    public BigDecimal decimal(String clave, BigDecimal porDefecto) {
        try { String v = texto(clave, null); return v == null ? porDefecto : new BigDecimal(v.trim()); }
        catch (NumberFormatException e) { return porDefecto; }
    }

    public boolean booleano(String clave, boolean porDefecto) {
        String v = texto(clave, null);
        if (v == null) return porDefecto;
        v = v.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "si".equals(v) || "s".equals(v);
    }

    /** Invalida la cache (llamar tras guardar un parametro). */
    public void invalidar() { cache.clear(); }

    private String leer(String clave, Long emp) {
        try {
            Object r = em.createNativeQuery(
                "SELECT valor FROM parametro_sistema WHERE clave = :c AND tenant IN (:t, -1)"
              + " ORDER BY tenant DESC LIMIT 1")
                .setParameter("c", clave).setParameter("t", emp == null ? -1L : emp)
                .getSingleResult();
            return r == null ? "\0NULL" : r.toString();
        } catch (jakarta.persistence.NoResultException e) {
            return "\0NULL";
        } catch (RuntimeException e) {
            return "\0NULL";
        }
    }
}
