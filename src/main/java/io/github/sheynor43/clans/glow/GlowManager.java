package io.github.sheynor43.clans.glow;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.service.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Toggles the {@code GLOWING} outline for a clan's online members.
 *
 * <p><strong>Limitation:</strong> the Bukkit API cannot restrict glow visibility
 * to clan mates only, so an enabled clan glow is visible to <em>everyone</em>.
 * A per-viewer implementation would require sending entity metadata packets,
 * which this plugin intentionally does not do. Runtime-only: not persisted.
 */
public final class GlowManager {

    private final Supplier<Settings> settings;
    private final ClanManager clans;
    private final Set<Integer> glowingClans = ConcurrentHashMap.newKeySet();

    public GlowManager(Supplier<Settings> settings, ClanManager clans) {
        this.settings = settings;
        this.clans = clans;
    }

    public boolean isModuleEnabled() {
        return settings.get().glowEnabled();
    }

    public boolean isGlowing(int clanId) {
        return glowingClans.contains(clanId);
    }

    /** Toggles glow for a clan. @return the new state (true = now glowing). */
    public boolean toggle(Clan clan) {
        if (isGlowing(clan.id())) {
            glowingClans.remove(clan.id());
            setGlow(clan, false);
            return false;
        }
        glowingClans.add(clan.id());
        setGlow(clan, true);
        return true;
    }

    /** Applies the current glow state to a single player (e.g. on join). */
    public void applyForJoin(Player player) {
        clans.getClanOf(player.getUniqueId()).ifPresent(clan -> {
            if (isGlowing(clan.id())) {
                player.setGlowing(true);
            }
        });
    }

    private void setGlow(Clan clan, boolean glowing) {
        for (var member : clan.members()) {
            Player player = Bukkit.getPlayer(member.uuid());
            if (player != null) {
                player.setGlowing(glowing);
            }
        }
    }

    /** Clears glow from everyone (plugin disable). */
    public void clearAll() {
        for (int clanId : glowingClans) {
            clans.getClanById(clanId).ifPresent(clan -> setGlow(clan, false));
        }
        glowingClans.clear();
    }
}
