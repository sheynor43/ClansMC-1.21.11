package io.github.sheynor43.clans.logic;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.OptionalLong;
import java.util.TreeMap;

/**
 * Immutable table mapping cumulative clan XP to levels and their perks.
 * Pure logic, unit-tested independently of Bukkit.
 */
public final class LevelTable {

    /** A single level definition and its perks. */
    public record LevelData(int level, long xp, int maxAllies, double bankCapacity, boolean glowUnlocked) {
    }

    private final NavigableMap<Long, LevelData> byXp = new TreeMap<>();
    private final NavigableMap<Integer, LevelData> byLevel = new TreeMap<>();

    public LevelTable(Collection<LevelData> levels) {
        for (LevelData data : levels) {
            byXp.put(data.xp(), data);
            byLevel.put(data.level(), data);
        }
        if (byXp.isEmpty()) {
            // Guarantee a sane level 1 so the rest of the code never sees an empty table.
            LevelData fallback = new LevelData(1, 0L, 0, 0.0, false);
            byXp.put(0L, fallback);
            byLevel.put(1, fallback);
        }
    }

    /** @return the level definition whose XP threshold is the highest not exceeding {@code xp}. */
    public LevelData dataForXp(long xp) {
        var entry = byXp.floorEntry(Math.max(0L, xp));
        return entry != null ? entry.getValue() : byXp.firstEntry().getValue();
    }

    /** @return the numeric level reached with the given cumulative XP. */
    public int levelForXp(long xp) {
        return dataForXp(xp).level();
    }

    /** @return the definition for an exact level, or the lowest level if unknown. */
    public LevelData dataForLevel(int level) {
        LevelData data = byLevel.get(level);
        return data != null ? data : byLevel.firstEntry().getValue();
    }

    /** @return the XP threshold of the next level, or empty if already at the maximum. */
    public OptionalLong xpForNext(long xp) {
        var next = byXp.higherEntry(dataForXp(xp).xp());
        return next == null ? OptionalLong.empty() : OptionalLong.of(next.getKey());
    }

    /** @return the highest defined level. */
    public int maxLevel() {
        return byLevel.lastKey();
    }
}
