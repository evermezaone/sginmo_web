package py.com.pysistemas.sginmo.servicio;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;
import py.com.one.core.NegocioException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generador de PDFs ESTANDAR con OpenPDF (decision del usuario 2026-07-06: sin JasperReports,
 * PDFs simples y directos). Helper reutilizable: encabezado (empresa + titulo + fecha/usuario),
 * cuerpo (tabla o parrafos), pie con paginado. No hay plantillas .jrxml ni herramientas externas.
 */
@ApplicationScoped
@AislarTenant
@jakarta.transaction.Transactional   // F5: fija app.tenant en la tx para RLS (V28)
public class PdfService {

    private static final Font H1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.decode("#1d3557"));
    private static final Font SUB = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
    private static final Font TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TD = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font TXT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Color AZUL = Color.decode("#457b9d");
    private static final DateTimeFormatter FCHT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Pie estandar en CADA pagina (obs 234): texto identificatorio a la izquierda y
     * "Página N de M" a la derecha. El total de paginas se resuelve con la tecnica del
     * PdfTemplate: se estampa al cerrar el documento.
     */
    private static class PiePagina extends com.lowagie.text.pdf.PdfPageEventHelper {
        private final String texto;
        private com.lowagie.text.pdf.PdfTemplate total;
        private com.lowagie.text.pdf.BaseFont bf;
        PiePagina(String texto) { this.texto = texto == null ? "" : texto; }

        @Override
        public void onOpenDocument(PdfWriter w, Document d) {
            try {
                bf = com.lowagie.text.pdf.BaseFont.createFont(
                        com.lowagie.text.pdf.BaseFont.HELVETICA, com.lowagie.text.pdf.BaseFont.WINANSI, false);
                total = w.getDirectContent().createTemplate(30, 12);
            } catch (Exception e) {
                throw new com.lowagie.text.ExceptionConverter(e);
            }
        }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            var cb = w.getDirectContent();
            float y = d.bottom() - 18;
            String pag = "Página " + w.getPageNumber() + " de ";
            float len = bf.getWidthPoint(pag, 8);
            cb.saveState();
            cb.beginText();
            cb.setFontAndSize(bf, 8);
            cb.setColorFill(Color.GRAY);
            cb.setTextMatrix(d.left(), y);
            cb.showText(texto);
            cb.setTextMatrix(d.right() - len - 20, y);
            cb.showText(pag);
            cb.endText();
            cb.addTemplate(total, d.right() - 20, y);
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter w, Document d) {
            total.beginText();
            total.setFontAndSize(bf, 8);
            total.setColorFill(Color.GRAY);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(w.getPageNumber() - 1));
            total.endText();
        }
    }

    /** Documento base con encabezado; el llamador agrega contenido y llama a cerrar(). */
    public static class Reporte {
        final Document doc;
        final ByteArrayOutputStream salida;
        Reporte(Document d, ByteArrayOutputStream s) { this.doc = d; this.salida = s; }
        public Document doc() { return doc; }
    }

    public Reporte iniciar(String empresa, String titulo, String usuario, String subtitulo) {
        var salida = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4, 36, 36, 42, 52);
        PdfWriter writer = PdfWriter.getInstance(doc, salida);
        // pie con paginado en todas las paginas (obs 234)
        writer.setPageEvent(new PiePagina((empresa == null ? "SGInmo" : empresa) + " — " + titulo));
        doc.open();
        var enc = new PdfPTable(new float[]{3, 2});
        enc.setWidthPercentage(100);
        enc.getDefaultCell().setBorder(0);
        var izq = new PdfPCell();
        izq.setBorder(0);
        izq.addElement(new Paragraph(empresa == null ? "SGInmo" : empresa, H1));
        izq.addElement(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK)));
        if (subtitulo != null && !subtitulo.isBlank()) {
            izq.addElement(new Paragraph(subtitulo, SUB));
        }
        enc.addCell(izq);
        var der = new PdfPCell(new Phrase(
                "Emitido: " + LocalDateTime.now().format(FCHT) + "\nUsuario: " + (usuario == null ? "-" : usuario), SUB));
        der.setBorder(0);
        der.setHorizontalAlignment(Element.ALIGN_RIGHT);
        enc.addCell(der);
        try {
            doc.add(enc);
            doc.add(new Paragraph(" ", SUB));
        } catch (Exception e) {
            throw new NegocioException("No se pudo generar el PDF: " + e.getMessage());
        }
        return new Reporte(doc, salida);
    }

    /** Tabla estandar: cabecera azul + filas alternadas. anchos opcional (proporciones). */
    public void tabla(Reporte r, String[] columnas, List<String[]> filas, float[] anchos) {
        try {
            var t = anchos != null ? new PdfPTable(anchos) : new PdfPTable(columnas.length);
            t.setWidthPercentage(100);
            for (String c : columnas) {
                var cell = new PdfPCell(new Phrase(c, TH));
                cell.setBackgroundColor(AZUL);
                cell.setPadding(4);
                t.addCell(cell);
            }
            boolean alterna = false;
            for (String[] fila : filas) {
                for (String v : fila) {
                    var cell = new PdfPCell(new Phrase(v == null ? "" : v, TD));
                    cell.setPadding(3);
                    if (alterna) cell.setBackgroundColor(Color.decode("#f4f6f9"));
                    t.addCell(cell);
                }
                alterna = !alterna;
            }
            r.doc.add(t);
        } catch (Exception e) {
            throw new NegocioException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    public void parrafo(Reporte r, String texto) {
        try {
            r.doc.add(new Paragraph(texto, TXT));
        } catch (Exception e) {
            throw new NegocioException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    public void espacio(Reporte r) {
        try { r.doc.add(new Paragraph(" ")); } catch (Exception ignored) { }
    }

    public byte[] cerrar(Reporte r) {
        r.doc.close();
        return r.salida.toByteArray();
    }
}
