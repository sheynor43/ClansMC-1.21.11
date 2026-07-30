package io.github.sheynor43.clans.storage.migration;

import io.github.sheynor43.clans.storage.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/** Initial schema: clans, members, relations, stats, pending XP. */
public final class V1Init implements Migration {

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "initial schema";
    }

    @Override
    public void apply(Connection connection, SqlDialect dialect) throws SQLException {
        String suffix = dialect.tableSuffix();
        String intT = dialect.intType();
        String bigT = dialect.bigIntType();
        String dblT = dialect.doubleType();

        String[] tables = {
                "CREATE TABLE IF NOT EXISTS `clans` ("
                        + "`id` " + intT + " PRIMARY KEY,"
                        + "`name` VARCHAR(32) NOT NULL,"
                        + "`name_lower` VARCHAR(32) NOT NULL,"
                        + "`tag` VARCHAR(16) NOT NULL,"
                        + "`tag_lower` VARCHAR(16) NOT NULL,"
                        + "`leader_uuid` VARCHAR(36) NOT NULL,"
                        + "`created_at` " + bigT + " NOT NULL,"
                        + "`level` " + intT + " NOT NULL DEFAULT 1,"
                        + "`clan_xp` " + bigT + " NOT NULL DEFAULT 0,"
                        + "`balance` " + dblT + " NOT NULL DEFAULT 0,"
                        + "UNIQUE (`name_lower`),"
                        + "UNIQUE (`tag_lower`)"
                        + ")" + suffix,
                "CREATE TABLE IF NOT EXISTS `clan_members` ("
                        + "`uuid` VARCHAR(36) PRIMARY KEY,"
                        + "`clan_id` " + intT + " NOT NULL,"
                        + "`last_name` VARCHAR(32),"
                        + "`role` VARCHAR(16) NOT NULL,"
                        + "`joined_at` " + bigT + " NOT NULL"
                        + ")" + suffix,
                "CREATE TABLE IF NOT EXISTS `clan_relations` ("
                        + "`clan_id` " + intT + " NOT NULL,"
                        + "`other_clan_id` " + intT + " NOT NULL,"
                        + "`type` VARCHAR(16) NOT NULL,"
                        + "`status` VARCHAR(16) NOT NULL,"
                        + "PRIMARY KEY (`clan_id`,`other_clan_id`)"
                        + ")" + suffix,
                "CREATE TABLE IF NOT EXISTS `clan_stats` ("
                        + "`clan_id` " + intT + " NOT NULL,"
                        + "`boss_type` VARCHAR(32) NOT NULL,"
                        + "`kills` " + intT + " NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (`clan_id`,`boss_type`)"
                        + ")" + suffix,
                "CREATE TABLE IF NOT EXISTS `pending_xp` ("
                        + "`uuid` VARCHAR(36) PRIMARY KEY,"
                        + "`amount` " + intT + " NOT NULL DEFAULT 0"
                        + ")" + suffix
        };

        // Non-unique index (kept separate: SQLite cannot declare it inline).
        String memberIndex = "CREATE INDEX `idx_members_clan` ON `clan_members`(`clan_id`)";

        try (Statement statement = connection.createStatement()) {
            for (String ddl : tables) {
                statement.execute(ddl);
            }
            executeTolerant(statement, memberIndex);
        }
    }

    /** Runs a statement, ignoring "already exists" errors so re-runs are safe. */
    private void executeTolerant(Statement statement, String ddl) throws SQLException {
        try {
            statement.execute(ddl);
        } catch (SQLException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
            if (!message.contains("exist") && !message.contains("duplicate")) {
                throw ex;
            }
        }
    }
}
