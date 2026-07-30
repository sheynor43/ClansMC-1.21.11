package io.github.sheynor43.clans.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Splits a boss XP pool equally between the clan members who dealt damage
 * (variant A). Any remainder from integer division goes to the top damager.
 * Pure logic, unit-tested.
 */
public final class BossXpSplitter {

    private BossXpSplitter() {
    }

    /**
     * @param totalXp      the XP pool to distribute (must be {@code >= 0})
     * @param contributors the clan members who dealt damage
     * @param topDamager   the member who dealt the most damage; receives the remainder
     * @return an ordered map of member to XP amount (never {@code null})
     */
    public static Map<UUID, Integer> split(int totalXp, List<UUID> contributors, UUID topDamager) {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        int count = contributors.size();
        if (count == 0 || totalXp <= 0) {
            return result;
        }
        int base = totalXp / count;
        int remainder = totalXp % count;
        for (UUID uuid : contributors) {
            int amount = base;
            if (uuid.equals(topDamager)) {
                amount += remainder;
            }
            result.merge(uuid, amount, Integer::sum);
        }
        // If the declared top damager was not in the list, hand the remainder to the first.
        if (remainder > 0 && !contributors.contains(topDamager)) {
            result.merge(contributors.get(0), remainder, Integer::sum);
        }
        return result;
    }
}
