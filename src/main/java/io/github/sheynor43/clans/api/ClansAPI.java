package io.github.sheynor43.clans.api;

import io.github.sheynor43.clans.model.Clan;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Stable, read-oriented public API for third-party add-ons.
 *
 * <p>Obtain an instance from the Bukkit services manager:
 * <pre>{@code
 * ClansAPI api = Bukkit.getServicesManager().load(ClansAPI.class);
 * }</pre>
 *
 * <p>All methods are safe to call from the main server thread. Returned
 * {@link Clan} objects are live cache instances and must be treated as
 * read-only by consumers.
 */
public interface ClansAPI {

    /** @return the clan with the given internal id, if present. */
    Optional<Clan> getClanById(int id);

    /** @return the clan with the given name (case-insensitive), if present. */
    Optional<Clan> getClanByName(String name);

    /** @return the clan with the given tag (case-insensitive), if present. */
    Optional<Clan> getClanByTag(String tag);

    /** @return the clan the given player belongs to, if any. */
    Optional<Clan> getClanOf(UUID playerId);

    /** @return {@code true} if the player is a member of any clan. */
    boolean isInClan(UUID playerId);

    /** @return an unmodifiable snapshot of every loaded clan. */
    Collection<Clan> getClans();
}
