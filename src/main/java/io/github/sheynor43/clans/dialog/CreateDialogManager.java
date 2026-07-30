package io.github.sheynor43.clans.dialog;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.lang.LangManager;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.service.ClanCreationService;
import io.github.sheynor43.clans.tab.TabManager;
import io.github.sheynor43.clans.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Drives the interactive {@code /clan create} chat dialog: name → tag → confirm.
 * Chat input is captured elsewhere and fed in on the main thread. Times out and
 * cancels on quit. Every message comes from the language files.
 */
public final class CreateDialogManager {

    private enum Step { NAME, TAG, CONFIRM }

    private static final class State {
        Step step = Step.NAME;
        String name;
        String tag;
        BukkitTask timeout;
    }

    private final Plugin plugin;
    private final Supplier<Settings> settings;
    private final LangManager lang;
    private final ClanCreationService creation;
    private final TabManager tab;

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    public CreateDialogManager(Plugin plugin, Supplier<Settings> settings, LangManager lang,
                               ClanCreationService creation, TabManager tab) {
        this.plugin = plugin;
        this.settings = settings;
        this.lang = lang;
        this.creation = creation;
        this.tab = tab;
    }

    public boolean isActive(UUID uuid) {
        return states.containsKey(uuid);
    }

    public void start(Player player) {
        State state = new State();
        states.put(player.getUniqueId(), state);
        String locale = lang.localeFor(player);
        lang.send(player, locale, "create.intro", Placeholder.unparsed("cancel", lang.cancelWord(locale)));
        lang.sendList(player, locale, "create.prompt-name");
        scheduleTimeout(player, state);
    }

    private void scheduleTimeout(Player player, State state) {
        long ticks = Math.max(1L, settings.get().dialogTimeoutSeconds() * 20L);
        state.timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (states.remove(player.getUniqueId()) != null && player.isOnline()) {
                lang.send(player, lang.localeFor(player), "create.timed-out");
            }
        }, ticks);
    }

    /** Handles a captured chat line for a player in the dialog (main thread). */
    public void handleInput(Player player, String rawInput) {
        State state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        String locale = lang.localeFor(player);
        String input = rawInput.strip();
        if (input.equalsIgnoreCase(lang.cancelWord(locale))) {
            cancel(player);
            return;
        }

        switch (state.step) {
            case NAME -> handleName(player, state, locale, input);
            case TAG -> handleTag(player, state, locale, input);
            case CONFIRM -> {
                // Confirmation is driven by the clickable buttons; ignore stray chat.
            }
        }
    }

    private void handleName(Player player, State state, String locale, String input) {
        Settings s = settings.get();
        TagResolver minMax = TagResolver.resolver(
                Placeholder.unparsed("min", String.valueOf(s.nameMin())),
                Placeholder.unparsed("max", String.valueOf(s.nameMax())));
        switch (creation.checkName(input)) {
            case TOO_SHORT, TOO_LONG -> lang.send(player, locale, "create.errors.name-length", minMax);
            case BLACKLISTED -> lang.send(player, locale, "create.errors.name-blacklisted");
            case TAKEN -> lang.send(player, locale, "create.errors.name-taken",
                    Placeholder.unparsed("clan", input.strip()));
            case OK -> {
                state.name = input.strip();
                state.step = Step.TAG;
                lang.sendList(player, locale, "create.prompt-tag",
                        Placeholder.unparsed("min", String.valueOf(s.tagMin())),
                        Placeholder.unparsed("max", String.valueOf(s.tagMax())));
            }
        }
    }

    private void handleTag(Player player, State state, String locale, String input) {
        Settings s = settings.get();
        TagResolver minMax = TagResolver.resolver(
                Placeholder.unparsed("min", String.valueOf(s.tagMin())),
                Placeholder.unparsed("max", String.valueOf(s.tagMax())));
        switch (creation.checkTag(input)) {
            case BAD_FORMAT -> lang.send(player, locale, "create.errors.tag-format");
            case TOO_SHORT, TOO_LONG -> lang.send(player, locale, "create.errors.tag-length", minMax);
            case TAKEN -> lang.send(player, locale, "create.errors.tag-taken",
                    Placeholder.unparsed("tag", input));
            case OK -> {
                state.tag = input;
                state.step = Step.CONFIRM;
                lang.sendList(player, locale, "create.confirm", clanResolvers(state.name, state.tag));
            }
        }
    }

    /** Called by the [Confirm] button. */
    public void confirm(Player player) {
        State state = states.get(player.getUniqueId());
        if (state == null || state.step != Step.CONFIRM) {
            return;
        }
        String locale = lang.localeFor(player);
        Clan clan = creation.create(player.getUniqueId(), player.getName(), state.name, state.tag);
        if (clan == null) {
            lang.send(player, locale, "create.errors.name-taken", Placeholder.unparsed("clan", state.name));
            return;
        }
        finish(player);
        lang.send(player, locale, "clan.created", clanResolvers(clan.name(), clan.tag()));
        tab.apply(player);
    }

    public void cancel(Player player) {
        if (finish(player) && player.isOnline()) {
            lang.send(player, lang.localeFor(player), "create.cancelled");
        }
    }

    public void remove(UUID uuid) {
        State state = states.remove(uuid);
        if (state != null && state.timeout != null) {
            state.timeout.cancel();
        }
    }

    private boolean finish(Player player) {
        State state = states.remove(player.getUniqueId());
        if (state == null) {
            return false;
        }
        if (state.timeout != null) {
            state.timeout.cancel();
        }
        return true;
    }

    private TagResolver clanResolvers(String name, String tag) {
        return TagResolver.resolver(
                Placeholder.unparsed("clan", name),
                Placeholder.unparsed("tag", tag),
                Placeholder.component("tag_colored", Text.parse(settings.get().tagColor() + Text.escape(tag))));
    }
}
