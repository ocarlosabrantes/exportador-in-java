package br.com.sunnyvale.reports;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

final class PdfExporter {

    private static final Font FOOT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Color HEAD_BG = new Color(0x33, 0x4E, 0x68);
    private static final Color ZEBRA = new Color(0xF3, 0xF5, 0xF7);

    private PdfExporter() { }

    static void export(Path file, ReportDefinition def, Map<String, String> params, ReportResult r) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 28, 36);
        try (OutputStream out = Files.newOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Footer());
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font infoFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
            doc.add(new Paragraph(def.title(), titleFont));

            String paramText = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; "));
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            doc.add(new Paragraph("Parâmetros: " + paramText + "  |  Gerado em " + now, infoFont));
            if (r.truncated()) {
                doc.add(new Paragraph("ATENÇÃO: resultado limitado a " + r.rows().size() + " linhas.", infoFont));
            }
            doc.add(new Paragraph(" ", infoFont));

            int n = r.columns().size();
            if (n > 0) {
                float size = n > 10 ? 6.5f : (n > 7 ? 7f : 8f);
                Font headFont = new Font(Font.HELVETICA, size, Font.BOLD, Color.WHITE);
                Font cellFont = new Font(Font.HELVETICA, size, Font.NORMAL);

                float[] w = new float[n];
                for (int j = 0; j < n; j++) {
                    w[j] = r.columns().get(j).length();
                }
                for (Object[] row : r.rows()) {
                    for (int j = 0; j < n; j++) {
                        w[j] = Math.max(w[j], text(row[j]).length());
                    }
                }
                for (int j = 0; j < n; j++) {
                    w[j] = Math.min(40f, Math.max(4f, w[j]));
                }

                PdfPTable table = new PdfPTable(n);
                table.setWidthPercentage(100);
                table.setWidths(w);
                table.setHeaderRows(1);
                for (String col : r.columns()) {
                    table.addCell(cell(col, headFont, Element.ALIGN_CENTER, HEAD_BG));
                }
                int i = 0;
                for (Object[] row : r.rows()) {
                    Color bg = (i++ % 2 == 1) ? ZEBRA : null;
                    for (int j = 0; j < n; j++) {
                        boolean num = row[j] instanceof Number;
                        table.addCell(cell(text(row[j]), cellFont, num ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT, bg));
                    }
                }
                doc.add(table);
            }
            if (r.rows().isEmpty()) {
                doc.add(new Paragraph("Nenhum registro encontrado para os parâmetros informados.", infoFont));
            }
            doc.close();
        } catch (DocumentException e) {
            throw new IOException("Falha ao gerar PDF: " + e.getMessage(), e);
        }
    }

    private static String text(Object v) {
        return v instanceof Number num ? Formatters.formatNumber(num) : String.valueOf(v);
    }

    private static PdfPCell cell(String text, Font font, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setPadding(3);
        c.setBorderWidth(0.4f);
        if (bg != null) {
            c.setBackgroundColor(bg);
        }
        return c;
    }

    private static final class Footer extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), FOOT),
                    document.right(), document.bottom() - 14, 0);
        }
    }
}
