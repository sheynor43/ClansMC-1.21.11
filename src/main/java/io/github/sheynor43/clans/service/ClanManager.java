package io.github.sheynor43.clans.service;

import io.github.sheynor43.clans.api.ClansAPI;
import io.github.sheynor43.clans.api.RelationStatus;
import io.github.sheynor43.clans.api.RelationType;
import io.github.sheynor43.clans.api.event.ClanCreateEvent;
import io.github.sheynor43.clans.api.event.ClanDisbandEvent;
import io.github.sheynor43.clans.api.event.ClanMemberJoinEvent;
import io.github.sheynor43.clans.api.event.ClanMemberLeaveEvent;
import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.logic.LevelTable;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.model.ClanMember;
import io.github.sheynor43.clans.model.ClanRelation;
import io.github.sheynor43.clans.model.ClanRole;
import io.github.sheynor43.clans.storage.ClanStorage;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory cache and single source of truth for clan operations. Reads are safe
 * from any thread; every mutation runs on the main thread (updating the cache
 * immediately) and is written through to {@link ClanStorage} asynchronously.
 */
public final class ClanManager implements ClansAPI {

    private final ClanStorage storage;
    private final Supplier<Settings> settings;
    private final Logger logger;

    private final ConcurrentHashMap<Integer, Clan> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Clan> byNameLower = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Clan> byTagLower = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Clan> byPlayer = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private volatile boolean ready = false;

    public ClanManager(ClanStorage storage, Supplier<Settings> settings, Logger logger) {
        this.storage = storage;
        this.settings = settings;
        this.logger = logger;
    }

    /** Loads the full cache from storage. Completes on the storage thread. */
    public CompletableFuture<Void> loadAll() {
        return storage.loadAll().thenAccept(clans -> {
            byId.clear();
            byNameLower.clear();
            byTagLower.clear();
            byPlayer.clear();
            int maxId = 0;
            for (Clan clan : clans) {
                index(clan);
                for (ClanMember member : clan.members()) {
                    byPlayer.put(member.uuid(), clan);
                }
                maxId = Math.max(maxId, clan.id());
            }
            idGenerator.set(maxId + 1);
            ready = true;
            logger.info("Loaded " + clans.size() + " clan(s) from storage.");
        });
    }

    public boolean isReady() {
        return ready;
    }

    private void index(Clan clan) {
        byId.put(clan.id(), clan);
        byNameLower.put(clan.name().toLowerCase(Locale.ROOT), clan);
        byTagLower.put(clan.tag().toLowerCase(Locale.ROOT), clan);
    }

    // ---- ClansAPI -----------------------------------------------------------

    @Override
    public Optional<Clan> getClanById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Clan> getClanByName(String name) {
        return name == null ? Optional.empty()
                : Optional.ofNullable(byNameLower.get(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        return tag == null ? Optional.empty()
                : Optional.ofNullable(byTagLower.get(tag.toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<Clan> getClanOf(UUID playerId) {
        return Optional.ofNullable(byPlayer.get(playerId));
    }

    @Override
    public boolean isInClan(UUID playerId) {
        return byPlayer.containsKey(playerId);
    }

    @Override
    public Collection<Clan> getClans() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<Clan> sortedByMembers() {
        return byId.values().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.memberCount(), a.memberCount());
                    return cmp != 0 ? cmp : a.name().compareToIgnoreCase(b.name());
                })
                .toList();
    }

    public boolean isNameTaken(String name) {
        return byNameLower.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public boolean isTagTaken(String tag) {
        return byTagLower.containsKey(tag.toLowerCase(Locale.ROOT));
    }

    // ---- mutations (main thread) -------------------------------------------

    public Clan createClan(String name, String tag, UUID leader, String leaderName) {
        int id = idGenerator.getAndIncrement();
        long now = System.currentTimeMillis();
        Clan clan = new Clan(id, name, tag, leader, now, 1, 0L, 0.0);
        clan.membersMap().put(leader, new ClanMember(leader, leaderName, ClanRole.LEADER, now));
        index(clan);
        byPlayer.put(leader, clan);
        persist(storage.insertClan(clan, leaderName));
        fire(new ClanCreateEvent(clan));
        return clan;
    }

    public void disband(Clan clan) {
        fire(new ClanDisbandEvent(clan));
        for (UUID member : clan.membersMap().keySet()) {
            byPlayer.remove(member);
        }
        byId.remove(clan.id());
        byNameLower.remove(clan.name().toLowerCase(Locale.ROOT));
        byTagLower.remove(clan.tag().toLowerCase(Locale.ROOT));
        // Drop dangling relations other clans held toward this one.
        for (Clan other : byId.values()) {
            other.relationsMap().remove(clan.id());
        }
        persist(storage.deleteClan(clan.id()));
    }

    public void addMember(Clan clan, UUID uuid, String name) {
        clan.membersMap().put(uuid, new ClanMember(uuid, name, ClanRole.MEMBER, System.currentTimeMillis()));
        byPlayer.put(uuid, clan);
        persist(storage.addMember(clan.id(), uuid, name, ClanRole.MEMBER, System.currentTimeMillis()));
        fire(new ClanMemberJoinEvent(clan, uuid));
    }

    public void removeMember(Clan clan, UUID uuid, boolean kicked) {
        clan.membersMap().remove(uuid);
        byPlayer.remove(uuid);
        persist(storage.removeMember(uuid));
        fire(new ClanMemberLeaveEvent(clan, uuid, kicked));
    }

    public void transferLeadership(Clan clan, UUID newLeader) {
        UUID oldLeader = clan.leader();
        ClanMember old = clan.member(oldLeader);
        ClanMember fresh = clan.member(newLeader);
        if (old != null) {
            old.role(ClanRole.MEMBER);
            persist(storage.setMemberRole(oldLeader, ClanRole.MEMBER));
        }
        if (fresh != null) {
            fresh.role(ClanRole.LEADER);
            persist(storage.setMemberRole(newLeader, ClanRole.LEADER));
        }
        clan.leader(newLeader);
        persist(storage.saveClanMeta(clan.id(), clan.name(), clan.tag(), newLeader));
    }

    public void rename(Clan clan, String newName) {
        byNameLower.remove(clan.name().toLowerCase(Locale.ROOT));
        clan.name(newName);
        byNameLower.put(newName.toLowerCase(Locale.ROOT), clan);
        persist(storage.saveClanMeta(clan.id(), clan.name(), clan.tag(), clan.leader()));
    }

    public void setTag(Clan clan, String newTag) {
        byTagLower.remove(clan.tag().toLowerCase(Locale.ROOT));
        clan.tag(newTag);
        byTagLower.put(newTag.toLowerCase(Locale.ROOT), clan);
        persist(storage.saveClanMeta(clan.id(), clan.name(), clan.tag(), clan.leader()));
    }

    public void setRelation(Clan clan, Clan other, RelationType type, RelationStatus status) {
        clan.relationsMap().put(other.id(), new ClanRelation(other.id(), type, status));
        persist(storage.setRelation(clan.id(), other.id(), type, status));
    }

    public void removeRelation(Clan clan, Clan other) {
        clan.relationsMap().remove(other.id());
        persist(storage.removeRelation(clan.id(), other.id()));
    }

    /** Adds clan XP and recomputes the level. @return {@code true} if the clan levelled up. */
    public boolean addClanXp(Clan clan, long amount) {
        int before = clan.level();
        long newXp = Math.max(0L, clan.clanXp() + amount);
        clan.clanXp(newXp);
        LevelTable table = settings.get().levelTable();
        int newLevel = table.levelForXp(newXp);
        clan.level(newLevel);
        persist(storage.saveClanProgress(clan.id(), newLevel, newXp));
        return newLevel > before;
    }

    public void incrementBossStat(Clan clan, String bossType) {
        clan.statsMap().merge(bossType, 1, Integer::sum);
        persist(storage.incrementStat(clan.id(), bossType, 1));
    }

    public void setBalance(Clan clan, double balance) {
        clan.balance(balance);
        persist(storage.saveClanBalance(clan.id(), balance));
    }

    public void updateMemberName(UUID uuid, String name) {
        Clan clan = byPlayer.get(uuid);
        if (clan == null) {
            return;
        }
        ClanMember member = clan.member(uuid);
        if (member != null && !name.equals(member.lastName())) {
            member.lastName(name);
            persist(storage.updateMemberName(uuid, name));
        }
    }

    // ---- perks --------------------------------------------------------------

    public LevelTable.LevelData perksFor(Clan clan) {
        return settings.get().levelTable().dataForLevel(clan.level());
    }

    // ---- helpers ------------------------------------------------------------

    private void persist(CompletableFuture<?> future) {
        future.exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to persist clan change", ex);
            return null;
        });
    }

    private void fire(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
}
