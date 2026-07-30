package io.github.sheynor43.clans.service;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.hook.EconomyProvider;
import io.github.sheynor43.clans.model.Clan;
import org.bukkit.OfflinePlayer;

import java.util.function.Supplier;

/**
 * Clan bank operations. Disabled unless {@code bank.enabled} is true and a Vault
 * economy is available. Capacity is taken from the clan's current level perk.
 */
public final class BankService {

    public enum Result {
        OK,
        DISABLED,
        INVALID_AMOUNT,
        CAPACITY_EXCEEDED,
        NOT_ENOUGH_PERSONAL,
        NOT_ENOUGH_BANK
    }

    private final Supplier<Settings> settings;
    private final ClanManager clans;
    private final EconomyProvider economy;

    public BankService(Supplier<Settings> settings, ClanManager clans, EconomyProvider economy) {
        this.settings = settings;
        this.clans = clans;
        this.economy = economy;
    }

    public boolean isEnabled() {
        return settings.get().bankEnabled() && economy != null && economy.isEnabled();
    }

    public double capacity(Clan clan) {
        return clans.perksFor(clan).bankCapacity();
    }

    public Result deposit(OfflinePlayer player, Clan clan, double amount) {
        if (!isEnabled()) {
            return Result.DISABLED;
        }
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        if (clan.balance() + amount > capacity(clan)) {
            return Result.CAPACITY_EXCEEDED;
        }
        if (!economy.has(player, amount)) {
            return Result.NOT_ENOUGH_PERSONAL;
        }
        if (!economy.withdraw(player, amount)) {
            return Result.NOT_ENOUGH_PERSONAL;
        }
        clans.setBalance(clan, clan.balance() + amount);
        return Result.OK;
    }

    public Result withdraw(OfflinePlayer player, Clan clan, double amount) {
        if (!isEnabled()) {
            return Result.DISABLED;
        }
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        if (clan.balance() < amount) {
            return Result.NOT_ENOUGH_BANK;
        }
        if (!economy.deposit(player, amount)) {
            return Result.NOT_ENOUGH_BANK;
        }
        clans.setBalance(clan, clan.balance() - amount);
        return Result.OK;
    }

    public String format(double amount) {
        return economy != null ? economy.format(amount) : String.valueOf(amount);
    }
}
