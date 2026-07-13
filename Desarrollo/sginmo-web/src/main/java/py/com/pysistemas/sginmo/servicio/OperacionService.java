package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.activo.Activo;
import py.com.pysistemas.sginmo.dominio.operacion.CronogramaCuota;
import py.com.pysistemas.sginmo.dominio.operacion.IngresoEgreso;
import py.com.pysistemas.sginmo.dominio.operacion.Operacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Operaciones de alquiler/venta (REQ-0016/0017/0018/0019/0020/0021).
 * TODA la matematica financiera vive en la BD (V16/V17): este servicio orquesta e
 * INVOCA f_generar_cronograma / f_mora_cuota; jamas recalcula cuadres en Java.
 * Movimientos automaticos (REQ-0018): documento interno de deposito de garantia
 * (alquiler) y de comision (segun % del activo) al confirmar la operacion.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class OperacionService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private AuditoriaFuncionalService auditoria;   // obs 271: auditoria funcional visible

    @jakarta.inject.Inject
    private py.com.one.core.UsuarioActual usuarioActual;

    /** Aislamiento por tenant (F4): las operaciones son transaccionales, tenant = actual. */
    @jakarta.inject.Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    /** Usuario autenticado para auditar las escrituras nativas (documentos, cuotas, cronograma,
     *  rescision). Fallback 'sistema' solo sin sesion (batch/tests), igual que AuditoriaListener. */
    private String usuarioAuditoria() {
        try {
            String u = usuarioActual.codigoUsuario();
            return (u == null || u.isBlank()) ? py.com.one.core.UsuarioActual.SISTEMA : u;
        } catch (RuntimeException sinContexto) {
            return py.com.one.core.UsuarioActual.SISTEMA;
        }
    }

    // ── Consultas ──

    public long contar(String filtro) {
        var q = em.createQuery(
            "SELECT COUNT(o) FROM Operacion o, Persona p WHERE o.tenant = :t AND p.id = o.cliente"
            + " AND (:f = '' OR lower(p.nombre) LIKE :like)", Long.class);
        filtroGlobal(q, filtro);
        return q.getSingleResult();
    }

    /** Filas: [operacion, nombreCliente, nombreActivo]. */
    public List<Object[]> listar(int primero, int cantidad, String filtro) {
        var q = em.createQuery(
            "SELECT o, p.nombre, a.nombre FROM Operacion o, Persona p, Activo a"
            + " WHERE o.tenant = :t AND p.id = o.cliente AND a.id = o.activo"
            + " AND (:f = '' OR lower(p.nombre) LIKE :like)"
            + " ORDER BY o.id DESC", Object[].class);
        filtroGlobal(q, filtro);
        return q.setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    private void filtroGlobal(jakarta.persistence.TypedQuery<?> q, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        q.setParameter("f", f).setParameter("like", "%" + f + "%").setParameter("t", tenant.actual());
    }

    /** Resuelve una operacion por su id (no depende de las primeras N filas de la grilla). */
    /** Detalle por id SOLO si la operacion es del tenant (obs 254); si no, null (invisible). */
    public Operacion porId(Long id) {
        if (id == null) return null;
        Operacion o = em.find(Operacion.class, id);
        if (o == null) return null;
        if (!tenant.esSuperadmin() && !tenant.actual().equals(o.getTenant())) return null;
        return o;
    }

    public List<CronogramaCuota> cuotasDe(Long operacionId) {
        // Solo si la operacion es visible al tenant (obs 254); reutiliza la guarda de porId.
        if (porId(operacionId) == null) return java.util.List.of();
        return em.createQuery(
                "SELECT c FROM CronogramaCuota c WHERE c.operacion = :op ORDER BY c.numeroCuota",
                CronogramaCuota.class)
            .setParameter("op", operacionId).getResultList();
    }

    /** Mora de una cuota a hoy, calculada por la BD (f_mora_cuota). */
    public BigDecimal moraDe(Long cuotaId) {
        // La cuota debe pertenecer a una operacion del tenant (obs 254) antes de calcular su mora.
        Long opId = em.createQuery("SELECT c.operacion FROM CronogramaCuota c WHERE c.id = :c", Long.class)
            .setParameter("c", cuotaId).getResultList().stream().findFirst().orElse(null);
        if (opId == null || porId(opId) == null) return BigDecimal.ZERO;
        Object r = em.createNativeQuery("SELECT f_mora_cuota(:c, current_date)")
            .setParameter("c", cuotaId).getSingleResult();
        return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString());
    }

    /** Activos disponibles para operar (LIBRES) por autocomplete. */
    public List<Activo> activosLibres(String texto) {
        String f = texto == null ? "" : texto.trim().toLowerCase();
        // Aislamiento (obs 249): solo activos LIBRES del tenant del contexto.
        return em.createQuery(
                "SELECT a FROM Activo a WHERE a.estado = 'LIBRE' AND a.tenant = :t AND lower(a.nombre) LIKE :like ORDER BY a.nombre",
                Activo.class)
            .setParameter("t", tenant.actual())
            .setParameter("like", "%" + f + "%").setMaxResults(15).getResultList();
    }

    // ── Alta de operacion (REQ-0016): transaccion completa ──

    @Transactional
    public Operacion crear(Operacion op) {
        autorizacion.exigir("operaciones", "CREAR");
        // El tenant lo fija el contexto, NUNCA el payload (obs 249): evita crear operaciones
        // con tenant alterado y documentos/movimientos bajo una empresa que no corresponde.
        op.setTenant(tenant.actual());
        validar(op);
        Activo activo = em.find(Activo.class, op.getActivo());
        if (activo == null) throw new NegocioException("El activo no existe");
        if (tenant.actual() == null || !tenant.actual().equals(activo.getTenant())) {
            throw new NegocioException("El activo pertenece a otra empresa");
        }
        // La sucursal del contexto debe ser del mismo tenant (obs 249).
        Long sucOk = em.createQuery(
                "SELECT COUNT(s) FROM Sucursal s WHERE s.id = :s AND s.tenant = :t", Long.class)
            .setParameter("s", op.getSucursal()).setParameter("t", tenant.actual()).getSingleResult();
        if (sucOk == 0) throw new NegocioException("La sucursal pertenece a otra empresa");
        if (!"LIBRE".equals(activo.getEstado())) {
            throw new NegocioException("El activo '" + activo.getNombre() + "' no está LIBRE (está " + activo.getEstado() + ")");
        }
        try {
            // monto total: precio x plazo en alquiler credito; precio en venta/contado
            if ("ALQUILER".equals(op.getTipoOperacion()) && op.getPlazo() != null && op.getPlazo() > 0) {
                op.setMontoTotalOperacion(op.getPrecio().multiply(BigDecimal.valueOf(op.getPlazo())));
                op.setFechaFinContrato(op.getFechaInicioContrato().plusMonths(op.getPlazo()));
            } else {
                op.setMontoTotalOperacion(op.getPrecio());
            }
            em.persist(op);
            em.flush();

            // documento interno de cuenta corriente que respalda el cronograma
            Long doc = crearDocumentoInterno(op, op.getMontoTotalOperacion(),
                    ("ALQUILER".equals(op.getTipoOperacion()) ? "Alquiler " : "Venta ") + activo.getNombre()
                    + " - Operación " + op.getId());

            // cronograma en la BD (cuadre exacto garantizado por f_generar_cronograma)
            if ("CREDITO".equals(op.getCondicionOperacion()) && op.getPlazo() != null && op.getPlazo() > 0) {
                em.createNativeQuery("SELECT f_generar_cronograma(:op, :n, :total, :desde, :dia, :mon, :usr)")
                    .setParameter("op", op.getId())
                    .setParameter("n", op.getPlazo())
                    .setParameter("total", op.getMontoTotalOperacion())
                    .setParameter("desde", java.sql.Date.valueOf(op.getFechaInicioContrato().plusMonths(1)))
                    .setParameter("dia", op.getDiaPago())
                    .setParameter("mon", op.getMoneda())
                    .setParameter("usr", usuarioAuditoria())
                    .getSingleResult();
                em.createNativeQuery("UPDATE cronograma_cuota SET documento = :doc WHERE operacion = :op")
                    .setParameter("doc", doc).setParameter("op", op.getId()).executeUpdate();
            }

            // REQ-0018: movimientos automaticos en ingreso_egreso, clasificados y CANCELADOS
            boolean esAlquiler = "ALQUILER".equals(op.getTipoOperacion());

            // Deposito de garantia (solo alquiler): INGRESO, item DEPOSITO_GARANTIA (RN-OPE-013)
            if (esAlquiler && op.getGarantia() != null && op.getGarantia().signum() > 0) {
                crearMovimiento("INGRESO", "DEPOSITO_GARANTIA", op.getGarantia(), op, op.getCliente(),
                        "Depósito de garantía - Operación " + op.getId());
            }

            // Comision: EGRESO, item COMISION_ALQUILER/COMISION_VENTA (RN-OPE-002/012).
            // Base RN-OPE-002: ALQUILER = garantia * %, VENTA = precio * %.
            BigDecimal pctComision = esAlquiler ? activo.getComisionAlquiler() : activo.getComisionVenta();
            BigDecimal baseComision = esAlquiler ? op.getGarantia() : op.getPrecio();
            if (pctComision != null && pctComision.signum() > 0 && baseComision != null && baseComision.signum() > 0) {
                BigDecimal comision = baseComision.multiply(pctComision)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                crearMovimiento("EGRESO", esAlquiler ? "COMISION_ALQUILER" : "COMISION_VENTA", comision,
                        op, op.getVendedor(),
                        "Comisión " + pctComision + "% - Operación " + op.getId());
            }

            // el activo pasa a OCUPADA (alquiler) o VENDIDA (venta)
            activo.setEstado("ALQUILER".equals(op.getTipoOperacion()) ? "OCUPADA" : "VENDIDA");
            em.flush();
            // obs 271: alta de operacion (accion critica) en la auditoria funcional visible.
            auditoria.registrar("operacion", op.getId(), AuditoriaFuncionalService.CREAR, "operaciones",
                    op.getTipoOperacion() + "/" + op.getCondicionOperacion() + " monto " + op.getMontoTotalOperacion());
            return op;
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);
        }
    }

    /** Id del articulo/concepto cuya aplicacion funcional coincide (COMISION_*, DEPOSITO_GARANTIA...). */
    private Long articuloPorAplicacion(String aplicacion) {
        var l = em.createQuery(
                "SELECT a.id FROM Articulo a WHERE a.aplicacion = :apl AND a.estado = 'ACTIVO' ORDER BY a.id",
                Long.class)
            .setParameter("apl", aplicacion).setMaxResults(1).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    /**
     * Movimiento automatico de caja (REQ-0018) en ingreso_egreso, clasificado por articulo/aplicacion,
     * estado CANCELADO (contado), con trazabilidad a operacion/activo/persona. Auditable fija el usuario real.
     */
    private void crearMovimiento(String tipo, String aplicacion, BigDecimal monto,
                                 Operacion op, Long persona, String observacion) {
        Long articulo = articuloPorAplicacion(aplicacion);
        if (articulo == null) {
            throw new NegocioException("Falta el artículo con aplicación '" + aplicacion + "' en el catálogo");
        }
        var ie = new IngresoEgreso();
        ie.setFecha(op.getFechaOperacion());
        ie.setTipo(tipo);
        ie.setMonto(monto);
        ie.setSaldo(BigDecimal.ZERO);      // CANCELADO: sin saldo pendiente
        ie.setEstado("CANCELADO");
        ie.setArticulo(articulo);
        ie.setPersona(persona);
        ie.setActivo(op.getActivo());
        ie.setOperacion(op.getId());
        ie.setTenant(op.getTenant());
        ie.setObservacion(observacion);
        em.persist(ie);
    }

    /** Documento interno (DINT) ENTRADA: numerado por f_siguiente_numero, detalle unico. */
    private Long crearDocumentoInterno(Operacion op, BigDecimal monto, String concepto) {
        String usr = usuarioAuditoria();
        // REQ-0076: el documento interno (DINT/OP) respalda la cuenta corriente del cronograma; es de uso
        // INTERNO, no fiscal, por lo que no requiere un timbrado real. Se garantiza un rango ACTIVO usable
        // para DINT/OP del tenant, de forma idempotente, tenant-safe (RLS) y SIN violar la unicidad
        // (tenant, tipo, serie, numero_desde) -obs 281-:
        //  0) se DESACTIVAN los rangos ACTIVOS AGOTADOS (numero_actual > numero_hasta): si no, f_siguiente_numero
        //     -que elige el activo mas antiguo por numero_desde sin mirar capacidad- volveria a fallar "agotado" (obs 289);
        //  1) si hay un rango inactivo AUN UTILIZABLE, se reactiva (no se crea uno nuevo);
        //  2) si no hay ninguno activo utilizable, se inserta uno nuevo con numero_desde MAS ALLA del
        //     mayor numero_hasta existente (asi nunca colisiona con un rango previo -inactivo o agotado-).
        em.createNativeQuery(
            "UPDATE rango_comprobante SET estado='INACTIVO', usuario_modificacion='sistema', fecha_modificacion=now()"
          + " WHERE tenant=:emp AND tipo='DINT' AND serie='OP' AND estado='ACTIVO' AND numero_actual > numero_hasta")
            .setParameter("emp", op.getTenant()).executeUpdate();
        em.createNativeQuery(
            "UPDATE rango_comprobante SET estado='ACTIVO', usuario_modificacion='sistema', fecha_modificacion=now()"
          + " WHERE rango_comprobante = (SELECT rc.rango_comprobante FROM rango_comprobante rc"
          + "     WHERE rc.tenant=:emp AND rc.tipo='DINT' AND rc.serie='OP' AND rc.estado<>'ACTIVO'"
          + "       AND rc.numero_actual <= rc.numero_hasta ORDER BY rc.numero_desde LIMIT 1)"
          + " AND NOT EXISTS (SELECT 1 FROM rango_comprobante a WHERE a.tenant=:emp AND a.tipo='DINT'"
          + "     AND a.serie='OP' AND a.estado='ACTIVO' AND a.numero_actual <= a.numero_hasta)")
            .setParameter("emp", op.getTenant()).executeUpdate();
        em.createNativeQuery(
            "INSERT INTO rango_comprobante (tenant, tipo, serie, numero_desde, numero_actual, numero_hasta,"
          + " estado, usuario_creacion, fecha_creacion)"
          + " SELECT :emp, 'DINT', 'OP', b.n, b.n, b.n + 1000000000, 'ACTIVO', 'sistema', now()"
          + " FROM (SELECT COALESCE(MAX(rc.numero_hasta),0) + 1 AS n FROM rango_comprobante rc"
          + "         WHERE rc.tenant=:emp AND rc.tipo='DINT' AND rc.serie='OP') b"
          + " WHERE NOT EXISTS (SELECT 1 FROM rango_comprobante a WHERE a.tenant=:emp AND a.tipo='DINT'"
          + "     AND a.serie='OP' AND a.estado='ACTIVO' AND a.numero_actual <= a.numero_hasta)")
            .setParameter("emp", op.getTenant()).executeUpdate();
        Object num = em.createNativeQuery("SELECT f_siguiente_numero(:emp, 'DINT', 'OP')")
            .setParameter("emp", op.getTenant()).getSingleResult();
        em.createNativeQuery(
            "INSERT INTO documento (tenant, empresa, tipo, serie, numero, fecha, persona, sucursal,"
            + " moneda, direccion_dinero, observacion, usuario_creacion, fecha_creacion)"
            + " VALUES (:emp, :emp, 'DINT', 'OP', :num, :fec, :per, :suc, :mon, 'ENTRADA', :obs, :usr, now())")
            .setParameter("emp", op.getTenant()).setParameter("num", ((Number) num).longValue())
            .setParameter("fec", java.sql.Date.valueOf(op.getFechaOperacion()))
            .setParameter("per", op.getCliente()).setParameter("suc", op.getSucursal())
            .setParameter("mon", op.getMoneda()).setParameter("obs", concepto)
            .setParameter("usr", usr)
            .executeUpdate();
        Object doc = em.createNativeQuery(
            "SELECT documento FROM documento WHERE tenant = :emp AND tipo = 'DINT' AND serie = 'OP' AND numero = :num")
            .setParameter("emp", op.getTenant()).setParameter("num", ((Number) num).longValue())
            .getSingleResult();
        Long docId = ((Number) doc).longValue();
        em.createNativeQuery(
            "INSERT INTO documento_detalle (documento, numero_item, concepto, cantidad, precio_unitario,"
            + " monto, saldo, usuario_creacion, fecha_creacion)"
            + " VALUES (:doc, 1, :con, 1, :monto, :monto, :monto, :usr, now())")
            .setParameter("doc", docId).setParameter("con", concepto).setParameter("monto", monto)
            .setParameter("usr", usr)
            .executeUpdate();
        return docId;
    }

    private void validar(Operacion op) {
        if (op.getCliente() == null) throw new NegocioException("El cliente es obligatorio");
        if (op.getActivo() == null) throw new NegocioException("El activo es obligatorio");
        if (op.getPrecio() == null || op.getPrecio().signum() <= 0) {
            throw new NegocioException("El precio debe ser mayor a cero");
        }
        if ("CREDITO".equals(op.getCondicionOperacion())
                && (op.getPlazo() == null || op.getPlazo() < 1)) {
            throw new NegocioException("A crédito el plazo (cuotas) es obligatorio");
        }
        if (op.getTenant() == null || op.getSucursal() == null) {
            throw new NegocioException("Falta el contexto de empresa/sucursal (selector de la barra superior)");
        }
    }

    // ── REQ-0019: regeneracion de cuotas (solo sin cobros; la BD lo garantiza) ──

    @Transactional
    public void regenerarCuotas(Long operacionId, int cuotas, java.time.LocalDate primeraFecha) {
        // Proceso correctivo sensible: la doc (backlog + reglas de negocio) lo restringe a ADMINISTRADOR.
        autorizacion.exigirAdministrador();
        Operacion op = em.find(Operacion.class, operacionId);
        if (op == null) throw new NegocioException("La operación no existe");
        if (op.getTenant() == null || !op.getTenant().equals(tenant.actual())) {
            throw new NegocioException("La operación pertenece a otra empresa");
        }
        if (!"VIGENTE".equals(op.getEstado())) throw new NegocioException("La operación no está vigente");
        try {
            em.createNativeQuery("SELECT f_generar_cronograma(:op, :n, :total, :desde, :dia, :mon, :usr)")
                .setParameter("op", operacionId).setParameter("n", cuotas)
                .setParameter("total", op.getMontoTotalOperacion())
                .setParameter("desde", java.sql.Date.valueOf(primeraFecha))
                .setParameter("dia", op.getDiaPago()).setParameter("mon", op.getMoneda())
                .setParameter("usr", usuarioAuditoria()).getSingleResult();
            op.setPlazo(cuotas);
            // Al cambiar el plazo se recalcula la fecha fin de contrato coherente con el nuevo
            // cronograma (ultima cuota = primeraFecha + (cuotas-1) meses, en el dia de pago).
            java.time.LocalDate fin = primeraFecha.plusMonths(cuotas - 1L);
            if (op.getDiaPago() != null && op.getDiaPago() >= 1 && op.getDiaPago() <= 28) {
                fin = fin.withDayOfMonth(op.getDiaPago());
            }
            op.setFechaFinContrato(fin);
            // obs 271: regeneracion de cuotas (accion critica correctiva) en la auditoria funcional.
            auditoria.registrar("operacion", operacionId, AuditoriaFuncionalService.REGENERAR, "operaciones",
                    "regenero cronograma a " + cuotas + " cuotas desde " + primeraFecha);
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(e);   // "ya tiene cuotas con cobros" viene de la BD
        }
    }

    // ── REQ-0020: renovacion (extiende contrato y agrega cuotas nuevas) ──

    @Transactional
    public void renovar(Long operacionId, int mesesAdicionales, BigDecimal nuevoPrecio) {
        autorizacion.exigir("operaciones", "EDITAR");
        Operacion op = em.find(Operacion.class, operacionId);
        if (op == null) throw new NegocioException("La operación no existe");
        if (op.getTenant() == null || !op.getTenant().equals(tenant.actual())) {
            throw new NegocioException("La operación pertenece a otra empresa");
        }
        if (!"VIGENTE".equals(op.getEstado())) throw new NegocioException("Solo se renuevan operaciones vigentes");
        if (mesesAdicionales < 1) throw new NegocioException("Los meses adicionales deben ser al menos 1");
        // RN-REN-001/002: no se renueva un contrato con deuda viva (cuotas PENDIENTE);
        // primero se cobran (o regularizan) las cuotas y recien entonces se renueva.
        Long pendientes = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM cronograma_cuota WHERE operacion = :op AND estado = 'PENDIENTE'")
            .setParameter("op", operacionId).getSingleResult()).longValue();
        if (pendientes > 0) {
            throw new NegocioException("No se puede renovar: la operación tiene " + pendientes
                    + " cuota(s) pendiente(s) de pago. Regularice las cuotas antes de renovar.");
        }
        BigDecimal precio = nuevoPrecio == null || nuevoPrecio.signum() <= 0 ? op.getPrecio() : nuevoPrecio;
        Integer ultima = (Integer) em.createNativeQuery(
                "SELECT COALESCE(MAX(numero_cuota),0)::int FROM cronograma_cuota WHERE operacion = :op")
            .setParameter("op", operacionId).getSingleResult();
        java.time.LocalDate base = op.getFechaFinContrato() != null ? op.getFechaFinContrato() : java.time.LocalDate.now();
        for (int i = 1; i <= mesesAdicionales; i++) {
            java.time.LocalDate venc = base.plusMonths(i);
            if (op.getDiaPago() != null && op.getDiaPago() >= 1 && op.getDiaPago() <= 28) {
                venc = venc.withDayOfMonth(op.getDiaPago());
            }
            em.createNativeQuery(
                "INSERT INTO cronograma_cuota (operacion, numero_cuota, fecha_vencimiento, monto, saldo,"
                + " estado, moneda, usuario_creacion, fecha_creacion)"
                + " VALUES (:op, :n, :venc, :monto, :monto, 'PENDIENTE', :mon, :usr, now())")
                .setParameter("op", operacionId).setParameter("n", ultima + i)
                .setParameter("venc", java.sql.Date.valueOf(venc))
                .setParameter("monto", precio).setParameter("mon", op.getMoneda())
                .setParameter("usr", usuarioAuditoria())
                .executeUpdate();
        }
        op.setPrecio(precio);
        op.setFechaRenovacion(java.time.LocalDate.now());
        op.setFechaFinContrato(base.plusMonths(mesesAdicionales));
        op.setMontoTotalOperacion(op.getMontoTotalOperacion().add(precio.multiply(BigDecimal.valueOf(mesesAdicionales))));
        op.setPlazo((op.getPlazo() == null ? 0 : op.getPlazo()) + mesesAdicionales);
    }

    // ── REQ-0021: rescision / finalizacion ──

    @Transactional
    public void finalizar(Long operacionId, String motivoRescision) {
        autorizacion.exigir("operaciones", "EDITAR");
        // El motivo es obligatorio: la finalizacion/rescision debe quedar auditable siempre.
        if (motivoRescision == null || motivoRescision.isBlank()) {
            throw new NegocioException("El motivo de la finalización/rescisión es obligatorio");
        }
        Operacion op = em.find(Operacion.class, operacionId);
        if (op == null) throw new NegocioException("La operación no existe");
        if (op.getTenant() == null || !op.getTenant().equals(tenant.actual())) {
            throw new NegocioException("La operación pertenece a otra empresa");
        }
        if (!"VIGENTE".equals(op.getEstado())) throw new NegocioException("La operación ya está finalizada");
        op.setEstado("FINALIZADO");
        op.setFechaFinalizacion(java.time.LocalDate.now());
        // el activo vuelve a LIBRE salvo venta consumada
        Activo a = em.find(Activo.class, op.getActivo());
        if (a != null && !"VENDIDA".equals(a.getEstado())) {
            a.setEstado("LIBRE");
        }
        // SIEMPRE se inserta la fila de rescision (trazabilidad). Tipo segun el momento
        // (codigos del legado): antes de la fecha fin de contrato = RESCISION anticipada;
        // en/tras la fecha fin (o sin fecha) = FIN_CONTRATO.
        boolean anticipada = op.getFechaFinContrato() != null
                && java.time.LocalDate.now().isBefore(op.getFechaFinContrato());
        em.createNativeQuery(
            "INSERT INTO rescision (operacion, fecha, tipo, observacion, usuario_creacion, fecha_creacion)"
            + " VALUES (:op, current_date, :tipo, :mot, :usr, now())")
            .setParameter("op", operacionId)
            .setParameter("tipo", anticipada ? "RESCISION" : "FIN_CONTRATO")
            .setParameter("mot", motivoRescision)
            .setParameter("usr", usuarioAuditoria())
            .executeUpdate();
    }
}
