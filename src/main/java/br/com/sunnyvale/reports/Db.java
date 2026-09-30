package br.com.sunnyvale.reports;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

final class Db {

    private Db() { }

    static Connection connect(AppConfig cfg) throws SQLException {
        if (cfg.url().isBlank()) {
            throw new IllegalArgumentException("db.url não configurado (config.properties ou variável DB_URL).");
        }
        Connection c = DriverManager.getConnection(cfg.url(), cfg.user(), cfg.password());
        try {
            c.setReadOnly(true); // apenas dica ao driver; a garantia real é o usuário somente leitura
        } catch (SQLException ignored) {
            // segue sem a dica
        }
        return c;
    }
}
