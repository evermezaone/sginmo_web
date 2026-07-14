package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    /** El pago por QR esta activo solo si se habilito y hay una cuenta destino configurada. */
    public boolean habilitado() {
        return parametros.booleano("PORTAL_QR_HABILITADO", false)
            && !parametros.texto("PORTAL_QR_CUENTA", "").isBlank();
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
