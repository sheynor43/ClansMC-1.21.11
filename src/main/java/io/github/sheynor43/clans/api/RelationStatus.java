package io.github.sheynor43.clans.api;

/**
 * The status of a relation. Alliances require mutual confirmation and therefore
 * pass through {@link #PENDING} before becoming {@link #ACTIVE}. Enmity is always
 * declared unilaterally and is stored directly as {@link #ACTIVE}.
 */
public enum RelationStatus {
    PENDING,
    ACTIVE
}
