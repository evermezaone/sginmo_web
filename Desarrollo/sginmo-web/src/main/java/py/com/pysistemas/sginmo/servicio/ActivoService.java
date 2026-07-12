package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.activo.Activo;
import py.com.pysistemas.sginmo.dominio.activo.ActivoAtributo;
import py.com.pysistemas.sginmo.dominio.activo.ActivoAtributoValor;
import py.com.pysistemas.sginmo.dominio.activo.ActivoPropietario;
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
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class ActivoService {

    private static final Map<String, String> ORDEN = Map.of(
        "nombre", "a.nombre", "tipo", "a.tipo", "estado", "a.estado",
        "precioVenta", "a.precioVenta", "precioAlquiler", "a.precioAlquiler");

    /** Tipos de activo que pueden contener lotes (generacion masiva, REQ-0015). */
    private static final java.util.Set<String> TIPOS_CONTENEDOR_LOTE =
        java.util.Set.of("LOTEAMIENTO", "BARRIO_CERRADO");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Resuelve id de opciones de catalogo (V26: Activo.tipo es id de entidad). */
    @jakarta.inject.Inject
    private CatalogoService catalogoService;

    /** Ids de las opciones TIPOS_ACTIVO que son contenedores de lote. */
    private java.util.Set<Long> tiposContenedorLoteIds() {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (String cod : TIPOS_CONTENEDOR_LOTE) {
            Long id = catalogoService.idOpcion("TIPOS_ACTIVO", cod);
            if (id != null) ids.add(id);
        }
        return ids;
    }

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private AuditoriaFuncionalService auditoria;   // obs 271: auditoria funcional visible

    /** Aislamiento por tenant (F4): activo es transaccional, se filtra tenant = actual. */
    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    // ── Consultas ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(a) FROM Activo a WHERE a.tenant = :t AND (:f = '' OR lower(a.nombre) LIKE :like OR lower(a.direccion) LIKE :like)",
            Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    public List<Activo> listar(int primero, int cantidad, String filtro, String ordenarPor, boolean asc) {
        String ruta = ordenarPor == null ? null : ORDEN.get(ordenarPor);
        var q = em.createQuery(
            "SELECT a FROM Activo a WHERE a.tenant = :t AND (:f = '' OR lower(a.nombre) LIKE :like OR lower(a.direccion) LIKE :like) ORDER BY "
            + (ruta == null ? "a.nombre" : ruta) + (asc ? " ASC" : " DESC"), Activo.class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%").setParameter("t", tenant.actual());
    }

    /** Autocomplete lazy de padre (contenedor): excluye el propio activo Y sus descendientes
     *  (asignar un descendiente como contenedor crearia un ciclo en la jerarquia). */
    public List<Activo> buscarContenedor(String texto, Long exceptoId) {
        String f = texto == null ? "" : texto.trim().toLowerCase();
        java.util.Set<Long> excluir = new java.util.HashSet<>();
        if (exceptoId != null) { excluir.add(exceptoId); excluir.addAll(descendientes(exceptoId)); }
        if (excluir.isEmpty()) excluir.add(-1L);   // JPQL NOT IN no admite coleccion vacia
        return em.createQuery(
                "SELECT a FROM Activo a WHERE a.tenant = :t AND lower(a.nombre) LIKE :like AND a.id NOT IN :excl ORDER BY a.nombre",
                Activo.class)
            .setParameter("t", tenant.actual()).setParameter("like", f + "%").setParameter("excl", excluir)
            .setMaxResults(15).getResultList();
    }

    /** Detalle por id SOLO si el activo es del tenant (obs 254); si no, null (invisible). */
    public Activo buscar(Long id) {
        if (id == null) return null;
        Activo a = em.find(Activo.class, id);
        if (a == null) return null;
        if (!tenant.esSuperadmin() && !tenant.actual().equals(a.getTenant())) return null;
        return a;
    }

    /** Exige que el activo sea del tenant actual (SUPERADMIN sin restriccion). Guarda comun de
     *  las acciones por id (obs 251): aunque la grilla filtre, una llamada manipulada no debe tocar
     *  un activo de otra empresa. */
    private void exigirActivoDelTenant(Long activoId) {
        if (tenant.esSuperadmin()) return;
        Activo a = activoId == null ? null : em.find(Activo.class, activoId);
        if (a == null) throw new NegocioException("El activo no existe");
        if (!tenant.actual().equals(a.getTenant())) {
            throw new NegocioException("El activo pertenece a otra empresa");
        }
    }

    /** Autocomplete de contenedores validos para generar lotes: solo tipos contenedor de lote. */
    public List<Activo> buscarLoteamiento(String texto) {
        String f = texto == null ? "" : texto.trim().toLowerCase();
        return em.createQuery(
                "SELECT a FROM Activo a WHERE a.tenant = :t AND lower(a.nombre) LIKE :like AND a.tipo IN :tipos ORDER BY a.nombre",
                Activo.class)
            .setParameter("t", tenant.actual()).setParameter("like", f + "%").setParameter("tipos", tiposContenedorLoteIds())
            .setMaxResults(15).getResultList();
    }

    /** Combo cerrado para generacion masiva: solo activos tipo LOTEAMIENTO del tenant. */
    public List<Activo> loteamientos() {
        Long loteamiento = catalogoService.idOpcion("TIPOS_ACTIVO", "LOTEAMIENTO");
        if (loteamiento == null) return java.util.List.of();
        return em.createQuery(
                "SELECT a FROM Activo a WHERE a.tenant = :t AND a.tipo = :tipo ORDER BY a.nombre",
                Activo.class)
            .setParameter("t", tenant.actual())
            .setParameter("tipo", loteamiento)
            .getResultList();
    }

    /** Ids de todos los descendientes (subarbol) de un activo, via CTE recursiva. */
    private java.util.Set<Long> descendientes(Long rootId) {
        if (rootId == null) return java.util.Set.of();
        var filas = em.createNativeQuery(
                "WITH RECURSIVE sub AS ("
                + "  SELECT activo FROM activo WHERE padre = :root "
                + "  UNION ALL "
                + "  SELECT a.activo FROM activo a JOIN sub s ON a.padre = s.activo) "
                + "SELECT activo FROM sub")
            .setParameter("root", rootId).getResultList();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (Object o : filas) ids.add(((Number) o).longValue());
        return ids;
    }

    // ── Atributos por tipo (definicion + valor) ──

    @SuppressWarnings("unchecked")
    public List<ActivoAtributoValor> atributosDe(Long activoId, Long tipoId) {
        var filas = em.createNativeQuery(
            "SELECT at.atributo, at.descripcion, at.tipo_dato, apt.obligatorio, "
            + "  aa.activo_atributo, aa.valor "
            + "FROM atributo_por_tipo apt "
            + "JOIN atributo at ON at.atributo = apt.atributo AND at.estado = 'ACTIVO' "
            + "LEFT JOIN activo_atributo aa ON aa.atributo = at.atributo AND aa.activo = :act "
            + "WHERE apt.tipo = :tipo ORDER BY at.descripcion")
            .setParameter("act", activoId == null ? -1L : activoId)
            .setParameter("tipo", tipoId == null ? -1L : tipoId)
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
        if (buscar(activoId) == null) return java.util.List.of();   // solo activos del tenant (obs 254)
        return em.createQuery(
                "SELECT p FROM Persona p WHERE p.id IN "
                + "(SELECT ap.propietario FROM ActivoPropietario ap WHERE ap.activo = :act AND ap.estado = 'ACTIVO') ORDER BY p.nombre",
                Persona.class)
            .setParameter("act", activoId).getResultList();
    }

    public List<Object[]> propietariosConId(Long activoId) {
        if (buscar(activoId) == null) return java.util.List.of();   // solo activos del tenant (obs 254)
        return em.createQuery(
                "SELECT ap.id, p.nombre FROM ActivoPropietario ap, Persona p "
                + "WHERE ap.activo = :act AND p.id = ap.propietario AND ap.estado = 'ACTIVO' ORDER BY p.nombre", Object[].class)
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
        if (activo.getTipo() == null) {
            throw new NegocioException("El tipo de activo es obligatorio");
        }
        if (activo.getPadre() != null) {
            if (activo.getPadre().equals(activo.getId())) {
                throw new NegocioException("Un activo no puede ser su propio contenedor");
            }
            // No permitir asignar como contenedor a un descendiente (crearia un ciclo)
            if (activo.getId() != null && descendientes(activo.getId()).contains(activo.getPadre())) {
                throw new NegocioException("El contenedor no puede ser un activo contenido por este (crearía un ciclo)");
            }
        }
        // Obligatoriedad de atributos por tipo (regla del diseno)
        if (atributos != null) {
            for (var a : atributos) {
                if (a.isObligatorio() && (a.getValor() == null || a.getValor().isBlank())) {
                    throw new NegocioException("El atributo '" + a.getDescripcion() + "' es obligatorio para este tipo");
                }
            }
        }
        // Pertenencia por tenant (F4): el alta toma el tenant del usuario; en edicion el
        // activo debe ser del tenant del contexto y su tenant no se cambia.
        Map<String, Object> antes = null;   // obs 271: snapshot para el diff de auditoria
        if (esNuevo) {
            activo.setTenant(tenant.actual());
        } else {
            Activo enBd = em.find(Activo.class, activo.getId());
            if (enBd == null) throw new NegocioException("El activo no existe");
            if (enBd.getTenant() == null || !enBd.getTenant().equals(tenant.actual())) {
                throw new NegocioException("El activo pertenece a otra empresa");
            }
            antes = snap(enBd);
            activo.setTenant(enBd.getTenant());
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
            if (esNuevo) auditoria.registrarAlta("activo", r.getNombre(), "activos");
            else if (antes != null) auditoria.registrarCambios("activo", r.getNombre(), "activos", null, antes, snap(r));
            return r;
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new NegocioException("El activo fue modificado por otro usuario. Vuelva a abrir el diálogo y reintente.");
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    private Activo persistir(Activo a) { em.persist(a); return a; }

    /** Snapshot de campos auditables del activo (obs 271). */
    private static Map<String, Object> snap(Activo a) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("nombre", a.getNombre());
        m.put("tipo", a.getTipo());
        m.put("padre", a.getPadre());
        m.put("precioVenta", a.getPrecioVenta());
        m.put("precioAlquiler", a.getPrecioAlquiler());
        return m;
    }

    private void guardarValorAtributo(Long activoId, ActivoAtributoValor a) {
        boolean vacio = a.getValor() == null || a.getValor().isBlank();
        if (a.getActivoAtributoId() != null) {
            var ent = em.find(ActivoAtributo.class, a.getActivoAtributoId());
            if (ent == null) return;
            if (vacio) {
                em.remove(ent);
            } else {
                ent.setValor(a.getValor());   // @PreUpdate de Auditable fija el usuario real
            }
        } else if (!vacio) {
            var ent = new ActivoAtributo();
            ent.setActivo(activoId);
            ent.setAtributo(a.getAtributoId());
            ent.setValor(a.getValor());
            em.persist(ent);                  // @PrePersist de Auditable fija el usuario real
        }
    }

    /**
     * Generacion masiva de lotes (REQ-0015): crea N lotes hijos de un contenedor
     * (loteamiento), numerados desde numeroDesde, con manzana, precio y comision comunes.
     * Devuelve la cantidad creada. Omite numeros de lote ya existentes bajo ese padre.
     */
    @Transactional
    public int generarLotes(Long contenedorId, String tipoLoteCodigo, String manzana,
                            int numeroDesde, int cantidad, java.math.BigDecimal precioVenta,
                            java.math.BigDecimal comisionVenta) {
        autorizacion.exigir("activos", "CREAR");
        if (contenedorId == null) {
            throw new NegocioException("Elija el loteamiento contenedor");
        }
        if (cantidad < 1 || cantidad > 500) {
            throw new NegocioException("La cantidad debe estar entre 1 y 500");
        }
        Activo padre = em.find(Activo.class, contenedorId);
        if (padre == null) {
            throw new NegocioException("El contenedor no existe");
        }
        // Pertenencia (obs 251): no se generan lotes bajo un contenedor de otra empresa.
        if (!tenant.esSuperadmin() && !tenant.actual().equals(padre.getTenant())) {
            throw new NegocioException("El contenedor pertenece a otra empresa");
        }
        if (!TIPOS_CONTENEDOR_LOTE.contains(catalogoService.codigoOpcion(padre.getTipo()))) {
            throw new NegocioException("El contenedor debe ser un LOTEAMIENTO o BARRIO_CERRADO; '"
                    + padre.getNombre() + "' no puede contener lotes");
        }
        // Manzana normalizada: null/vacia se tratan como "sin manzana" (una sola vez, para
        // duplicados Y para el guardado, evitando '' vs null inconsistentes).
        String mz = manzana == null || manzana.isBlank() ? null : manzana.trim();
        String tipo = tipoLoteCodigo == null || tipoLoteCodigo.isBlank() ? "LOTE" : tipoLoteCodigo;
        int creados = 0;
        for (int i = 0; i < cantidad; i++) {
            String numero = String.valueOf(numeroDesde + i);
            // Duplicado = mismo padre + mismo numero de lote EN LA MISMA manzana
            // (sin manzana solo colisiona con otros sin manzana; null y '' son equivalentes).
            Long rep = em.createQuery(
                    "SELECT COUNT(a) FROM Activo a WHERE a.padre = :p AND a.numeroLote = :n AND "
                    + "((:m IS NULL AND (a.numeroManzana IS NULL OR a.numeroManzana = '')) OR a.numeroManzana = :m)",
                    Long.class)
                .setParameter("p", contenedorId).setParameter("n", numero).setParameter("m", mz)
                .getSingleResult();
            if (rep > 0) {
                continue;   // ya existe ese lote en esa manzana: no duplicar
            }
            var lote = new Activo();
            lote.setPadre(contenedorId);
            lote.setTipo(catalogoService.idOpcion("TIPOS_ACTIVO", tipo));
            lote.setNombre(padre.getNombre() + " - Lote " + numero
                    + (mz == null ? "" : " Mz " + mz));
            lote.setNumeroLote(numero);
            lote.setNumeroManzana(mz);
            lote.setTenant(padre.getTenant());
            lote.setUbicacion(padre.getUbicacion());
            lote.setPrecioVenta(precioVenta == null ? java.math.BigDecimal.ZERO : precioVenta);
            lote.setComisionVenta(comisionVenta == null ? java.math.BigDecimal.ZERO : comisionVenta);
            em.persist(lote);
            creados++;
        }
        em.flush();
        return creados;
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
        exigirActivoDelTenant(activoId);   // obs 251: no se tocan propietarios de un activo ajeno
        // Pertenencia de la contraparte (obs 253): el propietario debe estar en la cartera del
        // tenant con rol PROPIETARIO ACTIVO; asi no se asocia una identidad de otra empresa.
        Long rolPropietario = catalogoService.idOpcion("ROLES_PERSONA", "PROPIETARIO");
        Long enCartera = em.createQuery(
                "SELECT COUNT(r) FROM PersonaRol r WHERE r.persona = :p AND r.rol = :rol"
                + " AND r.estado = 'ACTIVO' AND (:sa = TRUE OR r.tenant = :t)", Long.class)
            .setParameter("p", propietarioId).setParameter("rol", rolPropietario)
            .setParameter("sa", tenant.esSuperadmin()).setParameter("t", tenant.actual())
            .getSingleResult();
        if (enCartera == 0) {
            throw new NegocioException("El propietario debe tener el rol PROPIETARIO activo en esta empresa");
        }
        // Si ya existe (activo o inactivo) NO se duplica: activo -> error; inactivo -> se reactiva.
        var existentes = em.createQuery(
                "SELECT ap FROM ActivoPropietario ap WHERE ap.activo = :act AND ap.propietario = :pro", ActivoPropietario.class)
            .setParameter("act", activoId).setParameter("pro", propietarioId).getResultList();
        for (var ap : existentes) {
            if ("ACTIVO".equals(ap.getEstado())) throw new NegocioException("Esa persona ya figura como propietaria");
        }
        if (!existentes.isEmpty()) {
            existentes.get(0).setEstado("ACTIVO");   // reactiva preservando el historial
            return;
        }
        var ap = new ActivoPropietario();            // JPA -> Auditable fija el usuario real
        ap.setActivo(activoId);
        ap.setPropietario(propietarioId);
        em.persist(ap);
    }

    @Transactional
    public void quitarPropietario(Long activoPropietarioId) {
        autorizacion.exigir("activos", "EDITAR");
        var ap = em.find(ActivoPropietario.class, activoPropietarioId);
        if (ap == null) throw new NegocioException("El propietario no existe");
        exigirActivoDelTenant(ap.getActivo());   // obs 251: la relacion debe ser de un activo del tenant
        // Baja LOGICA: preserva la trazabilidad historica (operaciones, liquidaciones, reportes).
        ap.setEstado("INACTIVO");
    }
}
