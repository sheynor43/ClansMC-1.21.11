package io.github.sheynor43.clans.storage;

import io.github.sheynor43.clans.api.RelationStatus;
import io.github.sheynor43.clans.api.RelationType;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.model.ClanRole;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous persistence abstraction for clans. Every method returns a
 * {@link CompletableFuture} completed on a background thread; callers must hop
 * back to the main thread before touching Bukkit API. Two implementations exist:
 * SQLite (default-capable) and MySQL/MariaDB, sharing a single schema.
 */
public interface ClanStorage {

    /** Opens the connection pool and applies pending schema migrations. */
    CompletableFuture<Void> init();

    /** Loads every clan and its full object graph into fresh model instances. */
    CompletableFuture<List<Clan>> loadAll();

    /** Persists a brand-new clan together with its leader membership row. */
    CompletableFuture<Void> insertClan(Clan clan, String leaderName);

    CompletableFuture<Void> deleteClan(int clanId);

    CompletableFuture<Void> saveClanMeta(int clanId, String name, String tag, UUID leader);

    CompletableFuture<Void> saveClanProgress(int clanId, int level, long clanXp);

    CompletableFuture<Void> saveClanBalance(int clanId, double balance);

    CompletableFuture<Void> addMember(int clanId, UUID uuid, String name, ClanRole role, long joinedAt);

    CompletableFuture<Void> removeMember(UUID uuid);

    CompletableFuture<Void> updateMemberName(UUID uuid, String name);

    CompletableFuture<Void> setMemberRole(UUID uuid, ClanRole role);

    CompletableFuture<Void> setRelation(int clanId, int otherClanId, RelationType type, RelationStatus status);

    CompletableFuture<Void> removeRelation(int clanId, int otherClanId);

    CompletableFuture<Void> incrementStat(int clanId, String bossType, int by);

    CompletableFuture<Void> addPendingXp(UUID uuid, int amount);

    /** Atomically reads and clears a player's stored offline XP. */
    CompletableFuture<Integer> takePendingXp(UUID uuid);

    /** Closes the connection pool. Blocks the calling thread; call from onDisable. */
    void shutdown();
}
