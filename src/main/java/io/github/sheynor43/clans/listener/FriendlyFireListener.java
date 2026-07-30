package io.github.sheynor43.clans.listener;

import io.github.sheynor43.clans.api.RelationType;
import io.github.sheynor43.clans.config.FriendlyFireMode;
import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.logic.DamageCapCalculator;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.service.ClanManager;
import io.github.sheynor43.clans.util.Cuboid;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Implements friendly fire between clan mates. In CAP mode direct damage is
 * reduced (never cancelled) so knockback, sound and particles are preserved,
 * while the victim keeps at least {@code min-health} HP. Indirect sources (TNT,
 * end crystals, lava, fall, void, ...) are deliberately left vanilla and can kill.
 * Clan mates' tamed pets are fully protected. Potions of harming are neutralised
 * through the splash / lingering events, not the damage event.
 */
public final class FriendlyFireListener implements Listener {

    private final Supplier<Settings> settings;
    private final ClanManager clans;

    public FriendlyFireListener(Supplier<Settings> settings, ClanManager clans) {
        this.settings = settings;
        this.clans = clans;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Settings s = settings.get();
        if (s.friendlyFireMode() == FriendlyFireMode.OFF && !s.protectPets()) {
            return;
        }

        Player attacker = resolveDirectAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        Optional<Clan> attackerClan = clans.getClanOf(attacker.getUniqueId());
        if (attackerClan.isEmpty()) {
            return;
        }

        Entity victim = event.getEntity();

        // Pet protection: a clan mate's tamed animal takes no damage from the clan.
        if (s.protectPets() && victim instanceof Tameable tameable && tameable.isTamed()) {
            AnimalTamer owner = tameable.getOwner();
            if (owner != null && sameClan(attackerClan.get(), owner.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (!(victim instanceof Player victimPlayer) || victimPlayer.equals(attacker)) {
            return;
        }
        if (attacker.hasPermission("clans.bypass.friendlyfire")) {
            return;
        }
        if (!isProtectedPair(attackerClan.get(), victimPlayer.getUniqueId(), s)) {
            return;
        }

        // Spawn-style regions force a hard cancel regardless of mode.
        if (inCancelRegion(victimPlayer, s)) {
            event.setCancelled(true);
            return;
        }

        switch (s.friendlyFireMode()) {
            case OFF -> {
                // Only pet protection was requested; player damage is untouched.
            }
            case CANCEL -> event.setCancelled(true);
            case CAP -> {
                double newBase = DamageCapCalculator.cappedBaseDamage(
                        victimPlayer.getHealth(), s.friendlyFireMinHealth(),
                        event.getDamage(), event.getFinalDamage());
                event.setDamage(Math.max(0.0, newBase));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        Settings s = settings.get();
        if (s.friendlyFireMode() == FriendlyFireMode.OFF) {
            return;
        }
        if (!isHarmful(event.getPotion().getEffects())) {
            return;
        }
        Player thrower = shooterAsPlayer(event.getPotion().getShooter());
        if (thrower == null) {
            return;
        }
        Optional<Clan> throwerClan = clans.getClanOf(thrower.getUniqueId());
        if (throwerClan.isEmpty()) {
            return;
        }
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (affected instanceof Player target && !target.equals(thrower)
                    && !target.hasPermission("clans.bypass.friendlyfire")
                    && isProtectedPair(throwerClan.get(), target.getUniqueId(), s)) {
                event.setIntensity(affected, 0.0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAreaEffectCloud(AreaEffectCloudApplyEvent event) {
        Settings s = settings.get();
        if (s.friendlyFireMode() == FriendlyFireMode.OFF) {
            return;
        }
        if (!isHarmful(event.getEntity().getCustomEffects())) {
            return;
        }
        Player source = shooterAsPlayer(event.getEntity().getSource());
        if (source == null) {
            return;
        }
        Optional<Clan> sourceClan = clans.getClanOf(source.getUniqueId());
        if (sourceClan.isEmpty()) {
            return;
        }
        Iterator<LivingEntity> it = event.getAffectedEntities().iterator();
        while (it.hasNext()) {
            LivingEntity affected = it.next();
            if (affected instanceof Player target && !target.equals(source)
                    && !target.hasPermission("clans.bypass.friendlyfire")
                    && isProtectedPair(sourceClan.get(), target.getUniqueId(), s)) {
                it.remove();
            }
        }
    }

    // ---- helpers ------------------------------------------------------------

    private Player resolveDirectAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && isDirectProjectile(projectile)) {
            return shooterAsPlayer(projectile.getShooter());
        }
        return null;
    }

    private boolean isDirectProjectile(Projectile projectile) {
        // Direct: melee-like ranged hits. NOT thrown potions (handled separately)
        // and NOT indirect explosives.
        return projectile instanceof AbstractArrow
                || projectile instanceof Snowball
                || projectile instanceof Egg
                || projectile instanceof Fireball;
    }

    private Player shooterAsPlayer(ProjectileSource shooter) {
        return shooter instanceof Player player ? player : null;
    }

    private boolean isProtectedPair(Clan attackerClan, UUID victim, Settings s) {
        Optional<Clan> victimClan = clans.getClanOf(victim);
        if (victimClan.isEmpty()) {
            return false;
        }
        if (victimClan.get().id() == attackerClan.id()) {
            return true;
        }
        return s.includeAllies() && attackerClan.hasRelation(victimClan.get().id(), RelationType.ALLY);
    }

    private boolean sameClan(Clan attackerClan, UUID other) {
        return clans.getClanOf(other).map(c -> c.id() == attackerClan.id()).orElse(false);
    }

    private boolean inCancelRegion(Player victim, Settings s) {
        for (Cuboid region : s.friendlyFireRegions()) {
            if (region.contains(victim.getLocation())) {
                return true;
            }
        }
        return false;
    }

    private boolean isHarmful(Iterable<PotionEffect> effects) {
        for (PotionEffect effect : effects) {
            PotionEffectType type = effect.getType();
            if (type.equals(PotionEffectType.INSTANT_DAMAGE)
                    || type.equals(PotionEffectType.POISON)
                    || type.equals(PotionEffectType.WITHER)) {
                return true;
            }
        }
        return false;
    }
}
