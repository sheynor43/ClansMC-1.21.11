package io.github.sheynor43.clans.service;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.lang.LangManager;
import io.github.sheynor43.clans.logic.BossXpSplitter;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.storage.ClanStorage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Core boss-XP logic: a per-boss damage journal, clan aggregation, equal split
 * between contributing clan members, and XP delivery (online or stored for
 * offline players). The Ender Dragon's XP is captured from its suppressed orbs;
 * the Wither's from its dropped XP.
 */
public final class BossXpService {

    /** Damage dealt to one boss entity, plus a last-touch timestamp for leak control. */
    private static final class DamageJournal {
        final Map<UUID, Double> perPlayer = new HashMap<>();
        long lastUpdate = System.currentTimeMillis();
    }

    /** A live dragon-death context accumulating suppressed orb XP for the winning clan. */
    public static final class DragonContext {
        final Location location;
        final int winnerClanId;
        final List<UUID> contributors;
        final UUID topDamager;
        int accumulated;

        DragonContext(Location location, int winnerClanId, List<UUID> contributors, UUID topDamager) {
            this.location = location;
            this.winnerClanId = winnerClanId;
            this.contributors = contributors;
            this.topDamager = topDamager;
        }

        Location location() {
            return location;
        }
    }

    private final Plugin plugin;
    private final ClanManager clans;
    private final ClanStorage storage;
    private final LangManager lang;
    private final ClanBroadcaster broadcaster;
    private final java.util.function.Supplier<Settings> settings;

    private final Map<UUID, DamageJournal> journals = new ConcurrentHashMap<>();
    private final List<DragonContext> dragonContexts = new ArrayList<>();

    public BossXpService(Plugin plugin, ClanManager clans, ClanStorage storage, LangManager lang,
                         ClanBroadcaster broadcaster, java.util.function.Supplier<Settings> settings) {
        this.plugin = plugin;
        this.clans = clans;
        this.storage = storage;
        this.lang = lang;
        this.broadcaster = broadcaster;
        this.settings = settings;
    }

    // ---- damage journal -----------------------------------------------------

    public void recordDamage(UUID bossId, UUID playerId, double amount) {
        DamageJournal journal = journals.computeIfAbsent(bossId, k -> new DamageJournal());
        journal.perPlayer.merge(playerId, amount, Double::sum);
        journal.lastUpdate = System.currentTimeMillis();
    }

    public void forgetBoss(UUID bossId) {
        journals.remove(bossId);
    }

    public void purgeStale() {
        long timeout = settings.get().damageTimeoutSeconds() * 1000L;
        long now = System.currentTimeMillis();
        journals.entrySet().removeIf(e -> now - e.getValue().lastUpdate > timeout);
    }

    // ---- aggregation --------------------------------------------------------

    /** Resolves the winning clan and its contributing members, or {@code null} if no clan qualifies. */
    private DragonContext resolveWinner(UUID bossId, Location location) {
        DamageJournal journal = journals.remove(bossId);
        if (journal == null || journal.perPlayer.isEmpty()) {
            return null;
        }

        Map<Integer, Double> clanDamage = new HashMap<>();
        Map<Integer, Map<UUID, Double>> clanContributors = new HashMap<>();
        journal.perPlayer.forEach((playerId, damage) -> clans.getClanOf(playerId).ifPresent(clan -> {
            clanDamage.merge(clan.id(), damage, Double::sum);
            clanContributors.computeIfAbsent(clan.id(), k -> new HashMap<>()).merge(playerId, damage, Double::sum);
        }));
        if (clanDamage.isEmpty()) {
            return null;
        }

        int winnerId = clanDamage.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElseThrow();

        Map<UUID, Double> contributors = clanContributors.get(winnerId);
        List<UUID> ordered = contributors.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        UUID top = ordered.get(0);
        return new DragonContext(location, winnerId, new ArrayList<>(ordered), top);
    }

    // ---- wither -------------------------------------------------------------

    /** Distributes the Wither's dropped XP. @return {@code true} if a clan claimed it. */
    public boolean handleWitherDeath(UUID bossId, int droppedXp) {
        DragonContext ctx = resolveWinner(bossId, null);
        if (ctx == null) {
            return false;
        }
        distribute(ctx.winnerClanId, ctx.contributors, ctx.topDamager, droppedXp, Clan.BOSS_WITHER, "boss.wither-name");
        return true;
    }

    // ---- dragon -------------------------------------------------------------

    /** Begins a dragon suppression window if a clan won. @return {@code true} if suppression should run. */
    public boolean beginDragonDeath(UUID bossId, Location location) {
        DragonContext ctx = resolveWinner(bossId, location.clone());
        if (ctx == null) {
            return false;
        }
        synchronized (dragonContexts) {
            dragonContexts.add(ctx);
        }
        int ticks = Math.max(1, settings.get().dragonSuppressTicks());
        Bukkit.getScheduler().runTaskLater(plugin, () -> finishDragon(ctx), ticks);
        return true;
    }

    /**
     * If the orb falls inside an active dragon window, captures its XP and returns
     * {@code true} so the caller can cancel the orb's spawn.
     */
    public boolean captureDragonOrb(Location orbLocation, int orbXp) {
        double radiusSq = Math.pow(settings.get().dragonSuppressRadius(), 2);
        synchronized (dragonContexts) {
            for (DragonContext ctx : dragonContexts) {
                Location loc = ctx.location();
                if (loc.getWorld() != null && loc.getWorld().equals(orbLocation.getWorld())
                        && loc.distanceSquared(orbLocation) <= radiusSq) {
                    ctx.accumulated += orbXp;
                    return true;
                }
            }
        }
        return false;
    }

    private void finishDragon(DragonContext ctx) {
        synchronized (dragonContexts) {
            dragonContexts.remove(ctx);
        }
        distribute(ctx.winnerClanId, ctx.contributors, ctx.topDamager, ctx.accumulated,
                Clan.BOSS_DRAGON, "boss.dragon-name");
    }

    // ---- shared distribution ------------------------------------------------

    private void distribute(int clanId, List<UUID> contributors, UUID topDamager,
                            int pool, String bossType, String bossNameKey) {
        Clan clan = clans.getClanById(clanId).orElse(null);
        if (clan == null || contributors.isEmpty()) {
            return;
        }

        Map<UUID, Integer> shares = BossXpSplitter.split(pool, contributors, topDamager);
        boolean holdOffline = settings.get().holdForOffline();
        for (Map.Entry<UUID, Integer> entry : shares.entrySet()) {
            int amount = entry.getValue();
            if (amount <= 0) {
                continue;
            }
            Player online = Bukkit.getPlayer(entry.getKey());
            if (online != null && online.isOnline()) {
                online.giveExp(amount, false);
            } else if (holdOffline) {
                storage.addPendingXp(entry.getKey(), amount).exceptionally(ex -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to store offline boss XP", ex);
                    return null;
                });
            }
        }

        int clanXpReward = bossType.equals(Clan.BOSS_DRAGON)
                ? settings.get().dragonClanXp()
                : settings.get().witherClanXp();
        clans.incrementBossStat(clan, bossType);
        if (settings.get().levelsEnabled() && clanXpReward > 0) {
            clans.addClanXp(clan, clanXpReward);
        }

        int perMember = pool / Math.max(1, contributors.size());
        broadcaster.broadcast(clan, "boss.reward",
                Placeholder.parsed("boss", lang.renderPlain(lang.defaultLocale(), bossNameKey)),
                Placeholder.parsed("amount", String.valueOf(perMember)));
        if (settings.get().levelsEnabled() && clanXpReward > 0) {
            broadcaster.broadcast(clan, "boss.reward-clan-xp",
                    Placeholder.parsed("amount", String.valueOf(clanXpReward)));
        }
    }

    /** Cancels any pending dragon windows (plugin disable / reload). */
    public void clear() {
        synchronized (dragonContexts) {
            dragonContexts.clear();
        }
        journals.clear();
    }

    public boolean hasActiveDragonWindow() {
        synchronized (dragonContexts) {
            return !dragonContexts.isEmpty();
        }
    }
}
