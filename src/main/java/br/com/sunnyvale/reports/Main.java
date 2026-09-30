package br.com.sunnyvale.reports;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Main {

    private Main() { }

    public static void main(String[] args) {
        // Silencia o aviso do log4j-api usado internamente pelo Apache POI
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        int code;
        try {
            code = run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            code = 2;
        } catch (Exception e) {
            System.err.println("Falha: " + e.getMessage());
            if (System.getenv("DEBUG") != null) {
                e.printStackTrace();
            }
            code = 1;
        }
        System.exit(code);
    }

    private static int run(String[] args) throws Exception {
        Path configFile = Path.of("config.properties");
        Path reportsDir = Path.of("reports");
        Path outDir = Path.of("saida");
        String reportId = null;
        Set<String> formats = new LinkedHashSet<>(List.of("xlsx", "pdf"));
        Map<String, String> cli = new LinkedHashMap<>();
        boolean list = false;
        int maxRows = 100_000;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--help", "-h" -> {
                    usage();
                    return 0;
                }
                case "--list" -> list = true;
                case "--report" -> reportId = value(args, ++i, a);
                case "--config" -> configFile = Path.of(value(args, ++i, a));
                case "--reports" -> reportsDir = Path.of(value(args, ++i, a));
                case "--out" -> outDir = Path.of(value(args, ++i, a));
                case "--max-rows" -> {
                    try {
                        maxRows = Integer.parseInt(value(args, ++i, a));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("--max-rows precisa de um número inteiro (0 = sem limite).");
                    }
                }
                case "--format" -> {
                    formats = new LinkedHashSet<>();
                    for (String f : value(args, ++i, a).split(",")) {
                        String fmt = f.trim().toLowerCase();
                        if (!fmt.equals("xlsx") && !fmt.equals("pdf")) {
                            throw new IllegalArgumentException("Formato inválido: " + f + " (use xlsx, pdf ou xlsx,pdf)");
                        }
                        formats.add(fmt);
                    }
                }
                case "--param" -> {
                    String kv = value(args, ++i, a);
                    int eq = kv.indexOf('=');
                    if (eq < 1) {
                        throw new IllegalArgumentException("Use --param nome=valor");
                    }
                    cli.put(kv.substring(0, eq).trim().toLowerCase(), kv.substring(eq + 1));
                }
                default -> throw new IllegalArgumentException("Opção desconhecida: " + a + " (veja --help)");
            }
        }

        List<ReportDefinition> defs = ReportLoader.loadAll(reportsDir);
        if (list) {
            printList(defs);
            return 0;
        }
        if (reportId == null) {
            usage();
            return 2;
        }

        ReportDefinition def = null;
        for (ReportDefinition d : defs) {
            if (d.id().equalsIgnoreCase(reportId)) {
                def = d;
            }
        }
        if (def == null) {
            throw new IllegalArgumentException("Relatório não encontrado: " + reportId + " (use --list)");
        }

        AppConfig cfg = AppConfig.load(configFile);
        Map<String, String> values = new LinkedHashMap<>();
        for (ReportDefinition.Param p : def.params()) {
            values.put(p.name(), Formatters.resolveToken(p.defaultValue(), cfg));
        }
        for (Map.Entry<String, String> e : cli.entrySet()) {
            if (!values.containsKey(e.getKey())) {
                throw new IllegalArgumentException("Parâmetro '" + e.getKey() + "' não existe em " + def.id()
                        + ". Disponíveis: " + values.keySet());
            }
            values.put(e.getKey(), Formatters.resolveToken(e.getValue(), cfg));
        }

        System.out.println("Relatório : " + def.title());
        System.out.println("Parâmetros: " + values);

        try (Connection con = Db.connect(cfg)) {
            long t0 = System.currentTimeMillis();
            ReportResult result = ReportRunner.run(con, def, values, cfg.tableSuffix(), maxRows);
            long ms = System.currentTimeMillis() - t0;
            System.out.println("Linhas    : " + result.rows().size() + (result.truncated() ? " (limitado por --max-rows)" : "")
                    + "  [" + ms + " ms]");
            if (result.rows().isEmpty()) {
                System.out.println("Aviso     : consulta sem resultados; os arquivos serão gerados só com o cabeçalho.");
            }

            Files.createDirectories(outDir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            for (String fmt : formats) {
                Path file = outDir.resolve(def.id() + "_" + stamp + "." + fmt);
                if (fmt.equals("xlsx")) {
                    ExcelExporter.export(file, def, values, result);
                } else {
                    PdfExporter.export(file, def, values, result);
                }
                System.out.println("Gerado    : " + file.toAbsolutePath());
            }
        }
        return 0;
    }

    private static String value(String[] args, int idx, String opt) {
        if (idx >= args.length) {
            throw new IllegalArgumentException("A opção " + opt + " precisa de um valor.");
        }
        return args[idx];
    }

    private static void printList(List<ReportDefinition> defs) {
        for (ReportDefinition d : defs) {
            System.out.println(d.id() + "  -  " + d.title());
            if (!d.description().isEmpty()) {
                System.out.println("    " + d.description());
            }
            for (ReportDefinition.Param p : d.params()) {
                System.out.println("    --param " + p.name() + "=...   (padrão: '" + p.defaultValue() + "')  " + p.description());
            }
            System.out.println();
        }
    }

    private static void usage() {
        System.out.println("""
                Report Exporter

                Uso:
                  java -jar report-exporter.jar --list
                  java -jar report-exporter.jar --report <id> [opções]

                Opções:
                  --report <id>        Relatório (arquivo .sql em reports/)
                  --param nome=valor   Parâmetro do relatório (repetível). Tokens: @hoje @ontem @inicio_mes @fim_mes @filial
                  --format xlsx,pdf    Formatos de saída (padrão: xlsx,pdf)
                  --out <pasta>        Pasta de saída (padrão: ./C:/temp)
                  --max-rows <n>       Limite de linhas (padrão: 100000; 0 = sem limite)
                  --config <arquivo>   Arquivo de configuração (padrão: ./config.properties)
                  --reports <pasta>    Pasta dos relatórios (padrão: ./reports)
                  --list               Lista relatórios e parâmetros
                """);
    }
}
