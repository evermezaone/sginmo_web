package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.security.SecureRandom;
import java.util.List;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;

/**
 * REQ-0093 (Fase 1) - Genera el payload EMVCo y la imagen PNG del QR de pago del portal.
 *
 * El QR lleva el monto a pagar + la cuenta destino del comercio (parametrizada por empresa) para que el
 * socio complete la transferencia desde su app bancaria. La conciliacion/aplicacion automatica es de la
 * Fase 2 (REQ-0094); aca el socio, tras pagar, informa la transferencia (REQ-0092).
 *
 * Datos parametrizables (Parametros, por empresa o global -1): PORTAL_QR_HABILITADO, PORTAL_QR_GUI,
 * PORTAL_QR_CUENTA, PORTAL_QR_MERCHANT, PORTAL_QR_CIUDAD, PORTAL_QR_MCC, PORTAL_QR_MONEDA, PORTAL_QR_PAIS.
 * El contenido del "merchant account template" (tag 26 = GUI + cuenta) depende del banco/esquema SIPAP:
 * se toma tal cual de los parametros, sin asumir un formato propietario.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional
public class QrPagoService {

    @Inject
    private ParametroConfig parametros;

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    @Inject
    private CajaService caja;                       // REQ-0094: aplicar el cobro automatico

    @Inject
    private py.com.one.security.web.SesionUsuario sesion;   // permiso de caja del operador

    private static final SecureRandom RND = new SecureRandom();

    /**
     * REQ-0094: referencia UNICA de un intento de pago QR dinamico, vigente para (persona, importe).
     * Reusa un intento PENDIENTE no vencido del mismo importe (para no crear uno por cada carga de pagina);
     * si no hay, crea uno nuevo. Devuelve null si no se pudo (el llamador cae a una referencia estatica).
     */
    public String referenciaIntento(Long persona, java.math.BigDecimal importe, Long moneda) {
        if (persona == null || importe == null || importe.signum() <= 0) return null;
        Long t = tenant.actual();
        List<?> ex = em.createNativeQuery(
            "SELECT referencia FROM portal_pago_qr WHERE tenant=:t AND persona=:p AND importe=:imp"
          + " AND estado='PENDIENTE' AND (expira_en IS NULL OR expira_en > now())"
          + " ORDER BY creado_en DESC LIMIT 1")
            .setParameter("t", t).setParameter("p", persona).setParameter("imp", importe).getResultList();
        if (!ex.isEmpty() && ex.get(0) != null) return ex.get(0).toString();
        int min = Math.max(5, parametros.entero("PORTAL_QR_INTENTO_EXPIRA_MIN", 1440));
        String ref = nuevaReferencia();
        try {
            em.createNativeQuery(
                "INSERT INTO portal_pago_qr (tenant, persona, referencia, importe, moneda, expira_en, usuario_creacion, fecha_creacion)"
              + " VALUES (:t,:p,:r,:imp,:mon, now() + (:min || ' minutes')::interval, 'portal', now())")
                .setParameter("t", t).setParameter("p", persona).setParameter("r", ref).setParameter("imp", importe)
                .setParameter("mon", moneda).setParameter("min", String.valueOf(min))
                .executeUpdate();
        } catch (RuntimeException e) {
            return null;   // choque de referencia (improbable): el QR usara la referencia estatica
        }
        return ref;
    }

    /**
     * REQ-0094: intenta conciliar un movimiento bancario importado (REQ-0085) con un intento QR PENDIENTE,
     * cruzando por referencia (la del QR aparece dentro de la del movimiento) + importe exacto. Reclamo
     * atomico (un solo UPDATE...RETURNING). Devuelve true si concilio alguno. Deja el pago "listo para
     * aplicar" por el operador (la aplicacion automatica del cobro es la extension pendiente de Fase 2).
     */
    public boolean intentarConciliar(Long movimientoId, String referenciaMov, java.math.BigDecimal importe) {
        if (movimientoId == null || referenciaMov == null || referenciaMov.isBlank() || importe == null) return false;
        Long t = tenant.actual();
        // obs 317: no conciliar intentos VENCIDOS (respetar expira_en); un movimiento tardio no revive un QR expirado.
        List<?> upd = em.createNativeQuery(
            "UPDATE portal_pago_qr SET estado='CONCILIADO', movimiento=:mov"
          + " WHERE portal_pago_qr = (SELECT portal_pago_qr FROM portal_pago_qr"
          + "   WHERE tenant=:t AND estado='PENDIENTE' AND importe=:imp"
          + "     AND (expira_en IS NULL OR expira_en > now())"
          + "     AND position(referencia in upper(:ref)) > 0"
          + "   ORDER BY creado_en LIMIT 1)"
          + " RETURNING portal_pago_qr")
            .setParameter("mov", movimientoId).setParameter("t", t).setParameter("imp", importe)
            .setParameter("ref", referenciaMov.toUpperCase()).getResultList();
        if (upd.isEmpty()) return false;
        // obs 318 (anti doble aplicacion): el movimiento queda tomado por el QR -> CONCILIADO, para que
        // PortalTransferenciaService.candidatos() (que filtra PENDIENTE) no lo ofrezca en una conciliacion
        // manual de transferencia. Asi el mismo ingreso bancario no se aplica dos veces.
        em.createNativeQuery(
            "UPDATE movimiento_bancario_importado SET estado_conciliacion = 'CONCILIADO'"
          + " WHERE movimiento_bancario_importado = :mov AND estado_conciliacion = 'PENDIENTE'")
            .setParameter("mov", movimientoId).executeUpdate();
        // REQ-0094 (obs 316): intentar aplicar el cobro AUTOMATICAMENTE (gated). Best-effort: si no se
        // dan las condiciones (sin permiso/caja/documento/moneda), queda CONCILIADO para el operador.
        aplicarAutomatico(((Number) upd.get(0)).longValue(), referenciaMov);
        return true;
    }

    /**
     * REQ-0094 (obs 316): aplica el cobro del intento QR conciliado con el motor de caja y lo marca
     * APLICADO. Regla por defecto: imputa a la cuota/documento pendiente MAS ANTIGUO del socio, en la
     * caja abierta de la empresa, forma de pago TRANSFERENCIA. Todo pre-validado para NO lanzar (no
     * envenena la tx del import): si falta permiso de caja, caja abierta, documento, moneda, o el importe
     * excede el saldo, se omite y la intencion queda CONCILIADO para que el operador la aplique a mano.
     */
    private void aplicarAutomatico(Long intentoId, String referenciaMov) {
        if (sesion == null || !sesion.puede("caja", "CREAR")) return;   // sin permiso de caja: lo hace el operador
        Long t = tenant.actual();
        List<?> qs = em.createNativeQuery(
            "SELECT persona, importe, moneda FROM portal_pago_qr WHERE portal_pago_qr = :id AND estado = 'CONCILIADO'")
            .setParameter("id", intentoId).getResultList();
        if (qs.isEmpty()) return;
        Object[] q = (Object[]) qs.get(0);
        Long persona = ((Number) q[0]).longValue();
        java.math.BigDecimal importe = (java.math.BigDecimal) q[1];
        Long moneda = q[2] == null ? null : ((Number) q[2]).longValue();
        if (persona == null || importe == null || importe.signum() <= 0) return;

        List<?> pl = em.createNativeQuery(
            "SELECT planilla FROM planilla WHERE tenant = :t AND estado = 'ABIERTA' ORDER BY planilla DESC LIMIT 1")
            .setParameter("t", t).getResultList();
        if (pl.isEmpty()) return;   // sin caja abierta -> queda para el operador
        Long planillaId = ((Number) pl.get(0)).longValue();

        List<Object[]> docs = em.createNativeQuery(
            "SELECT documento, moneda, saldo FROM documento WHERE persona = :p AND estado = 'PENDIENTE'"
          + " AND direccion_dinero = 'ENTRADA' AND saldo > 0 ORDER BY fecha, documento LIMIT 1")
            .setParameter("p", persona).getResultList();
        if (docs.isEmpty()) return;   // sin documento a imputar -> queda para el operador
        Object[] d = docs.get(0);
        Long documentoId = ((Number) d[0]).longValue();
        Long monedaFinal = moneda != null ? moneda : (d[1] == null ? null : ((Number) d[1]).longValue());
        java.math.BigDecimal saldo = (java.math.BigDecimal) d[2];
        if (monedaFinal == null || saldo == null || importe.compareTo(saldo) > 0) return;  // sin moneda o sobrepago -> operador

        List<?> fps = em.createNativeQuery(
            "SELECT forma_pago FROM forma_pago WHERE codigo = 'TRF' AND tenant IN (:t, -1) ORDER BY tenant DESC LIMIT 1")
            .setParameter("t", t).getResultList();
        if (fps.isEmpty()) return;
        Long formaTrf = ((Number) fps.get(0)).longValue();

        long cobroId = caja.cobrar(documentoId, planillaId, formaTrf, persona, importe, monedaFinal,
                "portal-qr", null, null, null, null, null, null, referenciaMov,
                null, null, null, null, null, null);
        em.createNativeQuery("UPDATE portal_pago_qr SET estado = 'APLICADO', cobro = :c"
          + " WHERE portal_pago_qr = :id AND estado = 'CONCILIADO'")
            .setParameter("c", cobroId).setParameter("id", intentoId).executeUpdate();
    }

    private static String nuevaReferencia() {
        String abc = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder("QR");
        for (int i = 0; i < 12; i++) sb.append(abc.charAt(RND.nextInt(abc.length())));
        return sb.toString();
    }

    /**
     * El pago por QR esta activo solo si se habilito y estan cargados los datos MINIMOS del esquema:
     * la cuenta destino (tag 26 subtag 01) Y el GUI/identificador del banco/esquema (tag 26 subtag 00).
     * obs 315: sin GUI el payload EMVCo/SIPAP queda bancariamente incompleto, por eso se bloquea.
     */
    public boolean habilitado() {
        return parametros.booleano("PORTAL_QR_HABILITADO", false)
            && !parametros.texto("PORTAL_QR_CUENTA", "").isBlank()
            && !parametros.texto("PORTAL_QR_GUI", "").isBlank();
    }

    /** Construye el payload EMVCo QR (con CRC16) para el monto y la referencia dados. */
    public String payload(BigDecimal monto, String referencia) {
        String gui    = parametros.texto("PORTAL_QR_GUI", "");
        String cuenta = parametros.texto("PORTAL_QR_CUENTA", "");
        String mcc    = defecto(parametros.texto("PORTAL_QR_MCC", "0000"), "0000");
        String moneda = defecto(parametros.texto("PORTAL_QR_MONEDA", "600"), "600").trim();
        String pais   = defecto(parametros.texto("PORTAL_QR_PAIS", "PY"), "PY").trim().toUpperCase();
        String nombre = recorta(defecto(parametros.texto("PORTAL_QR_MERCHANT", ""), "COMERCIO"), 25);
        String ciudad = recorta(defecto(parametros.texto("PORTAL_QR_CIUDAD", "Asuncion"), "Asuncion"), 15);

        String cuentaTpl = tlv("00", gui) + tlv("01", cuenta);
        boolean sinDecimales = "600".equals(moneda);
        String montoStr = monto == null ? null
                : monto.setScale(sinDecimales ? 0 : 2, RoundingMode.HALF_UP).toPlainString();

        StringBuilder sb = new StringBuilder();
        sb.append(tlv("00", "01"));                              // payload format indicator
        sb.append(tlv("01", montoStr == null ? "11" : "12"));    // 11 estatico / 12 dinamico (con monto)
        sb.append(tlv("26", cuentaTpl));                         // merchant account info (GUI + cuenta)
        sb.append(tlv("52", mcc));
        sb.append(tlv("53", moneda));
        if (montoStr != null) sb.append(tlv("54", montoStr));
        sb.append(tlv("58", pais));
        sb.append(tlv("59", nombre));
        sb.append(tlv("60", ciudad));
        if (referencia != null && !referencia.isBlank())
            sb.append(tlv("62", tlv("05", recorta(soloAlfaNum(referencia), 25))));
        sb.append("6304");                                        // id+len del CRC (valor calculado sobre todo lo anterior + esto)
        sb.append(crc16(sb.toString()));
        return sb.toString();
    }

    /** PNG del QR como data URI base64 listo para usar en <img src>. Devuelve null si algo falla. */
    public String pngDataUri(String payload, int size) {
        if (payload == null || payload.isBlank()) return null;
        try {
            EnumMap<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    // ── helpers EMVCo ──
    private static String tlv(String id, String value) {
        String v = value == null ? "" : value;
        return id + String.format("%02d", v.length()) + v;
    }
    /** CRC16-CCITT (init 0xFFFF, poly 0x1021), 4 hex mayusculas — estandar EMVCo. */
    private static String crc16(String s) {
        int crc = 0xFFFF;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? ((crc << 1) ^ 0x1021) : (crc << 1);
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
    private static String recorta(String s, int max) { return s == null ? "" : (s.length() <= max ? s : s.substring(0, max)); }
    private static String defecto(String s, String d) { return s == null || s.isBlank() ? d : s; }
    private static String soloAlfaNum(String s) { return s == null ? "" : s.replaceAll("[^A-Za-z0-9]", ""); }
}
