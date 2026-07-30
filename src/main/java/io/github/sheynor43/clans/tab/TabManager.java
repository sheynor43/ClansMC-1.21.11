package io.github.sheynor43.clans.tab;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.config.TabMode;
import io.github.sheynor43.clans.hook.AuthMeHook;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.service.ClanManager;
import io.github.sheynor43.clans.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Renders the clan tag into the tab list (and only the tab list). In
 * {@code INTERNAL} mode it sets {@link Player#playerListName(Component)}; in
 * {@code PLACEHOLDER_ONLY} mode it never touches the tab, leaving that to an
 * external tab plugin via PlaceholderAPI. AuthMe is respected before applying.
 */
public final class TabManager {

    private final Plugin plugin;
    private final Supplier<Settings> settings;
    private final ClanManager clans;
    private final AuthMeHook authMe;

    public TabManager(Plugin plugin, Supplier<Settings> settings, ClanManager clans, AuthMeHook authMe) {
        this.plugin = plugin;
        this.settings = settings;
        this.clans = clans;
        this.authMe = authMe;
    }

    /** Applies (or resets) a player's tab name immediately. */
    public void apply(Player player) {
        if (settings.get().tabMode() != TabMode.INTERNAL) {
            return;
        }
        Optional<Clan> clan = clans.getClanOf(player.getUniqueId());
        if (clan.isEmpty()) {
            player.playerListName(null);
            return;
        }
        player.playerListName(render(player, clan.get()));
    }

    private Component render(Player player, Clan clan) {
        Settings s = settings.get();
        Component tagColored = Text.parse(s.tagColor() + Text.escape(clan.tag()));
        return Text.parse(s.tabFormat(),
                Placeholder.unparsed("player_name", player.getName()),
                Placeholder.unparsed("clan_tag", clan.tag()),
                Placeholder.unparsed("clan_name", clan.name()),
                Placeholder.component("clan_tag_colored", tagColored));
    }

    /** Called on join: waits for AuthMe login (or a fixed delay) before applying. */
    public void applyOnJoin(Player player) {
        if (settings.get().tabMode() != TabMode.INTERNAL) {
            return;
        }
        if (authMe.isPresent()) {
            new BukkitRunnable() {
                private int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || ticks > 1200) {
                        cancel();
                        return;
                    }
                    ticks += 10;
                    if (authMe.isAuthenticated(player)) {
                        apply(player);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 10L, 10L);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    apply(player);
                }
            }, Math.max(0L, settings.get().tabApplyDelayTicks()));
        }
    }

    /** Re-applies names for every online player (reload / periodic refresh). */
    public void applyAll() {
        if (settings.get().tabMode() != TabMode.INTERNAL) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }
}
