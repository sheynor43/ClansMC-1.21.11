package io.github.sheynor43.clans.service;

import io.github.sheynor43.clans.lang.LangManager;
import io.github.sheynor43.clans.model.Clan;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Sends localized messages to all online members of a clan, honouring each
 * player's locale when per-player locale is enabled.
 */
public final class ClanBroadcaster {

    private final LangManager lang;

    public ClanBroadcaster(LangManager lang) {
        this.lang = lang;
    }

    public void broadcast(Clan clan, String key, TagResolver... resolvers) {
        for (var member : clan.members()) {
            Player player = Bukkit.getPlayer(member.uuid());
            if (player != null) {
                lang.send(player, lang.localeFor(player), key, resolvers);
            }
        }
    }
}
