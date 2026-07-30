package io.github.sheynor43.clans;

import io.github.sheynor43.clans.logic.DamageCapCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageCapCalculatorTest {

    private static final double EPS = 1.0e-9;

    @Test
    void nonLethalHitIsUntouched() {
        // Victim at 20 HP, floor 0.5, unmitigated 4 damage -> survives, no scaling.
        double base = DamageCapCalculator.cappedBaseDamage(20.0, 0.5, 4.0, 4.0);
        assertEquals(4.0, base, EPS);
    }

    @Test
    void lethalHitIsScaledToLeaveMinHealth() {
        // Victim at 6 HP, floor 0.5 -> allowed final = 5.5. Final would be 10 (base 12 with armour).
        // factor = 5.5 / 10 = 0.55 -> base 12 * 0.55 = 6.6
        double base = DamageCapCalculator.cappedBaseDamage(6.0, 0.5, 12.0, 10.0);
        assertEquals(6.6, base, EPS);
    }

    @Test
    void victimAlreadyAtFloorTakesZero() {
        double base = DamageCapCalculator.cappedBaseDamage(0.5, 0.5, 8.0, 8.0);
        assertEquals(0.0, base, EPS);
    }

    @Test
    void victimBelowFloorTakesZero() {
        double base = DamageCapCalculator.cappedBaseDamage(0.25, 0.5, 8.0, 8.0);
        assertEquals(0.0, base, EPS);
    }

    @Test
    void scaledResultNeverExceedsOriginalBase() {
        double base = DamageCapCalculator.cappedBaseDamage(3.0, 0.5, 20.0, 15.0);
        assertTrue(base <= 20.0);
        assertTrue(base >= 0.0);
    }
}
