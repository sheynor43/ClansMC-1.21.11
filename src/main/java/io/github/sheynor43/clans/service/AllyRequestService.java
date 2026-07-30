package io.github.sheynor43.clans.service;

import io.github.sheynor43.clans.config.Settings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Tracks pending alliance offers, which require mutual confirmation. When clan A
 * offers an alliance to clan B and B later offers back (or accepts), the request
 * is fulfilled. Purely in-memory with a configurable timeout.
 */
public final class AllyRequestService {

    private final Supplier<Settings> settings;

    /** "fromClanId:toClanId" -> expiry epoch millis. */
    private final Map<String, Long> pending = new ConcurrentHashMap<>();

    public AllyRequestService(Supplier<Settings> settings) {
        this.settings = settings;
    }

    private static String key(int from, int to) {
        return from + ":" + to;
    }

    /** Records an offer from {@code fromClanId} to {@code toClanId}. */
    public void offer(int fromClanId, int toClanId) {
        long expiry = System.currentTimeMillis() + settings.get().allyRequestTimeoutSeconds() * 1000L;
        pending.put(key(fromClanId, toClanId), expiry);
    }

    /** @return {@code true} if {@code fromClanId} currently has a live offer to {@code toClanId}. */
    public boolean hasOffer(int fromClanId, int toClanId) {
        Long expiry = pending.get(key(fromClanId, toClanId));
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public void remove(int fromClanId, int toClanId) {
        pending.remove(key(fromClanId, toClanId));
    }

    public void clearClan(int clanId) {
        pending.keySet().removeIf(k -> {
            String[] parts = k.split(":");
            return parts[0].equals(String.valueOf(clanId)) || parts[1].equals(String.valueOf(clanId));
        });
    }

    public void purgeExpired() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(e -> e.getValue() <= now);
    }
}
