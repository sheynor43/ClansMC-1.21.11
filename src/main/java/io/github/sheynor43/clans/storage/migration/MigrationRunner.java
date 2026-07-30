package io.github.sheynor43.clans.storage.migration;

import io.github.sheynor43.clans.storage.SqlDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

/**
 * Applies pending {@link Migration}s in order, tracked by a {@code schema_version}
 * table so future schema changes never require rewriting existing data.
 */
public final class MigrationRunner {

    private final SqlDialect dialect;
    private final Logger logger;
    private final List<Migration> migrations;

    public MigrationRunner(SqlDialect dialect, Logger logger, List<Migration> migrations) {
        this.dialect = dialect;
        this.logger = logger;
        this.migrations = migrations.stream()
                .sorted((a, b) -> Integer.compare(a.version(), b.version()))
                .toList();
    }

    public void run(Connection connection) throws SQLException {
        ensureVersionTable(connection);
        int current = currentVersion(connection);

        for (Migration migration : migrations) {
            if (migration.version() <= current) {
                continue;
            }
            logger.info("Applying schema migration v" + migration.version()
                    + " (" + migration.description() + ")...");
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                migration.apply(connection, dialect);
                recordVersion(connection, migration.version());
                connection.commit();
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // Best effort; DDL on MySQL auto-commits and cannot be rolled back.
                }
                throw ex;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    private void ensureVersionTable(Connection connection) throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS `schema_version` ("
                + "`version` " + dialect.intType() + " PRIMARY KEY,"
                + "`applied_at` " + dialect.bigIntType() + " NOT NULL"
                + ")" + dialect.tableSuffix();
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT MAX(`version`) FROM `schema_version`")) {
            if (rs.next()) {
                int value = rs.getInt(1);
                return rs.wasNull() ? 0 : value;
            }
            return 0;
        }
    }

    private void recordVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO `schema_version` (`version`, `applied_at`) VALUES (?, ?)")) {
            ps.setInt(1, version);
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }
}
