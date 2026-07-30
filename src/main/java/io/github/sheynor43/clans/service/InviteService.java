package io.github.sheynor43.clans.service;

import io.github.sheynor43.clans.config.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * In-memory clan invitations with expiry and per-target anti-spam. Nothing is
 * persisted: invitations live only for the configured window.
 */
public final class InviteService {

    private final Supplier<Settings> settings;

    /** target player -> (clan id -> expiry epoch millis). */
    private final Map<UUID, Map<Integer, Long>> invites = new ConcurrentHashMap<>();
    /** "clanId:target" -> last invite epoch millis, for anti-spam. */
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

    public InviteService(Supplier<Settings> settings) {
        this.settings = settings;
    }

    public boolean isOnCooldown(int clanId, UUID target) {
        Long last = lastSent.get(clanId + ":" + target);
        if (last == null) {
            return false;
        }
        long window = settings.get().inviteAntiSpamSeconds() * 1000L;
        return System.currentTimeMillis() - last < window;
    }

    public void invite(int clanId, UUID target) {
        long expiry = System.currentTimeMillis() + settings.get().inviteExpireSeconds() * 1000L;
        invites.computeIfAbsent(target, k -> new ConcurrentHashMap<>()).put(clanId, expiry);
        lastSent.put(clanId + ":" + target, System.currentTimeMillis());
    }

    public boolean hasInvite(UUID target, int clanId) {
        Map<Integer, Long> map = invites.get(target);
        if (map == null) {
            return false;
        }
        Long expiry = map.get(clanId);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public List<Integer> activeInvites(UUID target) {
        List<Integer> result = new ArrayList<>();
        Map<Integer, Long> map = invites.get(target);
        if (map == null) {
            return result;
        }
        long now = System.currentTimeMillis();
        map.forEach((clanId, expiry) -> {
            if (expiry > now) {
                result.add(clanId);
            }
        });
        return result;
    }

    public void consume(UUID target, int clanId) {
        Map<Integer, Long> map = invites.get(target);
        if (map != null) {
            map.remove(clanId);
            if (map.isEmpty()) {
                invites.remove(target);
            }
        }
    }

    public void clearPlayer(UUID target) {
        invites.remove(target);
    }

    /** Drops all invitations to a clan (e.g. on disband). */
    public void clearClan(int clanId) {
        invites.values().forEach(map -> map.remove(clanId));
        lastSent.keySet().removeIf(key -> key.startsWith(clanId + ":"));
    }

    /** Removes expired invitations and stale anti-spam records. */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        invites.values().forEach(map -> map.values().removeIf(expiry -> expiry <= now));
        invites.entrySet().removeIf(e -> e.getValue().isEmpty());
        long antiSpamWindow = settings.get().inviteAntiSpamSeconds() * 1000L;
        lastSent.entrySet().removeIf(e -> now - e.getValue() > antiSpamWindow);
    }
}
