package io.github.sheynor43.clans.chat;

import io.github.sheynor43.clans.dialog.CreateDialogManager;
import io.github.sheynor43.clans.lang.LangManager;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.service.ClanManager;
import io.github.sheynor43.clans.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Captures chat at {@link EventPriority#LOWEST} for two purposes: feeding the
 * clan-creation dialog and routing clan-chat messages. Cancelling here — before
 * other chat plugins such as FlectonePulse process the message — keeps the text
 * out of global chat. Handling hops to the main thread via the scheduler.
 */
public final class ChatListener implements Listener {

    private final Plugin plugin;
    private final CreateDialogManager dialog;
    private final ClanChatService clanChat;
    private final ClanManager clans;
    private final LangManager lang;

    public ChatListener(Plugin plugin, CreateDialogManager dialog, ClanChatService clanChat,
                        ClanManager clans, LangManager lang) {
        this.plugin = plugin;
        this.dialog = dialog;
        this.clanChat = clanChat;
        this.clans = clans;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        boolean inDialog = dialog.isActive(id);
        boolean inClanChat = clanChat.isToggled(id);
        if (!inDialog && !inClanChat) {
            return;
        }

        event.setCancelled(true);
        String message = Text.plain(event.message());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (dialog.isActive(id)) {
                dialog.handleInput(player, message);
                return;
            }
            if (clanChat.isToggled(id)) {
                Optional<Clan> clan = clans.getClanOf(id);
                if (clan.isPresent()) {
                    clanChat.send(player, clan.get(), message);
                } else {
                    clanChat.clear(id);
                    lang.send(player, lang.localeFor(player), "clan.not-in-clan");
                }
            }
        });
    }
}
