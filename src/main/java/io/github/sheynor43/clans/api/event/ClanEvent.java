package io.github.sheynor43.clans.api.event;

import io.github.sheynor43.clans.model.Clan;
import org.bukkit.event.Event;

/**
 * Base class holding the common {@link Clan} reference. Each concrete subclass
 * declares its own {@link org.bukkit.event.HandlerList}, as Bukkit requires.
 */
public abstract class ClanEvent extends Event {

    private final Clan clan;

    protected ClanEvent(Clan clan) {
        this.clan = clan;
    }

    /** @return the clan involved in this event. */
    public Clan getClan() {
        return clan;
    }
}
