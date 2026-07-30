package io.github.sheynor43.clans.model;

import io.github.sheynor43.clans.api.RelationStatus;
import io.github.sheynor43.clans.api.RelationType;

/** A relation held by one clan toward another, keyed by the other clan's id. */
public final class ClanRelation {

    private final int otherClanId;
    private final RelationType type;
    private volatile RelationStatus status;

    public ClanRelation(int otherClanId, RelationType type, RelationStatus status) {
        this.otherClanId = otherClanId;
        this.type = type;
        this.status = status;
    }

    public int otherClanId() {
        return otherClanId;
    }

    public RelationType type() {
        return type;
    }

    public RelationStatus status() {
        return status;
    }

    public void status(RelationStatus status) {
        this.status = status;
    }
}
