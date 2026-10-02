package br.com.sunnyvale.reports.core;

import java.util.ArrayList;
import java.util.List;

/** Converte parâmetros nomeados (:nome) em '?' preservando a ordem; ignora texto entre aspas simples. */
record NamedQuery(String sql, List<String> names) {

    static NamedQuery parse(String src) {
        StringBuilder out = new StringBuilder();
        List<String> names = new ArrayList<>();
        boolean inString = false;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '\'') {
                inString = !inString;
                out.append(c);
                continue;
            }
            if (!inString && c == ':' && i + 1 < src.length()
                    && Character.isJavaIdentifierStart(src.charAt(i + 1))) {
                int j = i + 1;
                while (j < src.length() && Character.isJavaIdentifierPart(src.charAt(j))) {
                    j++;
                }
                names.add(src.substring(i + 1, j).toLowerCase());
                out.append('?');
                i = j - 1;
                continue;
            }
            out.append(c);
        }
        return new NamedQuery(out.toString(), names);
    }
}
