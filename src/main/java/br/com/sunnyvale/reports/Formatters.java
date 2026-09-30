package br.com.sunnyvale.reports;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Formatters {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DecimalFormatSymbols BR = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"));

    private Formatters() { }

    /** Tokens aceitos em defaults e em --param: @hoje, @ontem, @inicio_mes, @fim_mes, @filial. */
    static String resolveToken(String v, AppConfig cfg) {
        if (v == null) {
            return "";
        }
        LocalDate today = LocalDate.now();
        return switch (v.trim().toLowerCase()) {
            case "@hoje" -> today.format(YMD);
            case "@ontem" -> today.minusDays(1).format(YMD);
            case "@inicio_mes" -> today.withDayOfMonth(1).format(YMD);
            case "@fim_mes" -> today.withDayOfMonth(today.lengthOfMonth()).format(YMD);
            case "@filial" -> cfg.filial();
            default -> v;
        };
    }

    /** Colunas cujo alias começa com "Data" ou "DT_" recebem datas AAAAMMDD do banco convertidas para dd/MM/aaaa. */
    static boolean isDateLabel(String label) {
        String l = label.toLowerCase();
        return l.startsWith("data") || l.startsWith("dt_");
    }

    /** Remove o preenchimento à direita típico do banco SQL, formata datas e trata nulos. */
    static Object normalize(String label, Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Number) {
            return v;
        }
        String s = v.toString().stripTrailing();
        if (isDateLabel(label) && s.matches("\\d{8}")) {
            return s.equals("00000000") ? "" : s.substring(6) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4);
        }
        return s;
    }

    static boolean isInteger(Number n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte
                || n instanceof BigInteger || (n instanceof BigDecimal b && b.scale() <= 0);
    }

    static String formatNumber(Number n) {
        DecimalFormat f = new DecimalFormat(isInteger(n) ? "#,##0" : "#,##0.00##", BR);
        return f.format(n);
    }

    /** Aceita dd/MM/aaaa, AAAAMMDD ou vazio e devolve AAAAMMDD (ou vazio). */
    static String toDbDate(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return "";
        }
        String y;
        String m;
        String d;
        if (t.matches("\\d{2}/\\d{2}/\\d{4}")) {
            d = t.substring(0, 2);
            m = t.substring(3, 5);
            y = t.substring(6);
        } else if (t.matches("\\d{8}")) {
            y = t.substring(0, 4);
            m = t.substring(4, 6);
            d = t.substring(6);
        } else {
            throw new IllegalArgumentException("Data inválida: '" + t + "'. Use dd/mm/aaaa.");
        }
        try {
            LocalDate.of(Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Data inexistente: '" + t + "'.");
        }
        return y + m + d;
    }

    /** AAAAMMDD para dd/MM/aaaa (para mostrar na tela); outros valores voltam como estão. */
    static String toDisplayDate(String ymd) {
        return ymd != null && ymd.matches("\\d{8}")
                ? ymd.substring(6) + "/" + ymd.substring(4, 6) + "/" + ymd.substring(0, 4) : (ymd == null ? "" : ymd);
    }
}
