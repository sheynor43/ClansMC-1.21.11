package io.github.sheynor43.clans;

import io.github.sheynor43.clans.logic.BossXpSplitter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossXpSplitterTest {

    @Test
    void splitsEvenlyWhenDivisible() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Map<UUID, Integer> shares = BossXpSplitter.split(100, List.of(a, b), a);
        assertEquals(50, shares.get(a));
        assertEquals(50, shares.get(b));
    }

    @Test
    void remainderGoesToTopDamager() {
        UUID top = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Map<UUID, Integer> shares = BossXpSplitter.split(100, List.of(top, b, c), top);
        // 100 / 3 = 33 each, remainder 1 to top.
        assertEquals(34, shares.get(top));
        assertEquals(33, shares.get(b));
        assertEquals(33, shares.get(c));
        assertEquals(100, shares.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void emptyContributorsYieldEmptyMap() {
        assertTrue(BossXpSplitter.split(100, List.of(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void zeroPoolYieldsEmptyMap() {
        assertTrue(BossXpSplitter.split(0, List.of(UUID.randomUUID()), UUID.randomUUID()).isEmpty());
    }

    @Test
    void wholePoolIsDistributed() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        Map<UUID, Integer> shares = BossXpSplitter.split(10, List.of(a, b, d), b);
        assertEquals(10, shares.values().stream().mapToInt(Integer::intValue).sum());
    }
}
