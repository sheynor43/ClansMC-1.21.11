package io.github.sheynor43.clans.config;

/** Friendly-fire handling strategy. */
public enum FriendlyFireMode {
    /** Clan mates can hit but never kill each other (damage is reduced). */
    CAP,
    /** All direct clan-mate damage is cancelled. */
    CANCEL,
    /** No friendly-fire handling. */
    OFF
}
