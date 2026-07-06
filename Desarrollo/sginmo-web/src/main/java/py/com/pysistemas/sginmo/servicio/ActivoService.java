package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.activo.Activo;
import py.com.pysistemas.sginmo.dominio.activo.ActivoAtributoValor;
import py.com.pysistemas.sginmo.dominio.persona.Persona;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ABM de activos inmobiliarios (REQ-0013/0014): recursivo, con atributos parametrizables
 * por tipo (obligatoriedad validada al guardar) y propietarios. El estado LIBRE/OCUPADA/
 * VENDIDA lo mueven las operaciones (REQ-0016+), aqui es de solo lectura.
 */
@ApplicationScoped
public class ActivoService {

    private static final Map<String, String> ORDEN = Map.of(
        "nombre", "a.nombre", "tipoCodigo", "a.tipoCodigo", "estado", "a.estado",
        "precioVenta", "a.precioVenta", "precioAlquiler", "a.precioAlquiler");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    // ── Consultas ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(a) FROM Activo a WHERE (:f = '' OR lower(a.nombre) LIKE :like OR lower(a.direccion) LIKE :like)",
            Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<Activo> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery(
            "SELECT a FROM Activo a WHERE (:f = '' OR lower(a.nombre) LIKE :like OR lower(a.direccion) LIKE :like) ORDER BY "
            + (ruta == null ? "a.nombre" : ruta) + (asc ? " ASC" : " DESC"), Activo.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%");
    }

    /** Autocomplete lazy de padre (contenedor): activos distintos del propio. */
    public List<Activo> buscarContenedor(String texto, Long exceptoId) {
        String f = texto == null ? "" : texto.trim().toLowerCase();
        return em.createQuery(
                "SELECT a FROM Activo a WHERE lower(a.nombre) LIKE :like AND (:id IS NULL OR a.id <> :id) ORDER BY a.nombre",
                Activo.class)
            .setParameter("like", f + "%").setParameter("id", exceptoId)
            .setMaxResults(15).getResultList();
    }

    public Activo buscar(Long id) { return id == null ? null : em.find(Activo.class, id); }

    // ── Atributos por tipo (definicion + valor) ──

    @SuppressWarnings("unchecked")
    public List<ActivoAtributoValor> atributosDe(Long activoId, String tipoCodigo) {
        var filas = em.createNativeQuery(
            "SELECT at.atributo, at.descripcion, at.tipo_dato, apt.obligatorio, "
            + "  aa.activo_atributo, aa.valor "
            + "FROM atributo_por_tipo apt "
            + "JOIN atributo at ON at.atributo = apt.atributo AND at.estado = 'ACTIVO' "
            + "LEFT JOIN activo_atributo aa ON aa.atributo = at.atributo AND aa.activo = :act "
            + "WHERE apt.tipo_codigo = :tipo ORDER BY at.descripcion")
            .setParameter("act", activoId == null ? -1L : activoId)
            .setParameter("tipo", tipoCodigo == null ? "" : tipoCodigo)
            .getResultList();
        List<ActivoAtributoValor> res = new ArrayList<>();
        for (Object fo : filas) {
            Object[] f = (Object[]) fo;
            var v = new ActivoAtributoValor();
            v.setAtributoId(((Number) f[0]).longValue());
            v.setDescripcion((String) f[1]);
            v.setTipoDato((String) f[2]);
            v.setObligatorio(Boolean.TRUE.equals(f[3]));
            v.setActivoAtributoId(f[4] == null ? null : ((Number) f[4]).longValue());
            v.setValor((String) f[5]);
            res.add(v);
        }
        return res;
    }

    public List<Persona> propietariosDe(Long activoId) {
        return em.createQuery(
                "SELECT p FROM Persona p WHERE p.id IN "
                + "(SELECT ap.propietario FROM ActivoPropietario ap WHERE ap.activo = :act) ORDER BY p.nombre",
                Persona.class)
            .setParameter("act", activoId).getResultList();
    }

    public List<Object[]> propietariosConId(Long activoId) {
        return em.createQuery(
                "SELECT ap.id, p.nombre FROM ActivoPropietario ap, Persona p "
                + "WHERE ap.activo = :act AND p.id = ap.propietario ORDER BY p.nombre", Object[].class)
            .setParameter("act", activoId).getResultList();
    }

    // ── Escrituras ──

    @Transactional
    public Activo guardar(Activo activo, List<ActivoAtributoValor> atributos) {
        boolean esNuevo = activo.getId() == null;
        autorizacion.exigir("activos", esNuevo ? "CREAR" : "EDITAR");
        if (activo.getNombre() == null || activo.getNombre().isBlank()) {
            throw new NegocioException("El nombre es obligatorio");
        }
        if (activo.getTipoCodigo() == null || activo.getTipoCodigo().isBlank()) {
            throw new NegocioException("El tipo de activo es obligatorio");
        }
        if (activo.getPadre() != null && activo.getPadre().equals(activo.getId())) {
            throw new NegocioException("Un activo no puede ser su propio contenedor");
        }
        // Obligatoriedad de atributos por tipo (regla del diseno)
        if (atributos != null) {
            for (var a : atributos) {
                if (a.isObligatorio() && (a.getValor() == null || a.getValor().isBlank())) {
                    throw new NegocioException("El atributo '" + a.getDescripcion() + "' es obligatorio para este tipo");
                }
            }
        }
        try {
            Activo r = esNuevo ? persistir(activo) : em.merge(activo);
            em.flush();
            if (atributos != null) {
                for (var a : atributos) {
                    guardarValorAtributo(r.getId(), a);
                }
            }
            em.flush();
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("El activo fue modificado por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private Activo persistir(Activo a) { em.persist(a); return a; }

    private void guardarValorAtributo(Long activoId, ActivoAtributoValor a) {
        boolean vacio = a.getValor() == null || a.getValor().isBlank();
        if (a.getActivoAtributoId() != null) {
            if (vacio) {
                em.createNativeQuery("DELETE FROM activo_atributo WHERE activo_atributo = :id")
                    .setParameter("id", a.getActivoAtributoId()).executeUpdate();
            } else {
                em.createNativeQuery("UPDATE activo_atributo SET valor = :v, "
                        + "usuario_modificacion = 'sistema', fecha_modificacion = now() WHERE activo_atributo = :id")
                    .setParameter("v", a.getValor()).setParameter("id", a.getActivoAtributoId()).executeUpdate();
            }
        } else if (!vacio) {
            em.createNativeQuery("INSERT INTO activo_atributo (activo, atributo, valor, usuario_creacion, fecha_creacion) "
                    + "VALUES (:act, :atr, :v, 'sistema', now())")
                .setParameter("act", activoId).setParameter("atr", a.getAtributoId())
                .setParameter("v", a.getValor()).executeUpdate();
        }
    }

    @Transactional
    public void cambiarEstadoLogico(Long id, boolean inactivar) {
        // Los activos no tienen estado ACTIVO/INACTIVO (usan LIBRE/OCUPADA/VENDIDA operativo).
        // La "baja" real la maneja la operacion; aqui se deja el hook para futuro si hace falta.
        throw new NegocioException("El estado del activo lo determinan las operaciones");
    }

    // ── Propietarios ──

    @Transactional
    public void agregarPropietario(Long activoId, Long propietarioId) {
        autorizacion.exigir("activos", "EDITAR");
        if (propietarioId == null) throw new NegocioException("Elija el propietario");
        Long rep = em.createQuery(
                "SELECT COUNT(ap) FROM ActivoPropietario ap WHERE ap.activo = :act AND ap.propietario = :pro", Long.class)
            .setParameter("act", activoId).setParameter("pro", propietarioId).getSingleResult();
        if (rep > 0) throw new NegocioException("Esa persona ya figura como propietaria");
        em.createNativeQuery("INSERT INTO activo_propietario (activo, propietario, usuario_creacion, fecha_creacion) "
                + "VALUES (:act, :pro, 'sistema', now())")
            .setParameter("act", activoId).setParameter("pro", propietarioId).executeUpdate();
    }

    @Transactional
    public void quitarPropietario(Long activoPropietarioId) {
        autorizacion.exigir("activos", "EDITAR");
        em.createNativeQuery("DELETE FROM activo_propietario WHERE activo_propietario = :id")
            .setParameter("id", activoPropietarioId).executeUpdate();
    }
}
