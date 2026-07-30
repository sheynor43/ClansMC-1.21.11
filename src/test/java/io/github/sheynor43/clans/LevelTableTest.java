package io.github.sheynor43.clans;

import io.github.sheynor43.clans.logic.LevelTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelTableTest {

    private final LevelTable table = new LevelTable(List.of(
            new LevelTable.LevelData(1, 0, 1, 10000.0, false),
            new LevelTable.LevelData(2, 500, 2, 50000.0, false),
            new LevelTable.LevelData(3, 1500, 3, 150000.0, true)));

    @Test
    void levelForXpUsesFloor() {
        assertEquals(1, table.levelForXp(0));
        assertEquals(1, table.levelForXp(499));
        assertEquals(2, table.levelForXp(500));
        assertEquals(2, table.levelForXp(1499));
        assertEquals(3, table.levelForXp(1500));
        assertEquals(3, table.levelForXp(99999));
    }

    @Test
    void perksResolveByLevel() {
        assertFalse(table.dataForLevel(2).glowUnlocked());
        assertTrue(table.dataForLevel(3).glowUnlocked());
        assertEquals(3, table.dataForLevel(3).maxAllies());
    }

    @Test
    void xpForNextReportsThresholdOrEmptyAtMax() {
        assertEquals(OptionalLong.of(500), table.xpForNext(0));
        assertEquals(OptionalLong.of(1500), table.xpForNext(500));
        assertTrue(table.xpForNext(1500).isEmpty());
        assertTrue(table.xpForNext(5000).isEmpty());
    }

    @Test
    void emptyTableGetsSafeDefault() {
        LevelTable empty = new LevelTable(List.of());
        assertEquals(1, empty.levelForXp(0));
        assertEquals(1, empty.maxLevel());
    }
}
