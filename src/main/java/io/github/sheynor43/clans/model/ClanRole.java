package io.github.sheynor43.clans.model;

/**
 * The role of a player inside a clan. Only two roles exist by design:
 * a single {@link #LEADER} and any number of {@link #MEMBER}s.
 */
public enum ClanRole {
    LEADER,
    MEMBER
}
