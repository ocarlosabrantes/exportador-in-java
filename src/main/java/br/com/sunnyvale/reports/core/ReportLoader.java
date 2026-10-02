package br.com.sunnyvale.reports.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lê arquivos .sql com cabeçalho de metadados:
 * <pre>
 * -- title: Nome do relatório
 * -- description: Texto livre
 * -- param: nome | default=valor | type=text/date/choice/hidden | options=A;B | desc=descrição
 * </pre>
 * Demais linhas iniciadas por "--" são comentários e são descartadas.
 */
public final class ReportLoader {

    private ReportLoader() {
    }

    public static List<ReportDefinition> loadAll(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IOException("Pasta de relatórios não encontrada: " + dir.toAbsolutePath());
        }
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".sql")).sorted().toList();
        }
        List<ReportDefinition> out = new ArrayList<>();
        for (Path f : files) {
            out.add(parse(f));
        }
        return out;
    }

    static ReportDefinition parse(Path file) throws IOException {
        String name = file.getFileName().toString();
        String id = name.substring(0, name.length() - 4);
        String title = id;
        String description = "";
        boolean requerLogin = false;
        List<ReportDefinition.Param> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.startsWith("--")) {
                String c = t.substring(2).trim();
                if (c.startsWith("title:")) {
                    title = c.substring(6).trim();
                } else if (c.startsWith("description:")) {
                    description = c.substring(12).trim();
                } else if (c.startsWith("param:")) {
                    params.add(parseParam(c.substring(6)));
                } else if (c.startsWith("requer_login:")) {
                    requerLogin = c.substring(13).trim().equalsIgnoreCase("true");
                }
                continue;
            }
            sql.append(line).append('\n');
        }

        String body = sql.toString().trim();
        while (body.endsWith(";")) {
            body = body.substring(0, body.length() - 1).trim();
        }
        if (body.isEmpty()) {
            throw new IOException("Arquivo sem SQL: " + file);
        }
        return new ReportDefinition(id, title, description, params, body, requerLogin);
    }

    private static ReportDefinition.Param parseParam(String spec) {
        String[] parts = spec.split("\\|", -1);
        String name = parts[0].trim().toLowerCase();
        String def = "";
        String desc = "";
        String type = "text";
        List<String> options = List.of();
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.startsWith("default=")) {
                def = p.substring(8).trim();
            } else if (p.startsWith("desc=")) {
                desc = p.substring(5).trim();
            } else if (p.startsWith("type=")) {
                type = p.substring(5).trim().toLowerCase();
            } else if (p.startsWith("options=")) {
                options = Arrays.stream(p.substring(8).split(";")).map(String::trim).filter(x -> !x.isEmpty()).toList();
            }
        }
        return new ReportDefinition.Param(name, def, desc, type, options);
    }
}