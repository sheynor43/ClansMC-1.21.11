package io.github.sheynor43.clans.chat;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.lang.LangManager;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.service.ClanBroadcaster;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Clan-chat toggle state and message delivery. */
public final class ClanChatService {

    private final Plugin plugin;
    private final Supplier<Settings> settings;
    private final LangManager lang;
    private final ClanBroadcaster broadcaster;

    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();

    public ClanChatService(Plugin plugin, Supplier<Settings> settings, LangManager lang, ClanBroadcaster broadcaster) {
        this.plugin = plugin;
        this.settings = settings;
        this.lang = lang;
        this.broadcaster = broadcaster;
    }

    public boolean isToggled(UUID uuid) {
        return toggled.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        if (toggled.remove(uuid)) {
            return false;
        }
        toggled.add(uuid);
        return true;
    }

    public void clear(UUID uuid) {
        toggled.remove(uuid);
    }

    /** Formats and delivers a clan-chat line to every online member, plus the console if configured. */
    public void send(Player sender, Clan clan, String message) {
        var playerResolver = Placeholder.unparsed("player", sender.getName());
        var messageResolver = Placeholder.unparsed("message", message);
        broadcaster.broadcast(clan, "chat.format", playerResolver, messageResolver);

        if (settings.get().chatLogToConsole()) {
            String line = lang.renderPlain(lang.defaultLocale(), "chat.format", playerResolver, messageResolver);
            plugin.getLogger().info("[ClanChat] [" + clan.tag() + "] " + line);
        }
    }
}
