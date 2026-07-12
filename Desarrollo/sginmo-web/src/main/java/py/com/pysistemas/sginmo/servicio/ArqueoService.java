package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.operacion.Planilla;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * REQ-0059 - Arqueo y cierre controlado de caja. Refuerza la planilla existente (no la reemplaza):
 * calcula efectivo esperado y totales por forma de pago, registra efectivo contado y diferencia,
 * cierra con confirmacion y permite reapertura excepcional con permiso + auditoria. @AislarTenant.
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class ArqueoService {

    private static final DecimalFormat GS;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("es", "PY"));
        s.setGroupingSeparator('.');
        GS = new DecimalFormat("#,##0", s);
    }

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private PdfService pdf;
    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;
    @jakarta.inject.Inject
    private TenantContext tenant;

    /** Planillas recientes del tenant (cap 50) para arqueo/reimpresion/reapertura. */
    public List<Planilla> planillasRecientes() {
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) return new ArrayList<>();
        return em.createQuery("SELECT p FROM Planilla p WHERE p.tenant = :emp ORDER BY p.id DESC", Planilla.class)
                .setParameter("emp", emp).setMaxResults(50).getResultList();
    }

    /** Resumen del arqueo de una planilla: totales por forma de pago + efectivo esperado. */
    public ResumenArqueo resumen(Long planillaId) {
        ResumenArqueo r = new ResumenArqueo();
        Planilla p = em.find(Planilla.class, planillaId);
        if (p == null) throw new NegocioException("La planilla no existe");
        r.planilla = p;
        r.montoApertura = nz(p.getMontoApertura());
        @SuppressWarnings("unchecked")
        List<Object[]> tot = em.createNativeQuery(
            "SELECT COALESCE(fp.descripcion,'(sin forma)'), COALESCE(SUM(c.monto),0),"
          + " bool_or(LOWER(COALESCE(fp.descripcion,'')) LIKE '%efectivo%')"
          + " FROM cobro c LEFT JOIN forma_pago fp ON fp.forma_pago=c.forma_pago"
          + " WHERE c.planilla=:pl AND c.estado='ACTIVO' GROUP BY fp.descripcion ORDER BY 1")
            .setParameter("pl", planillaId).getResultList();
        BigDecimal cobrosEfectivo = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;
        for (Object[] t : tot) {
            LineaForma lf = new LineaForma();
            lf.forma = (String) t[0];
            lf.monto = nz((Number) t[1]);
            boolean esEfectivo = Boolean.TRUE.equals(t[2]);
            r.lineas.add(lf);
            totalGeneral = totalGeneral.add(lf.monto);
            if (esEfectivo) cobrosEfectivo = cobrosEfectivo.add(lf.monto);
        }
        r.totalCobrado = totalGeneral;
        r.efectivoEsperado = r.montoApertura.add(cobrosEfectivo);
        return r;
    }

    /** Cierre controlado con arqueo: sella efectivo esperado/contado/diferencia y cierra la planilla. */
    @Transactional
    public void cerrarConArqueo(Long planillaId, BigDecimal efectivoContado, String observacion) {
        autorizacion.exigir("arqueo", "EDITAR");   // obs 263: permisos unificados a la pantalla arqueo/*
        Planilla p = em.find(Planilla.class, planillaId);
        if (p == null || !"ABIERTA".equals(p.getEstado())) {
            throw new NegocioException("La planilla no existe o no esta abierta");
        }
        if (efectivoContado == null || efectivoContado.signum() < 0) {
            throw new NegocioException("Ingrese el efectivo contado");
        }
        ResumenArqueo r = resumen(planillaId);
        p.setEfectivoEsperado(r.efectivoEsperado);
        p.setEfectivoContado(efectivoContado);
        p.setDiferencia(efectivoContado.subtract(r.efectivoEsperado));
        p.setObservacionCierre(observacion);
        p.setEstado("CERRADA");
        p.setUsuarioCierre(sesion.codigoUsuario());
        p.setFechaCierre(java.time.LocalDate.now());
        p.setHoraCierre(LocalDateTime.now());
        em.merge(p);
    }

    /** Reapertura excepcional: solo con permiso REACTIVAR + motivo; queda auditada. */
    @Transactional
    public void reabrir(Long planillaId, String motivo) {
        autorizacion.exigir("arqueo", "REACTIVAR");   // obs 263: reapertura sigue siendo privilegio aparte (no en plantilla CAJA base)
        if (motivo == null || motivo.isBlank()) throw new NegocioException("El motivo de reapertura es obligatorio");
        Planilla p = em.find(Planilla.class, planillaId);
        if (p == null || !"CERRADA".equals(p.getEstado())) {
            throw new NegocioException("La planilla no existe o no esta cerrada");
        }
        p.setEstado("ABIERTA");
        p.setReabierta(Boolean.TRUE);
        p.setUsuarioReapertura(sesion.codigoUsuario());
        p.setFechaReapertura(LocalDateTime.now());
        p.setMotivoReapertura(motivo);
        em.merge(p);
    }

    /** PDF de arqueo de cierre (OpenPDF). Requiere permiso EXPORTAR. */
    public byte[] arqueoPdf(Long planillaId) {
        autorizacion.exigir("arqueo", "EXPORTAR");   // obs 263: unificado a arqueo/*
        ResumenArqueo r = resumen(planillaId);
        Planilla p = r.planilla;
        var rep = pdf.iniciar(empresaNombre(), "ARQUEO DE CAJA", sesion.codigoUsuario(),
                "Planilla Nro " + p.getId() + " · " + (p.getFechaApertura() == null ? "" : p.getFechaApertura())
                + " · " + p.getEstado());
        pdf.parrafo(rep, "Apertura: " + GS.format(r.montoApertura)
                + "     Total cobrado: " + GS.format(r.totalCobrado));
        pdf.espacio(rep);
        List<String[]> filas = new ArrayList<>();
        for (LineaForma lf : r.lineas) filas.add(new String[]{ lf.forma, GS.format(lf.monto) });
        pdf.tabla(rep, new String[]{"Forma de pago", "Total"}, filas, new float[]{3, 2});
        pdf.espacio(rep);
        pdf.parrafo(rep, "Efectivo esperado: " + GS.format(r.efectivoEsperado));
        if (p.getEfectivoContado() != null) {
            pdf.parrafo(rep, "Efectivo contado: " + GS.format(p.getEfectivoContado()));
            pdf.titulo(rep, "DIFERENCIA: " + GS.format(nz(p.getDiferencia())));
        }
        if (p.getObservacionCierre() != null && !p.getObservacionCierre().isBlank()) {
            pdf.parrafo(rep, "Observacion: " + p.getObservacionCierre());
        }
        if (Boolean.TRUE.equals(p.getReabierta())) {
            pdf.parrafo(rep, "Reabierta por " + p.getUsuarioReapertura() + ": " + p.getMotivoReapertura());
        }
        return pdf.cerrar(rep);
    }

    private String empresaNombre() {
        try {
            Long emp = tenant.actual();
            Object n = em.createNativeQuery("SELECT nombre FROM v_persona WHERE persona=:t")
                    .setParameter("t", emp).getSingleResult();
            return n == null ? "SGInmo" : n.toString();
        } catch (RuntimeException e) { return "SGInmo"; }
    }

    private static BigDecimal nz(Number n) { return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString()); }
    private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }

    public static class ResumenArqueo {
        public Planilla planilla;
        public BigDecimal montoApertura = BigDecimal.ZERO, totalCobrado = BigDecimal.ZERO, efectivoEsperado = BigDecimal.ZERO;
        public List<LineaForma> lineas = new ArrayList<>();
        public Planilla getPlanilla() { return planilla; }
        public BigDecimal getMontoApertura() { return montoApertura; }
        public BigDecimal getTotalCobrado() { return totalCobrado; }
        public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
        public List<LineaForma> getLineas() { return lineas; }
    }
    public static class LineaForma {
        public String forma; public BigDecimal monto = BigDecimal.ZERO;
        public String getForma() { return forma; }
        public BigDecimal getMonto() { return monto; }
    }
}
