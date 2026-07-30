package io.github.sheynor43.clans.api.event;

import io.github.sheynor43.clans.model.Clan;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Fired after a player leaves or is removed from a clan (not on disband). */
public class ClanMemberLeaveEvent extends ClanEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final boolean kicked;

    public ClanMemberLeaveEvent(Clan clan, UUID playerId, boolean kicked) {
        super(clan);
        this.playerId = playerId;
        this.kicked = kicked;
    }

    /** @return the player who left. */
    public UUID getPlayerId() {
        return playerId;
    }

    /** @return {@code true} if the player was kicked, {@code false} if they left voluntarily. */
    public boolean wasKicked() {
        return kicked;
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
