package io.github.sheynor43.clans.storage;

import com.zaxxer.hikari.HikariDataSource;
import io.github.sheynor43.clans.api.RelationStatus;
import io.github.sheynor43.clans.api.RelationType;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.model.ClanMember;
import io.github.sheynor43.clans.model.ClanRelation;
import io.github.sheynor43.clans.model.ClanRole;
import io.github.sheynor43.clans.storage.migration.MigrationRunner;
import io.github.sheynor43.clans.storage.migration.V1Init;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared JDBC implementation of {@link ClanStorage}. Concrete subclasses only
 * provide a configured {@link HikariDataSource} and the matching {@link SqlDialect}.
 * All work runs on a dedicated single-thread executor, which also guarantees
 * write ordering.
 */
public abstract class SqlClanStorage implements ClanStorage {

    private final SqlDialect dialect;
    private final Logger logger;
    private final ExecutorService executor;

    private HikariDataSource dataSource;

    protected SqlClanStorage(SqlDialect dialect, Logger logger) {
        this.dialect = dialect;
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ClansMC-Storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Creates the pool. Implemented by the SQLite / MySQL subclasses. */
    protected abstract HikariDataSource createDataSource();

    protected SqlDialect dialect() {
        return dialect;
    }

    // ---- lifecycle ----------------------------------------------------------

    @Override
    public CompletableFuture<Void> init() {
        return run(() -> {
            dataSource = createDataSource();
            try (Connection connection = dataSource.getConnection()) {
                new MigrationRunner(dialect, logger, List.of(new V1Init())).run(connection);
            }
        });
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ---- reads --------------------------------------------------------------

    @Override
    public CompletableFuture<List<Clan>> loadAll() {
        return supply(() -> {
            Map<Integer, Clan> clans = new HashMap<>();
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT `id`,`name`,`tag`,`leader_uuid`,`created_at`,`level`,`clan_xp`,`balance` FROM `clans`");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Clan clan = new Clan(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("tag"),
                                UUID.fromString(rs.getString("leader_uuid")),
                                rs.getLong("created_at"),
                                rs.getInt("level"),
                                rs.getLong("clan_xp"),
                                rs.getDouble("balance"));
                        clans.put(clan.id(), clan);
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT `uuid`,`clan_id`,`last_name`,`role`,`joined_at` FROM `clan_members`");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Clan clan = clans.get(rs.getInt("clan_id"));
                        if (clan == null) {
                            continue;
                        }
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        clan.membersMap().put(uuid, new ClanMember(
                                uuid,
                                rs.getString("last_name"),
                                parseRole(rs.getString("role")),
                                rs.getLong("joined_at")));
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT `clan_id`,`other_clan_id`,`type`,`status` FROM `clan_relations`");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Clan clan = clans.get(rs.getInt("clan_id"));
                        if (clan == null) {
                            continue;
                        }
                        int other = rs.getInt("other_clan_id");
                        clan.relationsMap().put(other, new ClanRelation(
                                other,
                                RelationType.valueOf(rs.getString("type")),
                                RelationStatus.valueOf(rs.getString("status"))));
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT `clan_id`,`boss_type`,`kills` FROM `clan_stats`");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Clan clan = clans.get(rs.getInt("clan_id"));
                        if (clan == null) {
                            continue;
                        }
                        clan.statsMap().put(rs.getString("boss_type"), rs.getInt("kills"));
                    }
                }
            }
            return new ArrayList<>(clans.values());
        });
    }

    // ---- writes -------------------------------------------------------------

    @Override
    public CompletableFuture<Void> insertClan(Clan clan, String leaderName) {
        return run(() -> inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `clans` (`id`,`name`,`name_lower`,`tag`,`tag_lower`,`leader_uuid`,"
                            + "`created_at`,`level`,`clan_xp`,`balance`) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                ps.setInt(1, clan.id());
                ps.setString(2, clan.name());
                ps.setString(3, clan.name().toLowerCase(Locale.ROOT));
                ps.setString(4, clan.tag());
                ps.setString(5, clan.tag().toLowerCase(Locale.ROOT));
                ps.setString(6, clan.leader().toString());
                ps.setLong(7, clan.createdAt());
                ps.setInt(8, clan.level());
                ps.setLong(9, clan.clanXp());
                ps.setDouble(10, clan.balance());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `clan_members` (`uuid`,`clan_id`,`last_name`,`role`,`joined_at`) VALUES (?,?,?,?,?)")) {
                ps.setString(1, clan.leader().toString());
                ps.setInt(2, clan.id());
                ps.setString(3, leaderName);
                ps.setString(4, ClanRole.LEADER.name());
                ps.setLong(5, clan.createdAt());
                ps.executeUpdate();
            }
        }));
    }

    @Override
    public CompletableFuture<Void> deleteClan(int clanId) {
        return run(() -> inTransaction(connection -> {
            execUpdate(connection, "DELETE FROM `clan_members` WHERE `clan_id` = ?", ps -> ps.setInt(1, clanId));
            execUpdate(connection, "DELETE FROM `clan_relations` WHERE `clan_id` = ? OR `other_clan_id` = ?", ps -> {
                ps.setInt(1, clanId);
                ps.setInt(2, clanId);
            });
            execUpdate(connection, "DELETE FROM `clan_stats` WHERE `clan_id` = ?", ps -> ps.setInt(1, clanId));
            execUpdate(connection, "DELETE FROM `clans` WHERE `id` = ?", ps -> ps.setInt(1, clanId));
        }));
    }

    @Override
    public CompletableFuture<Void> saveClanMeta(int clanId, String name, String tag, UUID leader) {
        return run(() -> withConnection(connection ->
                execUpdate(connection,
                        "UPDATE `clans` SET `name`=?,`name_lower`=?,`tag`=?,`tag_lower`=?,`leader_uuid`=? WHERE `id`=?",
                        ps -> {
                            ps.setString(1, name);
                            ps.setString(2, name.toLowerCase(Locale.ROOT));
                            ps.setString(3, tag);
                            ps.setString(4, tag.toLowerCase(Locale.ROOT));
                            ps.setString(5, leader.toString());
                            ps.setInt(6, clanId);
                        })));
    }

    @Override
    public CompletableFuture<Void> saveClanProgress(int clanId, int level, long clanXp) {
        return run(() -> withConnection(connection ->
                execUpdate(connection, "UPDATE `clans` SET `level`=?,`clan_xp`=? WHERE `id`=?", ps -> {
                    ps.setInt(1, level);
                    ps.setLong(2, clanXp);
                    ps.setInt(3, clanId);
                })));
    }

    @Override
    public CompletableFuture<Void> saveClanBalance(int clanId, double balance) {
        return run(() -> withConnection(connection ->
                execUpdate(connection, "UPDATE `clans` SET `balance`=? WHERE `id`=?", ps -> {
                    ps.setDouble(1, balance);
                    ps.setInt(2, clanId);
                })));
    }

    @Override
    public CompletableFuture<Void> addMember(int clanId, UUID uuid, String name, ClanRole role, long joinedAt) {
        return run(() -> withConnection(connection ->
                execUpdate(connection,
                        "INSERT INTO `clan_members` (`uuid`,`clan_id`,`last_name`,`role`,`joined_at`) VALUES (?,?,?,?,?)",
                        ps -> {
                            ps.setString(1, uuid.toString());
                            ps.setInt(2, clanId);
                            ps.setString(3, name);
                            ps.setString(4, role.name());
                            ps.setLong(5, joinedAt);
                        })));
    }

    @Override
    public CompletableFuture<Void> removeMember(UUID uuid) {
        return run(() -> withConnection(connection ->
                execUpdate(connection, "DELETE FROM `clan_members` WHERE `uuid`=?",
                        ps -> ps.setString(1, uuid.toString()))));
    }

    @Override
    public CompletableFuture<Void> updateMemberName(UUID uuid, String name) {
        return run(() -> withConnection(connection ->
                execUpdate(connection, "UPDATE `clan_members` SET `last_name`=? WHERE `uuid`=?", ps -> {
                    ps.setString(1, name);
                    ps.setString(2, uuid.toString());
                })));
    }

    @Override
    public CompletableFuture<Void> setMemberRole(UUID uuid, ClanRole role) {
        return run(() -> withConnection(connection ->
                execUpdate(connection, "UPDATE `clan_members` SET `role`=? WHERE `uuid`=?", ps -> {
                    ps.setString(1, role.name());
                    ps.setString(2, uuid.toString());
                })));
    }

    @Override
    public CompletableFuture<Void> setRelation(int clanId, int otherClanId, RelationType type, RelationStatus status) {
        return run(() -> inTransaction(connection -> {
            execUpdate(connection, "DELETE FROM `clan_relations` WHERE `clan_id`=? AND `other_clan_id`=?", ps -> {
                ps.setInt(1, clanId);
                ps.setInt(2, otherClanId);
            });
            execUpdate(connection,
                    "INSERT INTO `clan_relations` (`clan_id`,`other_clan_id`,`type`,`status`) VALUES (?,?,?,?)",
                    ps -> {
                        ps.setInt(1, clanId);
                        ps.setInt(2, otherClanId);
                        ps.setString(3, type.name());
                        ps.setString(4, status.name());
                    });
        }));
    }

    @Override
    public CompletableFuture<Void> removeRelation(int clanId, int otherClanId) {
        return run(() -> withConnection(connection ->
                execUpdate(connection, "DELETE FROM `clan_relations` WHERE `clan_id`=? AND `other_clan_id`=?", ps -> {
                    ps.setInt(1, clanId);
                    ps.setInt(2, otherClanId);
                })));
    }

    @Override
    public CompletableFuture<Void> incrementStat(int clanId, String bossType, int by) {
        return run(() -> withConnection(connection -> {
            int updated = execUpdate(connection,
                    "UPDATE `clan_stats` SET `kills`=`kills`+? WHERE `clan_id`=? AND `boss_type`=?", ps -> {
                        ps.setInt(1, by);
                        ps.setInt(2, clanId);
                        ps.setString(3, bossType);
                    });
            if (updated == 0) {
                execUpdate(connection,
                        "INSERT INTO `clan_stats` (`clan_id`,`boss_type`,`kills`) VALUES (?,?,?)", ps -> {
                            ps.setInt(1, clanId);
                            ps.setString(2, bossType);
                            ps.setInt(3, by);
                        });
            }
        }));
    }

    @Override
    public CompletableFuture<Void> addPendingXp(UUID uuid, int amount) {
        return run(() -> withConnection(connection -> {
            int updated = execUpdate(connection,
                    "UPDATE `pending_xp` SET `amount`=`amount`+? WHERE `uuid`=?", ps -> {
                        ps.setInt(1, amount);
                        ps.setString(2, uuid.toString());
                    });
            if (updated == 0) {
                execUpdate(connection,
                        "INSERT INTO `pending_xp` (`uuid`,`amount`) VALUES (?,?)", ps -> {
                            ps.setString(1, uuid.toString());
                            ps.setInt(2, amount);
                        });
            }
        }));
    }

    @Override
    public CompletableFuture<Integer> takePendingXp(UUID uuid) {
        return supply(() -> {
            try (Connection connection = dataSource.getConnection()) {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    int amount = 0;
                    try (PreparedStatement ps = connection.prepareStatement(
                            "SELECT `amount` FROM `pending_xp` WHERE `uuid`=?")) {
                        ps.setString(1, uuid.toString());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                amount = rs.getInt(1);
                            }
                        }
                    }
                    if (amount != 0) {
                        try (PreparedStatement ps = connection.prepareStatement(
                                "DELETE FROM `pending_xp` WHERE `uuid`=?")) {
                            ps.setString(1, uuid.toString());
                            ps.executeUpdate();
                        }
                    }
                    connection.commit();
                    return amount;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            }
        });
    }

    // ---- infrastructure -----------------------------------------------------

    private static ClanRole parseRole(String raw) {
        try {
            return ClanRole.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return ClanRole.MEMBER;
        }
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private void withConnection(SqlConsumer consumer) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            consumer.accept(connection);
        }
    }

    private void inTransaction(SqlConsumer consumer) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                consumer.accept(connection);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    private int execUpdate(Connection connection, String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    private CompletableFuture<Void> run(SqlRunnable action) {
        return CompletableFuture.runAsync(() -> {
            try {
                action.run();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Storage operation failed", ex);
                throw new CompletionException(ex);
            }
        }, executor);
    }

    private <T> CompletableFuture<T> supply(SqlFunctionNoArg<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return action.get();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Storage query failed", ex);
                throw new CompletionException(ex);
            }
        }, executor);
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlFunctionNoArg<T> {
        T get() throws SQLException;
    }
}
