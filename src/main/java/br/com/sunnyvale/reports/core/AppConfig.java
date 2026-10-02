package br.com.sunnyvale.reports.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuração lida de config.properties; variáveis de ambiente _DB_* têm prioridade. */
public record AppConfig(String url, String user, String password, String filial, String tableSuffix) {

    public static AppConfig load(Path file) throws IOException {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        }
        String suffix = p.getProperty("db.table_suffix", "010").trim();
        if (!suffix.matches("[A-Za-z0-9]{0,4}")) {
            throw new IllegalArgumentException("db.table_suffix inválido: " + suffix);
        }
        return new AppConfig(
                env("DB_URL", p.getProperty("db.url", "")).trim(),
                env("DB_USER", p.getProperty("db.user", "")).trim(),
                env("DB_PASSWORD", p.getProperty("db.password", "")),
                p.getProperty("default.filial", "01").trim(),
                suffix);
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }
}
