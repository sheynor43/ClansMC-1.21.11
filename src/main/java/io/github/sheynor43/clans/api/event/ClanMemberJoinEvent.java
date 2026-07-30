package io.github.sheynor43.clans.api.event;

import io.github.sheynor43.clans.model.Clan;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Fired after a player joins a clan. */
public class ClanMemberJoinEvent extends ClanEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;

    public ClanMemberJoinEvent(Clan clan, UUID playerId) {
        super(clan);
        this.playerId = playerId;
    }

    /** @return the player who joined. */
    public UUID getPlayerId() {
        return playerId;
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
