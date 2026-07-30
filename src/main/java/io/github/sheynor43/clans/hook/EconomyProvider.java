package io.github.sheynor43.clans.hook;

import org.bukkit.OfflinePlayer;

/**
 * Economy abstraction so the clan bank does not depend on any specific economy
 * plugin. The default implementation is backed by Vault, but others can be added.
 */
public interface EconomyProvider {

    boolean isEnabled();

    double balance(OfflinePlayer player);

    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    String format(double amount);
}
