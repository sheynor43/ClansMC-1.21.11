package io.github.sheynor43.clans.listener;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.chat.ClanChatService;
import io.github.sheynor43.clans.dialog.CreateDialogManager;
import io.github.sheynor43.clans.glow.GlowManager;
import io.github.sheynor43.clans.service.ClanBroadcaster;
import io.github.sheynor43.clans.service.ClanManager;
import io.github.sheynor43.clans.service.InviteService;
import io.github.sheynor43.clans.storage.ClanStorage;
import io.github.sheynor43.clans.tab.TabManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;
import java.util.logging.Level;

/** Handles join/quit: name refresh, tab & glow application, offline XP delivery and cleanup. */
public final class ConnectionListener implements Listener {

    private final Plugin plugin;
    private final Supplier<Settings> settings;
    private final ClanManager clans;
    private final ClanStorage storage;
    private final TabManager tab;
    private final GlowManager glow;
    private final CreateDialogManager dialog;
    private final ClanChatService clanChat;
    private final InviteService invites;
    private final ClanBroadcaster broadcaster;

    public ConnectionListener(Plugin plugin, Supplier<Settings> settings, ClanManager clans, ClanStorage storage,
                              TabManager tab, GlowManager glow, CreateDialogManager dialog,
                              ClanChatService clanChat, InviteService invites, ClanBroadcaster broadcaster) {
        this.plugin = plugin;
        this.settings = settings;
        this.clans = clans;
        this.storage = storage;
        this.tab = tab;
        this.glow = glow;
        this.dialog = dialog;
        this.clanChat = clanChat;
        this.invites = invites;
        this.broadcaster = broadcaster;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        clans.updateMemberName(player.getUniqueId(), player.getName());
        tab.applyOnJoin(player);
        glow.applyForJoin(player);
        grantPendingXp(player);

        if (settings.get().notifyJoinQuit()) {
            clans.getClanOf(player.getUniqueId()).ifPresent(clan ->
                    broadcaster.broadcast(clan, "clan.join-broadcast", Placeholder.unparsed("player", player.getName())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dialog.remove(player.getUniqueId());
        clanChat.clear(player.getUniqueId());
        invites.clearPlayer(player.getUniqueId());

        if (settings.get().notifyJoinQuit()) {
            clans.getClanOf(player.getUniqueId()).ifPresent(clan ->
                    broadcaster.broadcast(clan, "clan.quit-broadcast", Placeholder.unparsed("player", player.getName())));
        }
    }

    private void grantPendingXp(Player player) {
        storage.takePendingXp(player.getUniqueId()).thenAccept(amount -> {
            if (amount > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player online = Bukkit.getPlayer(player.getUniqueId());
                    if (online != null && online.isOnline()) {
                        online.giveExp(amount, false);
                    }
                });
            }
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "Failed to load pending XP for " + player.getName(), ex);
            return null;
        });
    }
}
