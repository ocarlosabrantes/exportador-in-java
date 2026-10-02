package br.com.sunnyvale.reports.core;

import java.util.List;

/**
 * Relatório carregado de um arquivo .sql da pasta reports/.
 */
public record ReportDefinition(String id, String title, String description, List<Param> params, String sql,
                               boolean requerLogin) {

    /**
     * type: text (padrão), date, choice (usa options) ou hidden (não aparece na tela, usa o padrão).
     */
    public record Param(String name, String defaultValue, String description, String type, List<String> options) {
    }
}