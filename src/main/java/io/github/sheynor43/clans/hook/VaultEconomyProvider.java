package io.github.sheynor43.clans.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault-backed {@link EconomyProvider}. Resolves the economy service lazily and
 * reports {@link #isEnabled()} as {@code false} when Vault or an economy plugin
 * is missing, letting the bank module disable itself gracefully.
 */
public final class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;

    private VaultEconomyProvider(Economy economy) {
        this.economy = economy;
    }

    /** @return a provider if Vault and an economy are present, otherwise {@code null}. */
    public static VaultEconomyProvider tryCreate() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }
        return new VaultEconomyProvider(rsp.getProvider());
    }

    @Override
    public boolean isEnabled() {
        return economy != null && economy.isEnabled();
    }

    @Override
    public double balance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
