package io.github.sheynor43.clans.model;

import io.github.sheynor43.clans.api.RelationType;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory representation of a clan and its full object graph (members,
 * relations, boss statistics). Instances live in the {@code ClanManager} cache
 * and are mutated only from the main server thread; the concurrent collections
 * additionally make reads safe from asynchronous placeholder lookups.
 */
public final class Clan {

    public static final String BOSS_WITHER = "WITHER";
    public static final String BOSS_DRAGON = "DRAGON";

    private final int id;
    private volatile String name;
    private volatile String tag;
    private volatile UUID leader;
    private final long createdAt;
    private volatile int level;
    private volatile long clanXp;
    private volatile double balance;

    private final Map<UUID, ClanMember> members = new ConcurrentHashMap<>();
    private final Map<Integer, ClanRelation> relations = new ConcurrentHashMap<>();
    private final Map<String, Integer> stats = new ConcurrentHashMap<>();

    public Clan(int id, String name, String tag, UUID leader, long createdAt,
                int level, long clanXp, double balance) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.createdAt = createdAt;
        this.level = level;
        this.clanXp = clanXp;
        this.balance = balance;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public String tag() {
        return tag;
    }

    public void tag(String tag) {
        this.tag = tag;
    }

    public UUID leader() {
        return leader;
    }

    public void leader(UUID leader) {
        this.leader = leader;
    }

    public long createdAt() {
        return createdAt;
    }

    public int level() {
        return level;
    }

    public void level(int level) {
        this.level = level;
    }

    public long clanXp() {
        return clanXp;
    }

    public void clanXp(long clanXp) {
        this.clanXp = clanXp;
    }

    public double balance() {
        return balance;
    }

    public void balance(double balance) {
        this.balance = balance;
    }

    public Map<UUID, ClanMember> membersMap() {
        return members;
    }

    public Collection<ClanMember> members() {
        return members.values();
    }

    public int memberCount() {
        return members.size();
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public ClanMember member(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public Map<Integer, ClanRelation> relationsMap() {
        return relations;
    }

    public Collection<ClanRelation> relations() {
        return relations.values();
    }

    public ClanRelation relation(int otherClanId) {
        return relations.get(otherClanId);
    }

    public boolean hasRelation(int otherClanId, RelationType type) {
        ClanRelation r = relations.get(otherClanId);
        return r != null && r.type() == type;
    }

    public Map<String, Integer> statsMap() {
        return stats;
    }

    public int stat(String bossType) {
        return stats.getOrDefault(bossType, 0);
    }
}
