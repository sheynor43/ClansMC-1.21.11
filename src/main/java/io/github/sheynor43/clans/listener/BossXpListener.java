package io.github.sheynor43.clans.listener;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.service.BossXpService;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.function.Supplier;

/**
 * Event glue for {@link BossXpService}: records damage to the Ender Dragon and
 * Wither, distributes their XP to the winning clan on death, and suppresses the
 * dragon's death-animation XP orbs so the whole pool can be shared instead.
 */
public final class BossXpListener implements Listener {

    private final Supplier<Settings> settings;
    private final BossXpService service;

    public BossXpListener(Supplier<Settings> settings, BossXpService service) {
        this.settings = settings;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamaged(EntityDamageByEntityEvent event) {
        if (!settings.get().bossXpEnabled()) {
            return;
        }
        Entity victim = event.getEntity();
        if (!(victim instanceof EnderDragon) && !(victim instanceof Wither)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        service.recordDamage(victim.getUniqueId(), attacker.getUniqueId(), event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBossDeath(EntityDeathEvent event) {
        Settings s = settings.get();
        if (!s.bossXpEnabled()) {
            return;
        }
        Entity dead = event.getEntity();
        if (dead instanceof Wither) {
            if (!s.witherEnabled()) {
                service.forgetBoss(dead.getUniqueId());
                return;
            }
            int dropped = event.getDroppedExp();
            if (service.handleWitherDeath(dead.getUniqueId(), dropped)) {
                event.setDroppedExp(0);
            }
        } else if (dead instanceof EnderDragon) {
            if (!s.dragonEnabled()) {
                service.forgetBoss(dead.getUniqueId());
                return;
            }
            if (service.beginDragonDeath(dead.getUniqueId(), dead.getLocation())) {
                // The dragon spawns its XP via orbs; block the small dropped amount too.
                event.setDroppedExp(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOrbSpawn(EntitySpawnEvent event) {
        if (!settings.get().bossXpEnabled() || !settings.get().dragonEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof ExperienceOrb orb)) {
            return;
        }
        if (!service.hasActiveDragonWindow()) {
            return;
        }
        if (service.captureDragonOrb(orb.getLocation(), orb.getExperience())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof EnderDragon || entity instanceof Wither) {
                service.forgetBoss(entity.getUniqueId());
            }
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
