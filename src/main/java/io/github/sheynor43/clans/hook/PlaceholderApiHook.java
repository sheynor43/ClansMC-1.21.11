package io.github.sheynor43.clans.hook;

import io.github.sheynor43.clans.api.RelationType;
import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.model.ClanMember;
import io.github.sheynor43.clans.service.BankService;
import io.github.sheynor43.clans.service.ClanManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * PlaceholderAPI expansion. Registered only when PlaceholderAPI is installed.
 * Exposes clan data as placeholders so external tab plugins (e.g. FlectonePulse)
 * can render the clan tag in {@code PLACEHOLDER_ONLY} tab mode.
 */
public final class PlaceholderApiHook extends PlaceholderExpansion {

    private final Supplier<Settings> settings;
    private final ClanManager clans;
    private final BankService bank;
    private final String version;

    public PlaceholderApiHook(Supplier<Settings> settings, ClanManager clans, BankService bank, String version) {
        this.settings = settings;
        this.clans = clans;
        this.bank = bank;
        this.version = version;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "clans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "sheynor43";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Optional<Clan> optional = clans.getClanOf(player.getUniqueId());
        if (optional.isEmpty()) {
            return "";
        }
        Clan clan = optional.get();
        return switch (params.toLowerCase()) {
            case "tag" -> clan.tag();
            case "tag_formatted" -> settings.get().tagColor() + clan.tag();
            case "name" -> clan.name();
            case "role" -> roleOf(clan, player);
            case "level" -> String.valueOf(clan.level());
            case "members_total" -> String.valueOf(clan.memberCount());
            case "members_online" -> String.valueOf(onlineCount(clan));
            case "bank" -> bank.format(clan.balance());
            case "allies" -> String.valueOf(relationCount(clan, RelationType.ALLY));
            default -> null;
        };
    }

    private String roleOf(Clan clan, OfflinePlayer player) {
        ClanMember member = clan.member(player.getUniqueId());
        return member != null ? member.role().name() : "";
    }

    private long onlineCount(Clan clan) {
        return clan.members().stream()
                .filter(m -> {
                    var online = Bukkit.getPlayer(m.uuid());
                    return online != null && online.isOnline();
                })
                .count();
    }

    private long relationCount(Clan clan, RelationType type) {
        return clan.relations().stream().filter(r -> r.type() == type).count();
    }
}
