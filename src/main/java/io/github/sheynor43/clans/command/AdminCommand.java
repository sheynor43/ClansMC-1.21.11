package io.github.sheynor43.clans.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.sheynor43.clans.ClansPlugin;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.model.ClanMember;
import io.github.sheynor43.clans.util.ClanPlaceholders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

/** {@code /clanadmin} — administrative clan management. Requires {@code clans.admin}. */
public final class AdminCommand {

    private final ClansPlugin plugin;

    public AdminCommand(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("clanadmin")
                .requires(src -> src.getSender().hasPermission("clans.admin"))
                .then(Commands.literal("delete")
                        .then(clanArg().executes(this::delete)))
                .then(Commands.literal("settag")
                        .then(clanArg().then(Commands.argument("newtag", StringArgumentType.word())
                                .executes(this::setTag))))
                .then(Commands.literal("setname")
                        .then(clanArg().then(Commands.argument("newname", StringArgumentType.greedyString())
                                .executes(this::setName))))
                .then(Commands.literal("join")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(clanArg().executes(this::join))))
                .then(Commands.literal("leave")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(this::leave)))
                .then(Commands.literal("setleader")
                        .then(clanArg().then(Commands.argument("player", StringArgumentType.word())
                                .executes(this::setLeader))))
                .build();
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> clanArg() {
        return Commands.argument("clan", StringArgumentType.word()).suggests(clanTags());
    }

    private int delete(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Clan clan = resolve(ctx).orElse(null);
        if (clan == null) {
            unknownClan(ctx);
            return 1;
        }
        for (ClanMember member : clan.members()) {
            Player online = Bukkit.getPlayer(member.uuid());
            if (online != null) {
                online.setGlowing(false);
            }
        }
        plugin.invites().clearClan(clan.id());
        plugin.allyRequests().clearClan(clan.id());
        plugin.clans().disband(clan);
        plugin.tab().applyAll();
        reply(sender, "admin.clan-deleted", ClanPlaceholders.of(clan, plugin.settings()));
        return 1;
    }

    private int setTag(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Clan clan = resolve(ctx).orElse(null);
        if (clan == null) {
            unknownClan(ctx);
            return 1;
        }
        String newTag = StringArgumentType.getString(ctx, "newtag");
        Optional<Clan> existing = plugin.clans().getClanByTag(newTag);
        if (existing.isPresent() && existing.get().id() != clan.id()) {
            reply(sender, "create.errors.tag-taken", Placeholder.unparsed("tag", newTag));
            return 1;
        }
        plugin.clans().setTag(clan, newTag);
        plugin.tab().applyAll();
        reply(sender, "admin.tag-changed", ClanPlaceholders.of(clan, plugin.settings()));
        return 1;
    }

    private int setName(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Clan clan = resolve(ctx).orElse(null);
        if (clan == null) {
            unknownClan(ctx);
            return 1;
        }
        String newName = StringArgumentType.getString(ctx, "newname").strip();
        Optional<Clan> existing = plugin.clans().getClanByName(newName);
        if (existing.isPresent() && existing.get().id() != clan.id()) {
            reply(sender, "create.errors.name-taken", Placeholder.unparsed("clan", newName));
            return 1;
        }
        plugin.clans().rename(clan, newName);
        plugin.tab().applyAll();
        reply(sender, "admin.name-changed", ClanPlaceholders.of(clan, plugin.settings()));
        return 1;
    }

    private int join(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Clan clan = resolve(ctx).orElse(null);
        if (clan == null) {
            unknownClan(ctx);
            return 1;
        }
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = lookup(name);
        if (target == null) {
            reply(sender, "general.unknown-player", Placeholder.unparsed("player", name));
            return 1;
        }
        if (plugin.clans().isInClan(target.getUniqueId())) {
            reply(sender, "clan.target-already-in-clan", Placeholder.unparsed("player", displayName(target, name)));
            return 1;
        }
        plugin.clans().addMember(clan, target.getUniqueId(), displayName(target, name));
        reply(sender, "admin.player-joined", TagResolver.resolver(
                ClanPlaceholders.of(clan, plugin.settings()),
                Placeholder.unparsed("player", displayName(target, name))));
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            plugin.tab().apply(online);
        }
        return 1;
    }

    private int leave(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = lookup(name);
        if (target == null) {
            reply(sender, "general.unknown-player", Placeholder.unparsed("player", name));
            return 1;
        }
        Optional<Clan> clan = plugin.clans().getClanOf(target.getUniqueId());
        if (clan.isEmpty()) {
            reply(sender, "admin.player-not-in-clan", Placeholder.unparsed("player", displayName(target, name)));
            return 1;
        }
        Clan c = clan.get();
        if (c.isLeader(target.getUniqueId()) && c.memberCount() > 1) {
            // Leader removed by admin: promote the earliest-joined remaining member.
            c.members().stream()
                    .filter(m -> !m.uuid().equals(target.getUniqueId()))
                    .min((a, b) -> Long.compare(a.joinedAt(), b.joinedAt()))
                    .ifPresent(next -> plugin.clans().transferLeadership(c, next.uuid()));
        }
        boolean wasLastMember = c.memberCount() <= 1;
        plugin.clans().removeMember(c, target.getUniqueId(), true);
        if (wasLastMember) {
            plugin.clans().disband(c);
        }
        reply(sender, "admin.player-removed", Placeholder.unparsed("player", displayName(target, name)));
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            plugin.tab().apply(online);
            online.setGlowing(false);
        }
        return 1;
    }

    private int setLeader(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Clan clan = resolve(ctx).orElse(null);
        if (clan == null) {
            unknownClan(ctx);
            return 1;
        }
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = lookup(name);
        if (target == null || !clan.isMember(target.getUniqueId())) {
            reply(sender, "clan.target-not-in-your-clan", Placeholder.unparsed("player", name));
            return 1;
        }
        plugin.clans().transferLeadership(clan, target.getUniqueId());
        reply(sender, "admin.leader-set", TagResolver.resolver(
                ClanPlaceholders.of(clan, plugin.settings()),
                Placeholder.unparsed("player", displayName(target, name))));
        return 1;
    }

    // ---- helpers ------------------------------------------------------------

    private Optional<Clan> resolve(CommandContext<CommandSourceStack> ctx) {
        String token = StringArgumentType.getString(ctx, "clan");
        Optional<Clan> byTag = plugin.clans().getClanByTag(token);
        return byTag.isPresent() ? byTag : plugin.clans().getClanByName(token);
    }

    private void unknownClan(CommandContext<CommandSourceStack> ctx) {
        reply(ctx.getSource().getSender(), "general.unknown-clan",
                Placeholder.unparsed("clan", StringArgumentType.getString(ctx, "clan")));
    }

    private OfflinePlayer lookup(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private String displayName(OfflinePlayer player, String fallback) {
        return player.getName() != null ? player.getName() : fallback;
    }

    private SuggestionProvider<CommandSourceStack> clanTags() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (Clan clan : plugin.clans().getClans()) {
                if (clan.tag().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(clan.tag());
                }
            }
            return builder.buildFuture();
        };
    }

    private void reply(CommandSender sender, String key, TagResolver... resolvers) {
        String locale = sender instanceof Player p ? plugin.lang().localeFor(p) : plugin.lang().defaultLocale();
        plugin.lang().send(sender, locale, key, resolvers);
    }
}
