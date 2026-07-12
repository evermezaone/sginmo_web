package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * REQ-0063 - Plantillas de roles/permisos. Aplicar una plantilla a un grupo escribe permiso_grupo
 * (la autorizacion real del backend, que lee SesionUsuario.puede); NUNCA cambia el perfil del usuario
 * (no concede SUPERADMIN). Muestra el diff antes de aplicar; no borra ajustes salvo modo "reemplazar".
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class RolPlantillaService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;
    @jakarta.inject.Inject
    private TenantContext tenant;

    public List<Fila> plantillas() {
        List<Fila> out = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> f = em.createNativeQuery(
            "SELECT rol_plantilla, codigo, descripcion FROM rol_plantilla WHERE estado='ACTIVO' ORDER BY codigo")
            .getResultList();
        for (Object[] r : f) out.add(new Fila(((Number) r[0]).longValue(), (String) r[1], (String) r[2]));
        return out;
    }

    public List<Fila> grupos() {
        List<Fila> out = new ArrayList<>();
        Long emp = tenant.actual();
        @SuppressWarnings("unchecked")
        List<Object[]> f = em.createNativeQuery(
            "SELECT grupo, codigo, descripcion FROM grupo WHERE estado='ACTIVO' AND tenant=:t ORDER BY codigo")
            .setParameter("t", emp == null ? -1L : emp).getResultList();
        for (Object[] r : f) out.add(new Fila(((Number) r[0]).longValue(), (String) r[1], (String) r[2]));
        return out;
    }

    private Set<String> permisosPlantilla(Long plantillaId) {
        Set<String> s = new LinkedHashSet<>();
        // Obs 266 (aislamiento multiempresa): rol_plantilla_permiso NO tiene RLS; se lee via JOIN a
        // rol_plantilla (que SI tiene RLS, V44) para que solo devuelva el detalle de una plantilla
        // visible al tenant (global -1 o propia). El guard exigirPlantillaVisible refuerza con mensaje claro.
        @SuppressWarnings("unchecked")
        List<Object[]> f = em.createNativeQuery(
            "SELECT pp.pantalla, pp.accion FROM rol_plantilla_permiso pp"
          + " JOIN rol_plantilla rp ON rp.rol_plantilla = pp.rol_plantilla"
          + " WHERE pp.rol_plantilla=:p ORDER BY pp.pantalla, pp.accion")
            .setParameter("p", plantillaId).getResultList();
        for (Object[] r : f) s.add(r[0] + ":" + r[1]);
        return s;
    }

    /** Obs 266: la plantilla debe ser visible al tenant (global -1 o propia) segun la RLS de rol_plantilla. */
    private void exigirPlantillaVisible(Long plantillaId) {
        if (plantillaId == null) throw new NegocioException("Seleccione una plantilla");
        Object v = em.createNativeQuery("SELECT 1 FROM rol_plantilla WHERE rol_plantilla=:p AND estado='ACTIVO'")
                .setParameter("p", plantillaId).getResultStream().findFirst().orElse(null);
        if (v == null) throw new NegocioException("La plantilla no existe o no pertenece a su empresa");
    }

    private Set<String> permisosGrupo(Long grupoId) {
        Set<String> s = new LinkedHashSet<>();
        @SuppressWarnings("unchecked")
        List<Object[]> f = em.createNativeQuery(
            "SELECT pantalla, accion FROM permiso_grupo WHERE grupo=:g ORDER BY pantalla, accion")
            .setParameter("g", grupoId).getResultList();
        for (Object[] r : f) s.add(r[0] + ":" + r[1]);
        return s;
    }

    /** Diferencia entre lo que trae la plantilla y lo que ya tiene el grupo. */
    public Diff diff(Long plantillaId, Long grupoId) {
        exigirPlantillaVisible(plantillaId);
        exigirGrupoDelTenant(grupoId);
        Diff d = new Diff();
        Set<String> plantilla = permisosPlantilla(plantillaId);
        Set<String> grupo = permisosGrupo(grupoId);
        for (String p : plantilla) if (!grupo.contains(p)) d.agregar.add(p);
        for (String g : grupo) if (!plantilla.contains(g)) d.quitar.add(g);
        return d;
    }

    /**
     * Aplica la plantilla al grupo. Con reemplazar=false solo AGREGA (no borra ajustes existentes);
     * con reemplazar=true deja el grupo exactamente igual a la plantilla. Requiere permiso grupos/EDITAR.
     */
    @Transactional
    public void aplicar(Long plantillaId, Long grupoId, boolean reemplazar) {
        autorizacion.exigir("grupos", "EDITAR");
        exigirPlantillaVisible(plantillaId);
        exigirGrupoDelTenant(grupoId);
        Set<String> plantilla = permisosPlantilla(plantillaId);
        Set<String> grupo = permisosGrupo(grupoId);
        String usr = sesion.codigoUsuario();

        for (String p : plantilla) {
            if (grupo.contains(p)) continue;
            String[] pa = p.split(":", 2);
            em.createNativeQuery(
                "INSERT INTO permiso_grupo (grupo, pantalla, accion, usuario_creacion, fecha_creacion)"
              + " VALUES (:g,:pa,:ac,:u, now())")
                .setParameter("g", grupoId).setParameter("pa", pa[0]).setParameter("ac", pa[1])
                .setParameter("u", usr).executeUpdate();
        }
        if (reemplazar) {
            for (String g : grupo) {
                if (plantilla.contains(g)) continue;
                String[] pa = g.split(":", 2);
                em.createNativeQuery("DELETE FROM permiso_grupo WHERE grupo=:g AND pantalla=:pa AND accion=:ac")
                    .setParameter("g", grupoId).setParameter("pa", pa[0]).setParameter("ac", pa[1]).executeUpdate();
            }
        }
    }

    /** Verifica que el grupo pertenezca al tenant actual (grupo no tiene RLS). */
    private void exigirGrupoDelTenant(Long grupoId) {
        Long emp = tenant.actual();
        Object t = em.createNativeQuery("SELECT tenant FROM grupo WHERE grupo=:g").setParameter("g", grupoId)
                .getResultStream().findFirst().orElse(null);
        if (t == null) throw new NegocioException("El grupo no existe");
        Long tg = ((Number) t).longValue();
        if (emp != null && !TenantContext.GLOBAL.equals(emp) && !emp.equals(tg)) {
            throw new NegocioException("El grupo no pertenece a su empresa");
        }
    }

    public record Fila(Long id, String codigo, String descripcion) {
        public Long getId() { return id; }
        public String getCodigo() { return codigo; }
        public String getDescripcion() { return descripcion; }
    }
    public static class Diff {
        public final List<String> agregar = new ArrayList<>();
        public final List<String> quitar = new ArrayList<>();
        public List<String> getAgregar() { return agregar; }
        public List<String> getQuitar() { return quitar; }
    }
}
