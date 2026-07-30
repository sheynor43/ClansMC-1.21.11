package io.github.sheynor43.clans.logic;

/**
 * Computes the reduced <em>base</em> damage that keeps a victim at or above a
 * minimum health, for the friendly-fire CAP mode.
 *
 * <p>The event is never cancelled (knockback, sound and particles are kept);
 * instead the base damage is scaled by {@code allowed / finalDamage}, where
 * {@code finalDamage} already accounts for armour, enchantments and resistances.
 * Applying the returned value via {@code EntityDamageEvent#setDamage(base)} makes
 * the engine re-run its reductions and land the victim at exactly the minimum.
 *
 * <p>Pure logic, unit-tested.
 */
public final class DamageCapCalculator {

    private DamageCapCalculator() {
    }

    /**
     * @param currentHealth the victim's current health
     * @param minHealth     the minimum health the victim must keep
     * @param baseDamage    the raw event damage before mitigation
     * @param finalDamage   the damage after mitigation (armour/resist/etc.)
     * @return the base damage to apply so the victim keeps at least {@code minHealth}
     */
    public static double cappedBaseDamage(double currentHealth, double minHealth,
                                          double baseDamage, double finalDamage) {
        double allowedFinal = Math.max(0.0, currentHealth - minHealth);
        // The hit would not drop the victim below the floor: leave it untouched.
        if (finalDamage <= allowedFinal) {
            return baseDamage;
        }
        // Nothing meaningful to scale, or victim already at/below the floor.
        if (finalDamage <= 0.0 || baseDamage <= 0.0 || allowedFinal <= 0.0) {
            return 0.0;
        }
        double factor = allowedFinal / finalDamage;
        return baseDamage * factor;
    }
}
