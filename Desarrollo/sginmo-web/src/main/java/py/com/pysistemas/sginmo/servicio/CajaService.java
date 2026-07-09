package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.operacion.Planilla;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caja diaria y cobros (REQ-0022/0023): planilla por sucursal + cobros que INVOCAN
 * f_cobrar_documento / f_anular_cobro (motor V17, verificado numericamente).
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class CajaService {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    // ── Planilla (caja) ──

    public Planilla planillaAbierta(Long empresa, Long sucursal) {
        return em.createQuery(
                "SELECT p FROM Planilla p WHERE p.tenant = :emp AND p.sucursal = :suc AND p.estado = 'ABIERTA'"
                + " ORDER BY p.id DESC", Planilla.class)
            .setParameter("emp", empresa).setParameter("suc", sucursal)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    @Transactional
    public Planilla abrirPlanilla(Long empresa, Long sucursal, BigDecimal montoApertura, String usuario) {
        autorizacion.exigir("caja", "CREAR");
        if (planillaAbierta(empresa, sucursal) != null) {
            throw new NegocioException("Ya hay una planilla ABIERTA para esta sucursal; ciérrela primero");
        }
        var p = new Planilla();
        p.setTenant(empresa);
        p.setSucursal(sucursal);
        p.setUsuarioApertura(usuario);
        p.setFechaApertura(LocalDate.now());
        p.setHoraApertura(LocalDateTime.now());
        p.setMontoApertura(montoApertura == null ? BigDecimal.ZERO : montoApertura);
        em.persist(p);
        em.flush();
        return p;
    }

    @Transactional
    public void cerrarPlanilla(Long planillaId, String usuario) {
        autorizacion.exigir("caja", "EDITAR");
        Planilla p = em.find(Planilla.class, planillaId);
        if (p == null || !"ABIERTA".equals(p.getEstado())) {
            throw new NegocioException("La planilla no existe o ya está cerrada");
        }
        p.setEstado("CERRADA");
        p.setUsuarioCierre(usuario);
        p.setFechaCierre(LocalDate.now());
        p.setHoraCierre(LocalDateTime.now());
    }

    // ── Documentos cobrables y cuotas ──

    /** Filas: [documento(id), tipo, serie, numero, fecha, total, saldo, observacion]. */
    @SuppressWarnings("unchecked")
    public List<Object[]> documentosPendientesDe(Long personaId) {
        return em.createNativeQuery(
            "SELECT d.documento, d.tipo, d.serie, d.numero, d.fecha, d.total, d.saldo, d.observacion"
            + " FROM documento d WHERE d.persona = :per AND d.estado = 'PENDIENTE'"
            + " AND d.direccion_dinero = 'ENTRADA' AND d.saldo > 0 ORDER BY d.fecha, d.documento")
            .setParameter("per", personaId).getResultList();
    }

    /** Filas: [cuota(id), numero, vencimiento, monto, saldo, mora_hoy] del documento. */
    @SuppressWarnings("unchecked")
    public List<Object[]> cuotasPendientesDeDocumento(Long documentoId) {
        return em.createNativeQuery(
            "SELECT c.cronograma_cuota, c.numero_cuota, c.fecha_vencimiento, c.monto, c.saldo,"
            + " f_mora_cuota(c.cronograma_cuota, current_date) AS mora"
            + " FROM cronograma_cuota c WHERE c.documento = :doc AND c.estado = 'PENDIENTE'"
            + " ORDER BY c.fecha_vencimiento")
            .setParameter("doc", documentoId).getResultList();
    }

    // ── Cobro / anulacion: el motor de la BD hace TODO el cuadre ──

    @Transactional
    public long cobrar(Long documentoId, Long planillaId, Long formaPagoId, Long personaId,
                       BigDecimal monto, Long monedaId, String usuario) {
        return cobrar(documentoId, planillaId, formaPagoId, personaId, monto, monedaId, usuario,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    /**
     * Cobro con los datos del medio de pago (obs 225/226): la forma de pago parametriza
     * los 13 exigibles (flags requiere_*) y el SP los valida y persiste dato_cobro
     * junto al cobro (incluye cobrador, deposito, motivo de rechazo y nota de credito).
     */
    @Transactional
    public long cobrar(Long documentoId, Long planillaId, Long formaPagoId, Long personaId,
                       BigDecimal monto, Long monedaId, String usuario,
                       String emisor, String procesador, String numero, String serie,
                       String cuenta, java.time.LocalDate vencimiento, String referencia,
                       Long cobrador, java.time.LocalDate fechaDeposito, String numeroDeposito,
                       String estadoDeposito, String motivoRechazo, Long notaCredito) {
        autorizacion.exigir("caja", "CREAR");
        if (planillaId == null) throw new NegocioException("No hay planilla de caja abierta");
        try {
            Object r = em.createNativeQuery(
                "SELECT f_cobrar_documento(:doc, :pla, :fp, :per, :monto, :mon, current_date, :usr,"
                + " :emisor, :proc, :num, :serie, :cuenta, :venc, :ref,"
                + " :cobrador, :fdep, :ndep, :edep, :mrech, :ntcr)")
                .setParameter("doc", documentoId).setParameter("pla", planillaId)
                .setParameter("fp", formaPagoId).setParameter("per", personaId)
                .setParameter("monto", monto).setParameter("mon", monedaId)
                .setParameter("usr", usuario)
                .setParameter("emisor", emisor).setParameter("proc", procesador)
                .setParameter("num", numero).setParameter("serie", serie)
                .setParameter("cuenta", cuenta)
                .setParameter("venc", vencimiento == null ? null : java.sql.Date.valueOf(vencimiento))
                .setParameter("ref", referencia)
                .setParameter("cobrador", cobrador)
                .setParameter("fdep", fechaDeposito == null ? null : java.sql.Date.valueOf(fechaDeposito))
                .setParameter("ndep", numeroDeposito).setParameter("edep", estadoDeposito)
                .setParameter("mrech", motivoRechazo).setParameter("ntcr", notaCredito)
                .getSingleResult();
            return ((Number) r).longValue();
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(traducirSp(e));
        }
    }

    /** Notas de credito (NTCR) no anuladas del cliente, para asociar al cobro (obs 226). */
    @SuppressWarnings("unchecked")
    public List<Object[]> notasCreditoDe(Long personaId) {
        if (personaId == null) return java.util.List.of();
        List<Object[]> filas = em.createNativeQuery(
                "SELECT documento, serie || '-' || numero AS etiqueta, saldo FROM documento"
                + " WHERE persona = :p AND tipo = 'NTCR' AND estado <> 'ANULADO'"
                + " ORDER BY documento DESC")
            .setParameter("p", personaId).getResultList();
        return filas.stream()
            .map(f -> new Object[]{((Number) f[0]).longValue(), f[1], f[2]})
            .toList();
    }

    @Transactional
    public void anularCobro(Long cobroId, String usuario, String motivoCodigo) {
        autorizacion.exigir("caja", "INACTIVAR");
        if (motivoCodigo == null || motivoCodigo.isBlank()) {
            throw new NegocioException("Elija el motivo de la anulación");
        }
        try {
            em.createNativeQuery("SELECT f_anular_cobro(:cob, :usr, :mot)")
                .setParameter("cob", cobroId).setParameter("usr", usuario)
                .setParameter("mot", motivoCodigo)
                .getSingleResult();
        } catch (jakarta.persistence.PersistenceException e) {
            throw ErroresBd.traducir(traducirSp(e));
        }
    }

    /** Los RAISE EXCEPTION del motor traen mensaje de negocio: se muestran tal cual. */
    private jakarta.persistence.PersistenceException traducirSp(jakarta.persistence.PersistenceException e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof java.sql.SQLException) {
                String msg = causa.getMessage();
                if (msg != null) {
                    if (msg.startsWith("ERROR: ")) {
                        msg = msg.substring(7);
                    }
                    int corte = msg.indexOf('\n');
                    if (corte > 0) {
                        msg = msg.substring(0, corte);
                    }
                    throw new NegocioException(msg);
                }
            }
            causa = causa.getCause();
        }
        return e;
    }

    /** Filas: [cobro(id), fecha, persona_nombre, monto, forma_pago_desc, estado]. */
    @SuppressWarnings("unchecked")
    public List<Object[]> cobrosDePlanilla(Long planillaId) {
        return em.createNativeQuery(
            "SELECT c.cobro, c.fecha, COALESCE(p.nombre,'-'), c.monto, COALESCE(fp.descripcion,'-'), c.estado"
            + " FROM cobro c LEFT JOIN persona p ON p.persona = c.persona"
            + " LEFT JOIN forma_pago fp ON fp.forma_pago = c.forma_pago"
            + " WHERE c.planilla = :pla ORDER BY c.cobro DESC")
            .setParameter("pla", planillaId).getResultList();
    }
}
