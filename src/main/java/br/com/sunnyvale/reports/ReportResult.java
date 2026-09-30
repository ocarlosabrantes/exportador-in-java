package br.com.sunnyvale.reports;

import java.util.List;

record ReportResult(List<String> columns, List<Object[]> rows, boolean truncated) { }
