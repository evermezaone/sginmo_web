package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REQ-0084 (Fase 2) - Extraccion de datos del comprobante de transferencia.
 *  - PDF de texto: PDFBox (Java puro, sin binario nativo).
 *  - Imagen (JPG/PNG/WEBP) o PDF escaneado: se intenta la CLI de Tesseract si esta instalada en el sistema
 *    (ProcessBuilder). Si no esta, degrada limpiamente (texto null, motor NINGUNO) y el operador lo lee a mano;
 *    al instalar el binario `tesseract` se activa solo, sin cambios de codigo.
 * El OCR NUNCA aplica pagos: es solo insumo para la revision en la bandeja.
 */
@ApplicationScoped
public class ComprobanteOcrService {

    private static final Pattern P_FECHA = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern P_IMPORTE = Pattern.compile(
        "(?:gs\\.?|g\\.?|₲|importe|monto|total)[^\\d]{0,12}([\\d][\\d\\.]{3,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NUMERO = Pattern.compile(
        "(?:operaci[oó]n|transacci[oó]n|comprobante|nro\\.?|n[uú]mero|ref(?:erencia)?)[^\\d]{0,12}(\\d{4,})",
        Pattern.CASE_INSENSITIVE);
    private static final String[] BANCOS = {
        "Itau", "Itáu", "Continental", "Sudameris", "Familiar", "Regional", "Vision", "Atlas", "GNB",
        "BNF", "Ueno", "Banco Nacional", "Interfisa", "Solar", "Basa", "Rio", "Zeta", "Do Brasil"
    };

    /** Resultado de la extraccion. */
    public static class Resultado {
        public String texto, motor, banco, numero;
        public BigDecimal importe;
        public LocalDate fecha;
        public BigDecimal confianza = BigDecimal.ZERO;
    }

    public Resultado extraer(byte[] datos, String mime) {
        Resultado r = new Resultado();
        r.motor = "NINGUNO";
        if (datos == null || datos.length == 0) return r;
        boolean esPdf = esPdf(datos, mime);
        try {
            if (esPdf) {
                r.texto = extraerPdf(datos);
                r.motor = "PDF";
                if (r.texto == null || r.texto.isBlank()) {
                    // PDF sin capa de texto (escaneado): intentar Tesseract sobre el PDF.
                    String t = extraerConTesseract(datos, ".pdf");
                    if (t != null) { r.texto = t; r.motor = "TESSERACT"; }
                }
            } else {
                String t = extraerConTesseract(datos, extPorMime(mime));
                if (t != null) { r.texto = t; r.motor = "TESSERACT"; }
            }
        } catch (Throwable e) {
            // Cualquier fallo de extraccion no interrumpe el flujo: queda para revision manual.
            r.motor = esPdf ? "PDF" : "NINGUNO";
        }
        parsear(r);
        return r;
    }

    private void parsear(Resultado r) {
        if (r.texto == null || r.texto.isBlank()) return;
        int encontrados = 0;
        Matcher mf = P_FECHA.matcher(r.texto);
        if (mf.find()) {
            try { r.fecha = LocalDate.parse(mf.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy")); encontrados++; }
            catch (RuntimeException ignore) { }
        }
        Matcher mi = P_IMPORTE.matcher(r.texto);
        if (mi.find()) {
            try {
                String num = mi.group(1).replace(".", "").replace(" ", "");
                if (!num.isEmpty()) { r.importe = new BigDecimal(num); encontrados++; }
            } catch (RuntimeException ignore) { }
        }
        Matcher mn = P_NUMERO.matcher(r.texto);
        if (mn.find()) { r.numero = mn.group(1); encontrados++; }
        String low = r.texto.toLowerCase();
        for (String b : BANCOS) {
            if (low.contains(b.toLowerCase())) { r.banco = b; encontrados++; break; }
        }
        r.confianza = BigDecimal.valueOf(encontrados * 25L);   // 4 campos -> 0..100
    }

    private static boolean esPdf(byte[] d, String mime) {
        if (mime != null && mime.toLowerCase().contains("pdf")) return true;
        return d.length >= 4 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F';
    }

    private static String extPorMime(String mime) {
        String m = mime == null ? "" : mime.toLowerCase();
        if (m.contains("png")) return ".png";
        if (m.contains("webp")) return ".webp";
        return ".jpg";
    }

    private String extraerPdf(byte[] datos) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(datos)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
        }
    }

    /** Corre la CLI `tesseract` si esta en el PATH; null si no esta instalada o falla. */
    private String extraerConTesseract(byte[] datos, String ext) {
        Path tmp = null, outBase = null;
        try {
            tmp = Files.createTempFile("cmp_", ext);
            Files.write(tmp, datos);
            outBase = Files.createTempFile("ocr_", "");
            Files.deleteIfExists(outBase);   // tesseract agrega .txt
            String lang = System.getenv().getOrDefault("TESSERACT_LANG", "spa+eng");
            ProcessBuilder pb = new ProcessBuilder("tesseract", tmp.toString(), outBase.toString(), "-l", lang);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean ok = p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!ok) { p.destroyForcibly(); return null; }
            Path out = Path.of(outBase.toString() + ".txt");
            if (Files.exists(out)) {
                String texto = Files.readString(out);
                Files.deleteIfExists(out);
                return texto;
            }
            return null;
        } catch (java.io.IOException e) {
            return null;   // binario no instalado -> degrada
        } catch (Exception e) {
            return null;
        } finally {
            try { if (tmp != null) Files.deleteIfExists(tmp); } catch (Exception ignore) { }
        }
    }
}
