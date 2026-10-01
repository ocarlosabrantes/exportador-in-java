package br.com.sunnyvale.reports;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

final class ExcelExporter {

    private ExcelExporter() { }

    static void export(Path file, ReportDefinition def, Map<String, String> params, ReportResult r) throws IOException {
        int cols = r.columns().size();
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            String sheetName = WorkbookUtil.createSafeSheetName(def.title());
            if (sheetName.length() > 31) {
                sheetName = sheetName.substring(0, 31);
            }
            SXSSFSheet sh = wb.createSheet(sheetName);

            Font bold = wb.createFont();
            bold.setBold(true);
            Font big = wb.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);

            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(big);
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(bold);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setBorderBottom(BorderStyle.THIN);
            headStyle.setAlignment(HorizontalAlignment.CENTER);
            DataFormat df = wb.createDataFormat();
            CellStyle intStyle = wb.createCellStyle();
            intStyle.setDataFormat(df.getFormat("#,##0"));
            CellStyle decStyle = wb.createCellStyle();
            decStyle.setDataFormat(df.getFormat("#,##0.00##"));

            int[] width = new int[cols];

            Cell t = sh.createRow(0).createCell(0);
            t.setCellValue(def.title());
            t.setCellStyle(titleStyle);

            String paramText = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; "));
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sh.createRow(1).createCell(0).setCellValue("Gerado em " + now);

            if (r.truncated()) {
                sh.createRow(2).createCell(0).setCellValue(
                        "ATENÇÃO: resultado limitado a " + r.rows().size() + " linhas (use --max-rows para ajustar).");
            }

            Row head = sh.createRow(3);
            for (int j = 0; j < cols; j++) {
                Cell c = head.createCell(j);
                String label = r.columns().get(j);
                c.setCellValue(label);
                c.setCellStyle(headStyle);
                width[j] = label.length();
            }

            int rowNum = 4;
            for (Object[] data : r.rows()) {
                Row row = sh.createRow(rowNum++);
                for (int j = 0; j < cols; j++) {
                    Object v = data[j];
                    Cell c = row.createCell(j);
                    if (v instanceof Number n) {
                        c.setCellValue(n.doubleValue());
                        c.setCellStyle(Formatters.isInteger(n) ? intStyle : decStyle);
                        width[j] = Math.max(width[j], Formatters.formatNumber(n).length());
                    } else {
                        String s = String.valueOf(v);
                        c.setCellValue(s);
                        width[j] = Math.max(width[j], s.length());
                    }
                }
            }

            for (int j = 0; j < cols; j++) {
                sh.setColumnWidth(j, Math.min(60, Math.max(8, width[j] + 2)) * 256);
            }
            if (cols > 0) {
                sh.createFreezePane(0, 4);
                sh.setAutoFilter(new CellRangeAddress(3, Math.max(3, rowNum - 1), 0, cols - 1));
            }

            try (OutputStream out = Files.newOutputStream(file)) {
                wb.write(out);
            }
        }
    }
}
