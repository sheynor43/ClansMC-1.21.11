package io.github.sheynor43.clans.api.event;

import io.github.sheynor43.clans.model.Clan;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired just before a clan is removed from the cache and storage. */
public class ClanDisbandEvent extends ClanEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public ClanDisbandEvent(Clan clan) {
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
