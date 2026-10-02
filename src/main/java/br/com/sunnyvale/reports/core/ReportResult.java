package br.com.sunnyvale.reports.core;

import java.util.List;

public record ReportResult(List<String> columns, List<Object[]> rows, boolean truncated) {
}
