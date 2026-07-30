package io.github.sheynor43.clans.config;

import io.github.sheynor43.clans.logic.LevelTable;
import io.github.sheynor43.clans.util.Cuboid;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Typed, immutable snapshot of {@code config.yml}. Rebuilt on every reload so the
 * rest of the plugin can read configuration without touching Bukkit's config API.
 */
public final class Settings {

    private final String language;
    private final boolean perPlayerLocale;
    private final boolean debug;

    private final StorageType storageType;
    private final String sqliteFile;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUser;
    private final String mysqlPassword;
    private final int mysqlPoolSize;
    private final Map<String, String> mysqlProperties;

    private final int nameMin;
    private final int nameMax;
    private final List<String> nameBlacklist;
    private final int tagMin;
    private final int tagMax;
    private final String tagColor;
    private final String createPermission;
    private final int dialogTimeoutSeconds;

    private final FriendlyFireMode friendlyFireMode;
    private final double friendlyFireMinHealth;
    private final boolean protectPets;
    private final boolean includeAllies;
    private final List<Cuboid> friendlyFireRegions;

    private final boolean bossXpEnabled;
    private final boolean holdForOffline;
    private final long damageTimeoutSeconds;
    private final boolean dragonEnabled;
    private final int dragonSuppressTicks;
    private final double dragonSuppressRadius;
    private final boolean witherEnabled;

    private final TabMode tabMode;
    private final String tabFormat;
    private final int tabUpdateInterval;
    private final int tabApplyDelayTicks;

    private final boolean chatLogToConsole;
    private final boolean notifyJoinQuit;

    private final long allyRequestTimeoutSeconds;
    private final long inviteExpireSeconds;
    private final long inviteAntiSpamSeconds;

    private final boolean levelsEnabled;
    private final int witherClanXp;
    private final int dragonClanXp;
    private final LevelTable levelTable;

    private final boolean bankEnabled;
    private final boolean glowEnabled;
    private final int maxMembers;

    public Settings(FileConfiguration c) {
        this.language = c.getString("language", "en");
        this.perPlayerLocale = c.getBoolean("per-player-locale", false);
        this.debug = c.getBoolean("debug", false);

        this.storageType = enumOf(StorageType.class, c.getString("storage.type", "MYSQL"), StorageType.MYSQL);
        this.sqliteFile = c.getString("storage.sqlite.file", "clans.db");
        this.mysqlHost = c.getString("storage.mysql.host", "127.0.0.1");
        this.mysqlPort = c.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = c.getString("storage.mysql.database", "clans");
        this.mysqlUser = c.getString("storage.mysql.user", "root");
        this.mysqlPassword = c.getString("storage.mysql.password", "");
        this.mysqlPoolSize = c.getInt("storage.mysql.pool-size", 10);
        this.mysqlProperties = readStringMap(c.getConfigurationSection("storage.mysql.properties"));

        this.nameMin = c.getInt("clan.name.min-length", 3);
        this.nameMax = c.getInt("clan.name.max-length", 16);
        this.nameBlacklist = c.getStringList("clan.name.blacklist");
        this.tagMin = c.getInt("clan.tag.min-length", 2);
        this.tagMax = c.getInt("clan.tag.max-length", 5);
        this.tagColor = c.getString("clan.tag.color", "<aqua>");
        this.createPermission = c.getString("clan.create-permission", "clans.create");
        this.dialogTimeoutSeconds = c.getInt("clan.dialog.timeout-seconds", 60);

        this.friendlyFireMode = enumOf(FriendlyFireMode.class, c.getString("friendly-fire.mode", "CAP"), FriendlyFireMode.CAP);
        this.friendlyFireMinHealth = c.getDouble("friendly-fire.min-health", 0.5);
        this.protectPets = c.getBoolean("friendly-fire.protect-pets", true);
        this.includeAllies = c.getBoolean("friendly-fire.include-allies", false);
        this.friendlyFireRegions = readRegions(c.getMapList("friendly-fire.regions"));

        this.bossXpEnabled = c.getBoolean("boss-xp.enabled", true);
        this.holdForOffline = c.getBoolean("boss-xp.hold-for-offline", true);
        this.damageTimeoutSeconds = c.getLong("boss-xp.damage-timeout-seconds", 600);
        this.dragonEnabled = c.getBoolean("boss-xp.dragon.enabled", true);
        this.dragonSuppressTicks = c.getInt("boss-xp.dragon.suppress-orbs-ticks", 400);
        this.dragonSuppressRadius = c.getDouble("boss-xp.dragon.suppress-radius", 24);
        this.witherEnabled = c.getBoolean("boss-xp.wither.enabled", true);

        this.tabMode = enumOf(TabMode.class, c.getString("tab.mode", "INTERNAL"), TabMode.INTERNAL);
        this.tabFormat = c.getString("tab.format", "<player_name> <gray>[<clan_tag_colored><gray>]");
        this.tabUpdateInterval = c.getInt("tab.update-interval", 200);
        this.tabApplyDelayTicks = c.getInt("tab.apply-delay-ticks", 20);

        this.chatLogToConsole = c.getBoolean("clan-chat.log-to-console", true);
        this.notifyJoinQuit = c.getBoolean("notifications.join-quit", false);

        this.allyRequestTimeoutSeconds = c.getLong("relations.ally-request-timeout-seconds", 120);
        this.inviteExpireSeconds = c.getLong("invite.expire-seconds", 120);
        this.inviteAntiSpamSeconds = c.getLong("invite.antispam-seconds", 30);

        this.levelsEnabled = c.getBoolean("levels.enabled", true);
        this.witherClanXp = c.getInt("levels.xp-rewards.wither", 100);
        this.dragonClanXp = c.getInt("levels.xp-rewards.dragon", 500);
        this.levelTable = readLevels(c.getConfigurationSection("levels.levels"));

        this.bankEnabled = c.getBoolean("bank.enabled", false);
        this.glowEnabled = c.getBoolean("glow.enabled", false);
        this.maxMembers = c.getInt("limits.max-members", -1);
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static Map<String, String> readStringMap(ConfigurationSection section) {
        Map<String, String> map = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, String.valueOf(section.get(key)));
            }
        }
        return map;
    }

    private static List<Cuboid> readRegions(List<Map<?, ?>> raw) {
        List<Cuboid> regions = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            Object world = entry.get("world");
            Object c1 = entry.get("corner1");
            Object c2 = entry.get("corner2");
            if (world == null || !(c1 instanceof Map) || !(c2 instanceof Map)) {
                continue;
            }
            Map<?, ?> m1 = (Map<?, ?>) c1;
            Map<?, ?> m2 = (Map<?, ?>) c2;
            regions.add(new Cuboid(
                    String.valueOf(world),
                    intOf(m1.get("x")), intOf(m1.get("y")), intOf(m1.get("z")),
                    intOf(m2.get("x")), intOf(m2.get("y")), intOf(m2.get("z"))));
        }
        return regions;
    }

    private static int intOf(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static LevelTable readLevels(ConfigurationSection section) {
        List<LevelTable.LevelData> levels = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection lvl = section.getConfigurationSection(key);
                if (lvl == null) {
                    continue;
                }
                int level;
                try {
                    level = Integer.parseInt(key);
                } catch (NumberFormatException ex) {
                    continue;
                }
                levels.add(new LevelTable.LevelData(
                        level,
                        lvl.getLong("xp", 0L),
                        lvl.getInt("max-allies", 0),
                        lvl.getDouble("bank-capacity", 0.0),
                        lvl.getBoolean("glow-unlocked", false)));
            }
        }
        return new LevelTable(levels);
    }

    public String language() { return language; }
    public boolean perPlayerLocale() { return perPlayerLocale; }
    public boolean debug() { return debug; }

    public StorageType storageType() { return storageType; }
    public String sqliteFile() { return sqliteFile; }
    public String mysqlHost() { return mysqlHost; }
    public int mysqlPort() { return mysqlPort; }
    public String mysqlDatabase() { return mysqlDatabase; }
    public String mysqlUser() { return mysqlUser; }
    public String mysqlPassword() { return mysqlPassword; }
    public int mysqlPoolSize() { return mysqlPoolSize; }
    public Map<String, String> mysqlProperties() { return mysqlProperties; }

    public int nameMin() { return nameMin; }
    public int nameMax() { return nameMax; }
    public List<String> nameBlacklist() { return nameBlacklist; }
    public int tagMin() { return tagMin; }
    public int tagMax() { return tagMax; }
    public String tagColor() { return tagColor; }
    public String createPermission() { return createPermission; }
    public int dialogTimeoutSeconds() { return dialogTimeoutSeconds; }

    public FriendlyFireMode friendlyFireMode() { return friendlyFireMode; }
    public double friendlyFireMinHealth() { return friendlyFireMinHealth; }
    public boolean protectPets() { return protectPets; }
    public boolean includeAllies() { return includeAllies; }
    public List<Cuboid> friendlyFireRegions() { return friendlyFireRegions; }

    public boolean bossXpEnabled() { return bossXpEnabled; }
    public boolean holdForOffline() { return holdForOffline; }
    public long damageTimeoutSeconds() { return damageTimeoutSeconds; }
    public boolean dragonEnabled() { return dragonEnabled; }
    public int dragonSuppressTicks() { return dragonSuppressTicks; }
    public double dragonSuppressRadius() { return dragonSuppressRadius; }
    public boolean witherEnabled() { return witherEnabled; }

    public TabMode tabMode() { return tabMode; }
    public String tabFormat() { return tabFormat; }
    public int tabUpdateInterval() { return tabUpdateInterval; }
    public int tabApplyDelayTicks() { return tabApplyDelayTicks; }

    public boolean chatLogToConsole() { return chatLogToConsole; }
    public boolean notifyJoinQuit() { return notifyJoinQuit; }

    public long allyRequestTimeoutSeconds() { return allyRequestTimeoutSeconds; }
    public long inviteExpireSeconds() { return inviteExpireSeconds; }
    public long inviteAntiSpamSeconds() { return inviteAntiSpamSeconds; }

    public boolean levelsEnabled() { return levelsEnabled; }
    public int witherClanXp() { return witherClanXp; }
    public int dragonClanXp() { return dragonClanXp; }
    public LevelTable levelTable() { return levelTable; }

    public boolean bankEnabled() { return bankEnabled; }
    public boolean glowEnabled() { return glowEnabled; }
    public int maxMembers() { return maxMembers; }
}
