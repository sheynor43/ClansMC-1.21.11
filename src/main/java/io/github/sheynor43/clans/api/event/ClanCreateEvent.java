package io.github.sheynor43.clans.api.event;

import io.github.sheynor43.clans.model.Clan;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired after a clan has been created and cached. */
public class ClanCreateEvent extends ClanEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public ClanCreateEvent(Clan clan) {
        super(clan);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
