package br.com.sunnyvale.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ReportRunner {

    private ReportRunner() { }

    static ReportResult run(Connection con, ReportDefinition def, Map<String, String> values,
                            String tableSuffix, int maxRows) throws SQLException {
        String raw = def.sql().replace("${SUF}", tableSuffix);
        String head = raw.stripLeading().toUpperCase(Locale.ROOT);
        if (!head.startsWith("SELECT") && !head.startsWith("WITH")) {
            throw new IllegalArgumentException("Relatório " + def.id() + ": apenas consultas SELECT/WITH são permitidas.");
        }
        NamedQuery q = NamedQuery.parse(raw);

        try (PreparedStatement ps = con.prepareStatement(q.sql())) {
            ps.setQueryTimeout(120);
            if (maxRows > 0) {
                ps.setMaxRows(maxRows + 1);
            }
            List<String> names = q.names();
            for (int i = 0; i < names.size(); i++) {
                String v = values.get(names.get(i));
                if (v == null) {
                    throw new IllegalArgumentException("Relatório " + def.id() + ": parâmetro :" + names.get(i)
                            + " está no SQL mas não foi declarado com '-- param:'.");
                }
                ps.setString(i + 1, v);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<String> cols = new ArrayList<>();
                for (int i = 1; i <= n; i++) {
                    cols.add(md.getColumnLabel(i));
                }
                List<Object[]> rows = new ArrayList<>();
                boolean truncated = false;
                while (rs.next()) {
                    if (maxRows > 0 && rows.size() >= maxRows) {
                        truncated = true;
                        break;
                    }
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) {
                        row[i] = Formatters.normalize(cols.get(i), rs.getObject(i + 1));
                    }
                    rows.add(row);
                }
                return new ReportResult(cols, rows, truncated);
            }
        }
    }
}
