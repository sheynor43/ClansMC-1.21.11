package io.github.sheynor43.clans.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Soft AuthMe integration via reflection, so the plugin neither compiles nor
 * depends on AuthMe. When AuthMe is present, {@link #isAuthenticated(Player)}
 * reports whether a player has logged in; otherwise the tag is applied on a delay.
 */
public final class AuthMeHook {

    private final boolean present;
    private Object apiInstance;
    private Method isAuthenticatedMethod;

    public AuthMeHook() {
        boolean ok = false;
        if (Bukkit.getPluginManager().getPlugin("AuthMe") != null) {
            try {
                Class<?> apiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
                this.apiInstance = apiClass.getMethod("getInstance").invoke(null);
                this.isAuthenticatedMethod = apiClass.getMethod("isAuthenticated", Player.class);
                ok = this.apiInstance != null;
            } catch (ReflectiveOperationException ex) {
                ok = false;
            }
        }
        this.present = ok;
    }

    public boolean isPresent() {
        return present;
    }

    /** @return whether the player is authenticated, or {@code true} if AuthMe is absent. */
    public boolean isAuthenticated(Player player) {
        if (!present) {
            return true;
        }
        try {
            return (boolean) isAuthenticatedMethod.invoke(apiInstance, player);
        } catch (ReflectiveOperationException ex) {
            return true;
        }
    }
}
