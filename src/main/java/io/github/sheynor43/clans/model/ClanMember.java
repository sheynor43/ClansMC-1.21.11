package io.github.sheynor43.clans.model;

import java.util.UUID;

/**
 * A single membership record. Players are identified by {@link UUID}; the last
 * known name is stored so offline members can still be displayed.
 */
public final class ClanMember {

    private final UUID uuid;
    private volatile String lastName;
    private volatile ClanRole role;
    private final long joinedAt;

    public ClanMember(UUID uuid, String lastName, ClanRole role, long joinedAt) {
        this.uuid = uuid;
        this.lastName = lastName;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String lastName() {
        return lastName;
    }

    public void lastName(String lastName) {
        this.lastName = lastName;
    }

    public ClanRole role() {
        return role;
    }

    public void role(ClanRole role) {
        this.role = role;
    }

    public long joinedAt() {
        return joinedAt;
    }
}
