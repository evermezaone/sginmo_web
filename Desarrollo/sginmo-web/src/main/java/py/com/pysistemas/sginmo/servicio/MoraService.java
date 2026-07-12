package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.cobranza.GestionCobranza;
import py.com.pysistemas.sginmo.dominio.cobranza.PromesaPago;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * REQ-0057 - Mora y cobranza. @AislarTenant para RLS. La cartera vencida sale de cronograma_cuota
 * (misma fuente que cobros); la mora en dinero usa f_mora_cuota (no se duplica el calculo). Las
 * gestiones y promesas NO modifican la cuota; la promesa no es un pago ni cambia el estado de la cuota.
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class MoraService {

    private static final int LIMITE_CARTERA = 1000;
    private static final Set<String> ESTADOS_PROMESA = Set.of("PENDIENTE", "CUMPLIDA", "INCUMPLIDA");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private TenantContext tenant;

    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;

    /** Cartera vencida (cap {@value #LIMITE_CARTERA} filas para no bloquear la operacion). */
    public List<FilaCartera> carteraVencida(Integer diasMin, BigDecimal montoMin, Long moneda, Long cliente, Long operacion) {
        List<FilaCartera> out = new ArrayList<>();
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) return out;
        StringBuilder sql = new StringBuilder(
            "SELECT cc.cronograma_cuota, o.operacion, o.cliente, vp.nombre, cc.numero_cuota,"
          + " cc.fecha_vencimiento, cc.saldo, cc.moneda, (current_date - cc.fecha_vencimiento) AS dias,"
          + " f_mora_cuota(cc.cronograma_cuota, current_date) AS mora"
          + " FROM cronograma_cuota cc JOIN operacion o ON o.operacion = cc.operacion"
          + " LEFT JOIN v_persona vp ON vp.persona = o.cliente"
          + " WHERE cc.estado='PENDIENTE' AND cc.fecha_vencimiento < current_date");
        if (diasMin != null) sql.append(" AND (current_date - cc.fecha_vencimiento) >= :dias");
        if (montoMin != null) sql.append(" AND cc.saldo >= :monto");
        if (moneda != null) sql.append(" AND cc.moneda = :mon");
        if (cliente != null) sql.append(" AND o.cliente = :cli");
        if (operacion != null) sql.append(" AND o.operacion = :op");
        sql.append(" ORDER BY dias DESC");
        Query q = em.createNativeQuery(sql.toString());
        if (diasMin != null) q.setParameter("dias", diasMin);
        if (montoMin != null) q.setParameter("monto", montoMin);
        if (moneda != null) q.setParameter("mon", moneda);
        if (cliente != null) q.setParameter("cli", cliente);
        if (operacion != null) q.setParameter("op", operacion);
        q.setMaxResults(LIMITE_CARTERA);
        @SuppressWarnings("unchecked")
        List<Object[]> filas = q.getResultList();
        for (Object[] f : filas) {
            FilaCartera c = new FilaCartera();
            c.cronogramaCuota = ((Number) f[0]).longValue();
            c.operacion = ((Number) f[1]).longValue();
            c.cliente = f[2] == null ? null : ((Number) f[2]).longValue();
            c.clienteNombre = (String) f[3];
            c.numeroCuota = ((Number) f[4]).intValue();
            c.fechaVencimiento = ((java.sql.Date) f[5]).toLocalDate();
            c.saldo = f[6] == null ? BigDecimal.ZERO : new BigDecimal(f[6].toString());
            c.moneda = f[7] == null ? null : ((Number) f[7]).longValue();
            c.diasMora = ((Number) f[8]).intValue();
            c.mora = f[9] == null ? BigDecimal.ZERO : new BigDecimal(f[9].toString());
            out.add(c);
        }
        return out;
    }

    @Transactional
    public GestionCobranza registrarGestion(GestionCobranza g) {
        autorizacion.exigir("cobranza", "EDITAR");
        Long emp = requerirEmpresa();
        if (g.getResultado() == null || g.getResultado().isBlank()) throw new NegocioException("El resultado es obligatorio");
        g.setTenant(emp);
        if (g.getFecha() == null) g.setFecha(LocalDate.now());
        if (g.getResponsable() == null || g.getResponsable().isBlank()) g.setResponsable(sesion.codigoUsuario());
        try { em.persist(g); em.flush(); return g; }
        catch (RuntimeException ex) { throw ErroresBd.traducir(ex); }
    }

    @Transactional
    public PromesaPago registrarPromesa(PromesaPago p) {
        autorizacion.exigir("cobranza", "EDITAR");
        Long emp = requerirEmpresa();
        if (p.getFechaPromesa() == null) throw new NegocioException("La fecha de promesa es obligatoria");
        if (p.getMonto() == null || p.getMonto().signum() <= 0) throw new NegocioException("El monto debe ser mayor a cero");
        p.setTenant(emp);
        if (p.getEstado() == null || p.getEstado().isBlank()) p.setEstado("PENDIENTE");
        try { em.persist(p); em.flush(); return p; }
        catch (RuntimeException ex) { throw ErroresBd.traducir(ex); }
    }

    /** Cierra/actualiza el estado de una promesa (CUMPLIDA cuando se cobro; INCUMPLIDA si venció). No toca la cuota. */
    @Transactional
    public void cambiarEstadoPromesa(Long id, String estado) {
        autorizacion.exigir("cobranza", "EDITAR");
        if (!ESTADOS_PROMESA.contains(estado)) throw new NegocioException("Estado invalido");
        PromesaPago p = em.find(PromesaPago.class, id);
        if (p == null) throw new NegocioException("La promesa no existe");
        p.setEstado(estado);
        em.merge(p);
    }

    public List<GestionCobranza> gestionesDe(Long operacion) {
        return em.createQuery("SELECT g FROM GestionCobranza g WHERE g.operacion = :op ORDER BY g.fecha DESC", GestionCobranza.class)
                .setParameter("op", operacion).setMaxResults(200).getResultList();
    }

    public List<PromesaPago> promesasDe(Long operacion) {
        return em.createQuery("SELECT p FROM PromesaPago p WHERE p.operacion = :op ORDER BY p.fechaPromesa DESC", PromesaPago.class)
                .setParameter("op", operacion).setMaxResults(200).getResultList();
    }

    private Long requerirEmpresa() {
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) throw new NegocioException("Seleccione una empresa");
        return emp;
    }

    /** Fila de la cartera vencida. */
    public static class FilaCartera {
        public Long cronogramaCuota, operacion, cliente, moneda;
        public String clienteNombre;
        public int numeroCuota, diasMora;
        public LocalDate fechaVencimiento;
        public BigDecimal saldo = BigDecimal.ZERO, mora = BigDecimal.ZERO;
        public Long getCronogramaCuota() { return cronogramaCuota; }
        public Long getOperacion() { return operacion; }
        public Long getCliente() { return cliente; }
        public Long getMoneda() { return moneda; }
        public String getClienteNombre() { return clienteNombre; }
        public int getNumeroCuota() { return numeroCuota; }
        public int getDiasMora() { return diasMora; }
        public LocalDate getFechaVencimiento() { return fechaVencimiento; }
        public BigDecimal getSaldo() { return saldo; }
        public BigDecimal getMora() { return mora; }
    }
}
