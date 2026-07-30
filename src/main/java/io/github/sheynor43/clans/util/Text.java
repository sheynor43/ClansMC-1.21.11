package io.github.sheynor43.clans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Small helpers around Adventure MiniMessage. No legacy colour codes anywhere. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    public static MiniMessage mm() {
        return MM;
    }

    public static Component parse(String input, TagResolver... resolvers) {
        return MM.deserialize(input, resolvers);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Escapes user-provided text so it cannot inject MiniMessage tags. */
    public static String escape(String raw) {
        return MM.escapeTags(raw);
    }
}
