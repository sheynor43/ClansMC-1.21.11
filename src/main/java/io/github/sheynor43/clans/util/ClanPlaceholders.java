package io.github.sheynor43.clans.util;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.model.Clan;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Builds the common {@code <clan>}, {@code <tag>}, {@code <tag_colored>} resolvers. */
public final class ClanPlaceholders {

    private ClanPlaceholders() {
    }

    public static TagResolver of(Clan clan, Settings settings) {
        return TagResolver.resolver(
                Placeholder.unparsed("clan", clan.name()),
                Placeholder.unparsed("tag", clan.tag()),
                Placeholder.component("tag_colored", coloredTag(clan.tag(), settings)));
    }

    public static net.kyori.adventure.text.Component coloredTag(String tag, Settings settings) {
        return Text.parse(settings.tagColor() + Text.escape(tag));
    }
}
