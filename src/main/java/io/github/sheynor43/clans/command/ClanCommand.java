package io.github.sheynor43.clans.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.sheynor43.clans.ClansPlugin;
import io.github.sheynor43.clans.api.RelationStatus;
import io.github.sheynor43.clans.api.RelationType;
import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.model.ClanMember;
import io.github.sheynor43.clans.model.ClanRole;
import io.github.sheynor43.clans.service.BankService;
import io.github.sheynor43.clans.service.ClanCreationService;
import io.github.sheynor43.clans.util.ClanPlaceholders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds and handles the whole {@code /clan} Brigadier tree. Clans are referenced
 * by their (single-word) tag in arguments; names may contain spaces and are used
 * for display only.
 */
public final class ClanCommand {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private final ClansPlugin plugin;

    public ClanCommand(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("clan")
                .requires(src -> src.getSender().hasPermission("clans.use"))
                .executes(ctx -> help(ctx))
                .then(Commands.literal("create")
                        .requires(src -> src.getSender().hasPermission(plugin.settings().createPermission()))
                        .executes(ctx -> createDialog(ctx))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("tag", StringArgumentType.word())
                                        .executes(ctx -> createQuick(ctx)))))
                .then(Commands.literal("create-confirm").executes(ctx -> createConfirm(ctx)))
                .then(Commands.literal("cancel").executes(ctx -> cancel(ctx)))
                .then(inviteLiteral("add"))
                .then(inviteLiteral("invite"))
                .then(acceptLiteral())
                .then(denyLiteral())
                .then(Commands.literal("invites").executes(this::invites))
                .then(Commands.literal("info")
                        .executes(ctx -> infoSelf(ctx))
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .suggests(clanTags())
                                .executes(ctx -> infoOther(ctx))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> list(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(Commands.literal("members").executes(ctx -> members(ctx)))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx)))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(clanMemberNames())
                                .executes(ctx -> kick(ctx))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(clanMemberNames())
                                .executes(ctx -> transferConfirmPrompt(ctx))
                                .then(Commands.literal("confirm").executes(ctx -> transfer(ctx)))))
                .then(Commands.literal("disband")
                        .executes(ctx -> disbandPrompt(ctx))
                        .then(Commands.literal("confirm").executes(ctx -> disband(ctx))))
                .then(Commands.literal("chat").executes(ctx -> toggleChat(ctx)))
                .then(relationLiteral("ally", ctx -> ally(ctx)))
                .then(relationLiteral("unally", ctx -> unally(ctx)))
                .then(relationLiteral("enemy", ctx -> enemy(ctx)))
                .then(relationLiteral("unenemy", ctx -> unenemy(ctx)))
                .then(Commands.literal("bank").executes(ctx -> bankBalance(ctx)))
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                .executes(ctx -> deposit(ctx))))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                .executes(ctx -> withdraw(ctx))))
                .then(Commands.literal("glow").executes(ctx -> glow(ctx)))
                .then(Commands.literal("menu").executes(ctx -> menu(ctx)))
                .then(Commands.literal("gui").executes(ctx -> menu(ctx)))
                .then(Commands.literal("help").executes(ctx -> help(ctx)))
                .then(Commands.literal("reload")
                        .requires(src -> src.getSender().hasPermission("clans.admin"))
                        .executes(ctx -> reload(ctx)))
                .build();
    }

    // ---- create -------------------------------------------------------------

    private int createDialog(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        if (plugin.clans().isInClan(player.getUniqueId())) {
            msg(player, "clan.already-in-clan");
            return 1;
        }
        plugin.dialog().start(player);
        return 1;
    }

    private int createQuick(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        if (plugin.clans().isInClan(player.getUniqueId())) {
            msg(player, "clan.already-in-clan");
            return 1;
        }
        String name = StringArgumentType.getString(ctx, "name");
        String tag = StringArgumentType.getString(ctx, "tag");
        Settings s = plugin.settings();
        ClanCreationService creation = plugin.creation();

        TagResolver nameMinMax = TagResolver.resolver(
                Placeholder.unparsed("min", String.valueOf(s.nameMin())),
                Placeholder.unparsed("max", String.valueOf(s.nameMax())));
        switch (creation.checkName(name)) {
            case TOO_SHORT, TOO_LONG -> {
                msg(player, "create.errors.name-length", nameMinMax);
                return 1;
            }
            case BLACKLISTED -> {
                msg(player, "create.errors.name-blacklisted");
                return 1;
            }
            case TAKEN -> {
                msg(player, "create.errors.name-taken", Placeholder.unparsed("clan", name));
                return 1;
            }
            case OK -> { /* continue */ }
        }
        TagResolver tagMinMax = TagResolver.resolver(
                Placeholder.unparsed("min", String.valueOf(s.tagMin())),
                Placeholder.unparsed("max", String.valueOf(s.tagMax())));
        switch (creation.checkTag(tag)) {
            case BAD_FORMAT -> {
                msg(player, "create.errors.tag-format");
                return 1;
            }
            case TOO_SHORT, TOO_LONG -> {
                msg(player, "create.errors.tag-length", tagMinMax);
                return 1;
            }
            case TAKEN -> {
                msg(player, "create.errors.tag-taken", Placeholder.unparsed("tag", tag));
                return 1;
            }
            case OK -> { /* continue */ }
        }

        Clan clan = creation.create(player.getUniqueId(), player.getName(), name, tag);
        if (clan == null) {
            msg(player, "create.errors.name-taken", Placeholder.unparsed("clan", name));
            return 1;
        }
        msg(player, "clan.created", ClanPlaceholders.of(clan, s));
        plugin.tab().apply(player);
        return 1;
    }

    private int createConfirm(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player != null) {
            plugin.dialog().confirm(player);
        }
        return 1;
    }

    private int cancel(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player != null) {
            plugin.dialog().cancel(player);
        }
        return 1;
    }

    // ---- invitations --------------------------------------------------------

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> inviteLiteral(String name) {
        return Commands.literal(name)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(invitablePlayers())
                        .executes(ctx -> invite(ctx)));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> denyLiteral() {
        return Commands.literal("deny")
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(clanTags())
                        .executes(this::deny));
    }

    private int invite(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            msg(player, "clan.not-in-clan");
            return 1;
        }
        Clan clan = clanOpt.get();
        if (!clan.isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return 1;
        }
        if (!belowMemberLimit(clan)) {
            msg(player, "clan.member-limit-reached",
                    Placeholder.unparsed("amount", String.valueOf(plugin.settings().maxMembers())));
            return 1;
        }
        String targetName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            msg(player, "general.player-offline", Placeholder.unparsed("player", targetName));
            return 1;
        }
        if (plugin.clans().isInClan(target.getUniqueId())) {
            msg(player, "clan.target-already-in-clan", Placeholder.unparsed("player", target.getName()));
            return 1;
        }
        if (plugin.invites().isOnCooldown(clan.id(), target.getUniqueId())) {
            msg(player, "invite.antispam", Placeholder.unparsed("player", target.getName()));
            return 1;
        }
        plugin.invites().invite(clan.id(), target.getUniqueId());
        msg(player, "invite.sent", Placeholder.unparsed("player", target.getName()));
        plugin.lang().sendList(target, plugin.lang().localeFor(target), "invite.received",
                ClanPlaceholders.of(clan, plugin.settings()));
        return 1;
    }

    private int accept(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        if (plugin.clans().isInClan(player.getUniqueId())) {
            msg(player, "clan.already-in-clan");
            return 1;
        }
        Clan clan = resolveClan(StringArgumentType.getString(ctx, "tag")).orElse(null);
        if (clan == null || !plugin.invites().hasInvite(player.getUniqueId(), clan.id())) {
            msg(player, "invite.no-such-invite",
                    Placeholder.unparsed("clan", StringArgumentType.getString(ctx, "tag")));
            return 1;
        }
        if (!belowMemberLimit(clan)) {
            msg(player, "clan.member-limit-reached",
                    Placeholder.unparsed("amount", String.valueOf(plugin.settings().maxMembers())));
            return 1;
        }
        plugin.invites().consume(player.getUniqueId(), clan.id());
        plugin.clans().addMember(clan, player.getUniqueId(), player.getName());
        msg(player, "invite.accepted-self", ClanPlaceholders.of(clan, plugin.settings()));
        plugin.broadcaster().broadcast(clan, "invite.accepted-broadcast",
                Placeholder.unparsed("player", player.getName()));
        plugin.tab().apply(player);
        plugin.glow().applyForJoin(player);
        return 1;
    }

    private int deny(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Clan clan = resolveClan(StringArgumentType.getString(ctx, "tag")).orElse(null);
        if (clan == null || !plugin.invites().hasInvite(player.getUniqueId(), clan.id())) {
            msg(player, "invite.no-such-invite",
                    Placeholder.unparsed("clan", StringArgumentType.getString(ctx, "tag")));
            return 1;
        }
        plugin.invites().consume(player.getUniqueId(), clan.id());
        msg(player, "invite.denied-self", ClanPlaceholders.of(clan, plugin.settings()));
        return 1;
    }

    private int invites(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        List<Integer> active = plugin.invites().activeInvites(player.getUniqueId());
        if (active.isEmpty()) {
            msg(player, "invite.none-pending");
            return 1;
        }
        msg(player, "invite.list-header");
        for (int clanId : active) {
            plugin.clans().getClanById(clanId).ifPresent(clan ->
                    plugin.lang().send(player, plugin.lang().localeFor(player), "invite.list-line",
                            ClanPlaceholders.of(clan, plugin.settings())));
        }
        return 1;
    }

    // ---- info / list / members ---------------------------------------------

    private int infoSelf(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clan = plugin.clans().getClanOf(player.getUniqueId());
        if (clan.isEmpty()) {
            msg(player, "clan.not-in-clan");
            return 1;
        }
        sendInfo(player, clan.get());
        return 1;
    }

    private int infoOther(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        String token = StringArgumentType.getString(ctx, "tag");
        Optional<Clan> clan = resolveClan(token);
        if (clan.isEmpty()) {
            msg(player, "general.unknown-clan", Placeholder.unparsed("clan", token));
            return 1;
        }
        sendInfo(player, clan.get());
        return 1;
    }

    private void sendInfo(Player viewer, Clan clan) {
        String locale = plugin.lang().localeFor(viewer);
        Settings s = plugin.settings();
        TagResolver base = ClanPlaceholders.of(clan, s);
        send(viewer, locale, "clan.info.header", base);

        String leaderName = memberName(clan, clan.leader());
        send(viewer, locale, "clan.info.leader", Placeholder.unparsed("player", leaderName));

        if (s.levelsEnabled()) {
            OptionalLong next = s.levelTable().xpForNext(clan.clanXp());
            if (next.isPresent()) {
                send(viewer, locale, "clan.info.level",
                        Placeholder.unparsed("level", String.valueOf(clan.level())),
                        Placeholder.unparsed("xp", String.valueOf(clan.clanXp())),
                        Placeholder.unparsed("xp_next", String.valueOf(next.getAsLong())));
            } else {
                send(viewer, locale, "clan.info.level-max",
                        Placeholder.unparsed("level", String.valueOf(clan.level())));
            }
        }

        send(viewer, locale, "clan.info.created",
                Placeholder.unparsed("date", DATE.format(Instant.ofEpochMilli(clan.createdAt()))));

        long online = clan.members().stream().filter(m -> isOnline(m.uuid())).count();
        send(viewer, locale, "clan.info.members",
                Placeholder.unparsed("online", String.valueOf(online)),
                Placeholder.unparsed("total", String.valueOf(clan.memberCount())));
        String names = clan.members().stream().map(ClanMember::lastName)
                .filter(java.util.Objects::nonNull).collect(Collectors.joining(", "));
        send(viewer, locale, "clan.info.members-list", Placeholder.unparsed("names", names));

        if (plugin.bank().isEnabled()) {
            send(viewer, locale, "clan.info.bank",
                    Placeholder.unparsed("amount", plugin.bank().format(clan.balance())));
        }

        send(viewer, locale, "clan.info.allies",
                Placeholder.unparsed("allies", relationNames(clan, RelationType.ALLY, locale)));
        send(viewer, locale, "clan.info.enemies",
                Placeholder.unparsed("enemies", relationNames(clan, RelationType.ENEMY, locale)));
        send(viewer, locale, "clan.info.stats",
                Placeholder.unparsed("amount_wither", String.valueOf(clan.stat(Clan.BOSS_WITHER))),
                Placeholder.unparsed("amount_dragon", String.valueOf(clan.stat(Clan.BOSS_DRAGON))));
    }

    private String relationNames(Clan clan, RelationType type, String locale) {
        StringJoiner joiner = new StringJoiner(", ");
        clan.relations().stream()
                .filter(r -> r.type() == type && r.status() == RelationStatus.ACTIVE)
                .forEach(r -> plugin.clans().getClanById(r.otherClanId())
                        .ifPresent(other -> joiner.add(other.name())));
        return joiner.length() == 0 ? plugin.lang().renderPlain(locale, "clan.info.none") : joiner.toString();
    }

    private int list(CommandContext<CommandSourceStack> ctx, int page) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        String locale = plugin.lang().localeFor(player);
        List<Clan> all = plugin.clans().sortedByMembers();
        if (all.isEmpty()) {
            msg(player, "clan.list.empty");
            return 1;
        }
        int perPage = 8;
        int pages = (all.size() + perPage - 1) / perPage;
        int current = Math.min(Math.max(1, page), pages);
        send(player, locale, "clan.list.header",
                Placeholder.unparsed("page", String.valueOf(current)),
                Placeholder.unparsed("pages", String.valueOf(pages)));
        int from = (current - 1) * perPage;
        for (Clan clan : all.subList(from, Math.min(from + perPage, all.size()))) {
            send(player, locale, "clan.list.line", TagResolver.resolver(
                    ClanPlaceholders.of(clan, plugin.settings()),
                    Placeholder.unparsed("members", String.valueOf(clan.memberCount())),
                    Placeholder.unparsed("level", String.valueOf(clan.level()))));
        }
        if (current < pages) {
            send(player, locale, "clan.list.footer");
        }
        return 1;
    }

    private int members(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            msg(player, "clan.not-in-clan");
            return 1;
        }
        Clan clan = clanOpt.get();
        String locale = plugin.lang().localeFor(player);
        send(player, locale, "clan.members.header", ClanPlaceholders.of(clan, plugin.settings()));
        for (ClanMember member : clan.members()) {
            String key = isOnline(member.uuid()) ? "clan.members.online" : "clan.members.offline";
            send(player, locale, key,
                    Placeholder.unparsed("player", String.valueOf(member.lastName())),
                    Placeholder.unparsed("role", member.role().name()));
        }
        return 1;
    }

    // ---- membership ---------------------------------------------------------

    private int leave(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            msg(player, "clan.not-in-clan");
            return 1;
        }
        Clan clan = clanOpt.get();
        if (clan.isLeader(player.getUniqueId())) {
            msg(player, "clan.leader-cannot-leave");
            return 1;
        }
        plugin.clans().removeMember(clan, player.getUniqueId(), false);
        msg(player, "clan.you-left", ClanPlaceholders.of(clan, plugin.settings()));
        plugin.broadcaster().broadcast(clan, "clan.member-left", Placeholder.unparsed("player", player.getName()));
        plugin.tab().apply(player);
        player.setGlowing(false);
        return 1;
    }

    private int kick(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty() || !clanOpt.get().isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return 1;
        }
        Clan clan = clanOpt.get();
        String targetName = StringArgumentType.getString(ctx, "player");
        ClanMember target = findMemberByName(clan, targetName);
        if (target == null) {
            msg(player, "clan.target-not-in-your-clan", Placeholder.unparsed("player", targetName));
            return 1;
        }
        if (target.uuid().equals(player.getUniqueId())) {
            msg(player, "clan.cannot-kick-self");
            return 1;
        }
        plugin.clans().removeMember(clan, target.uuid(), true);
        plugin.broadcaster().broadcast(clan, "clan.member-kicked", Placeholder.unparsed("player", target.lastName()));
        Player online = Bukkit.getPlayer(target.uuid());
        if (online != null) {
            msg(online, "clan.you-were-kicked", ClanPlaceholders.of(clan, plugin.settings()));
            plugin.tab().apply(online);
            online.setGlowing(false);
        }
        return 1;
    }

    private int transferConfirmPrompt(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty() || !clanOpt.get().isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return 1;
        }
        String targetName = StringArgumentType.getString(ctx, "player");
        ClanMember target = findMemberByName(clanOpt.get(), targetName);
        if (target == null || target.uuid().equals(player.getUniqueId())) {
            msg(player, "clan.target-not-in-your-clan", Placeholder.unparsed("player", targetName));
            return 1;
        }
        plugin.lang().sendList(player, plugin.lang().localeFor(player), "clan.confirm-transfer", TagResolver.resolver(
                ClanPlaceholders.of(clanOpt.get(), plugin.settings()),
                Placeholder.unparsed("player", target.lastName())));
        return 1;
    }

    private int transfer(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty() || !clanOpt.get().isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return 1;
        }
        Clan clan = clanOpt.get();
        String targetName = StringArgumentType.getString(ctx, "player");
        ClanMember target = findMemberByName(clan, targetName);
        if (target == null || target.uuid().equals(player.getUniqueId())) {
            msg(player, "clan.target-not-in-your-clan", Placeholder.unparsed("player", targetName));
            return 1;
        }
        plugin.clans().transferLeadership(clan, target.uuid());
        msg(player, "clan.transferred", Placeholder.unparsed("player", target.lastName()));
        Player online = Bukkit.getPlayer(target.uuid());
        if (online != null) {
            msg(online, "clan.transfer-received", ClanPlaceholders.of(clan, plugin.settings()));
        }
        return 1;
    }

    private int disbandPrompt(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty() || !clanOpt.get().isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return 1;
        }
        plugin.lang().sendList(player, plugin.lang().localeFor(player), "clan.confirm-disband",
                ClanPlaceholders.of(clanOpt.get(), plugin.settings()));
        return 1;
    }

    private int disband(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty() || !clanOpt.get().isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return 1;
        }
        Clan clan = clanOpt.get();
        plugin.broadcaster().broadcast(clan, "clan.disbanded", ClanPlaceholders.of(clan, plugin.settings()));
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
        return 1;
    }

    // ---- chat ---------------------------------------------------------------

    private int toggleChat(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        if (!player.hasPermission("clans.chat")) {
            msg(player, "general.no-permission");
            return 1;
        }
        if (plugin.clans().getClanOf(player.getUniqueId()).isEmpty()) {
            msg(player, "clan.not-in-clan");
            return 1;
        }
        boolean on = plugin.clanChat().toggle(player.getUniqueId());
        msg(player, on ? "chat.toggle-on" : "chat.toggle-off");
        return 1;
    }

    // ---- relations ----------------------------------------------------------

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> relationLiteral(
            String name, com.mojang.brigadier.Command<CommandSourceStack> action) {
        return Commands.literal(name)
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(clanTags())
                        .executes(action));
    }

    private Optional<Clan> leaderClan(Player player) {
        Optional<Clan> clan = plugin.clans().getClanOf(player.getUniqueId());
        if (clan.isEmpty() || !clan.get().isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return Optional.empty();
        }
        return clan;
    }

    private int ally(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> mine = leaderClan(player);
        if (mine.isEmpty()) {
            return 1;
        }
        Clan clan = mine.get();
        Clan other = resolveClan(StringArgumentType.getString(ctx, "tag")).orElse(null);
        if (other == null) {
            msg(player, "general.unknown-clan", Placeholder.unparsed("clan", StringArgumentType.getString(ctx, "tag")));
            return 1;
        }
        if (other.id() == clan.id()) {
            msg(player, "relations.cannot-self");
            return 1;
        }
        if (clan.hasRelation(other.id(), RelationType.ALLY)) {
            msg(player, "relations.ally-already", ClanPlaceholders.of(other, plugin.settings()));
            return 1;
        }
        int maxAllies = plugin.clans().perksFor(clan).maxAllies();
        long currentAllies = clan.relations().stream()
                .filter(r -> r.type() == RelationType.ALLY && r.status() == RelationStatus.ACTIVE).count();
        if (currentAllies >= maxAllies) {
            msg(player, "relations.ally-limit", Placeholder.unparsed("amount", String.valueOf(maxAllies)));
            return 1;
        }

        if (plugin.allyRequests().hasOffer(other.id(), clan.id())) {
            // Mutual: form the alliance both ways.
            plugin.allyRequests().remove(other.id(), clan.id());
            plugin.clans().setRelation(clan, other, RelationType.ALLY, RelationStatus.ACTIVE);
            plugin.clans().setRelation(other, clan, RelationType.ALLY, RelationStatus.ACTIVE);
            msg(player, "relations.ally-formed", ClanPlaceholders.of(other, plugin.settings()));
            plugin.broadcaster().broadcast(other, "relations.ally-formed", ClanPlaceholders.of(clan, plugin.settings()));
        } else {
            plugin.allyRequests().offer(clan.id(), other.id());
            msg(player, "relations.ally-requested", ClanPlaceholders.of(other, plugin.settings()));
            plugin.broadcaster().broadcast(other, "relations.ally-request-received",
                    ClanPlaceholders.of(clan, plugin.settings()));
        }
        return 1;
    }

    private int unally(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> mine = leaderClan(player);
        if (mine.isEmpty()) {
            return 1;
        }
        Clan clan = mine.get();
        Clan other = resolveClan(StringArgumentType.getString(ctx, "tag")).orElse(null);
        if (other == null) {
            msg(player, "general.unknown-clan", Placeholder.unparsed("clan", StringArgumentType.getString(ctx, "tag")));
            return 1;
        }
        plugin.allyRequests().remove(clan.id(), other.id());
        if (!clan.hasRelation(other.id(), RelationType.ALLY)) {
            msg(player, "relations.no-relation", ClanPlaceholders.of(other, plugin.settings()));
            return 1;
        }
        plugin.clans().removeRelation(clan, other);
        plugin.clans().removeRelation(other, clan);
        msg(player, "relations.ally-removed", ClanPlaceholders.of(other, plugin.settings()));
        plugin.broadcaster().broadcast(other, "relations.ally-removed", ClanPlaceholders.of(clan, plugin.settings()));
        return 1;
    }

    private int enemy(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> mine = leaderClan(player);
        if (mine.isEmpty()) {
            return 1;
        }
        Clan clan = mine.get();
        Clan other = resolveClan(StringArgumentType.getString(ctx, "tag")).orElse(null);
        if (other == null) {
            msg(player, "general.unknown-clan", Placeholder.unparsed("clan", StringArgumentType.getString(ctx, "tag")));
            return 1;
        }
        if (other.id() == clan.id()) {
            msg(player, "relations.cannot-self");
            return 1;
        }
        if (clan.hasRelation(other.id(), RelationType.ALLY)) {
            msg(player, "relations.conflict");
            return 1;
        }
        if (clan.hasRelation(other.id(), RelationType.ENEMY)) {
            msg(player, "relations.enemy-already", ClanPlaceholders.of(other, plugin.settings()));
            return 1;
        }
        plugin.clans().setRelation(clan, other, RelationType.ENEMY, RelationStatus.ACTIVE);
        msg(player, "relations.enemy-declared", ClanPlaceholders.of(other, plugin.settings()));
        plugin.broadcaster().broadcast(other, "relations.enemy-received", ClanPlaceholders.of(clan, plugin.settings()));
        return 1;
    }

    private int unenemy(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> mine = leaderClan(player);
        if (mine.isEmpty()) {
            return 1;
        }
        Clan clan = mine.get();
        Clan other = resolveClan(StringArgumentType.getString(ctx, "tag")).orElse(null);
        if (other == null || !clan.hasRelation(other.id(), RelationType.ENEMY)) {
            msg(player, "relations.no-relation",
                    Placeholder.unparsed("clan", StringArgumentType.getString(ctx, "tag")));
            return 1;
        }
        plugin.clans().removeRelation(clan, other);
        msg(player, "relations.enemy-removed", ClanPlaceholders.of(other, plugin.settings()));
        return 1;
    }

    // ---- bank ---------------------------------------------------------------

    private int bankBalance(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            msg(player, "clan.not-in-clan");
            return 1;
        }
        if (!plugin.bank().isEnabled()) {
            msg(player, "bank.disabled");
            return 1;
        }
        Clan clan = clanOpt.get();
        msg(player, "bank.balance",
                Placeholder.unparsed("amount", plugin.bank().format(clan.balance())),
                Placeholder.unparsed("capacity", plugin.bank().format(plugin.bank().capacity(clan))));
        return 1;
    }

    private int deposit(CommandContext<CommandSourceStack> ctx) {
        return bankMove(ctx, true);
    }

    private int withdraw(CommandContext<CommandSourceStack> ctx) {
        return bankMove(ctx, false);
    }

    private int bankMove(CommandContext<CommandSourceStack> ctx, boolean depositing) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        Optional<Clan> clanOpt = plugin.clans().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            msg(player, "clan.not-in-clan");
            return 1;
        }
        Clan clan = clanOpt.get();
        if (!depositing && !clan.isLeader(player.getUniqueId())) {
            msg(player, "clan.only-leader");
            return 1;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        BankService.Result result = depositing
                ? plugin.bank().deposit(player, clan, amount)
                : plugin.bank().withdraw(player, clan, amount);
        BankService bank = plugin.bank();
        switch (result) {
            case DISABLED -> msg(player, "bank.disabled");
            case INVALID_AMOUNT -> msg(player, "bank.amount-positive");
            case CAPACITY_EXCEEDED -> msg(player, "bank.capacity-exceeded",
                    Placeholder.unparsed("capacity", bank.format(bank.capacity(clan))));
            case NOT_ENOUGH_PERSONAL -> msg(player, "bank.not-enough-personal",
                    Placeholder.unparsed("amount", bank.format(amount)));
            case NOT_ENOUGH_BANK -> msg(player, "bank.not-enough-bank",
                    Placeholder.unparsed("amount", bank.format(amount)));
            case OK -> msg(player, depositing ? "bank.deposited" : "bank.withdrew",
                    Placeholder.unparsed("amount", bank.format(amount)),
                    Placeholder.unparsed("balance", bank.format(clan.balance())));
        }
        return 1;
    }

    // ---- glow / menu / help / reload ---------------------------------------

    private int glow(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        if (!plugin.glow().isModuleEnabled()) {
            msg(player, "glow.disabled");
            return 1;
        }
        Optional<Clan> mine = leaderClan(player);
        if (mine.isEmpty()) {
            return 1;
        }
        Clan clan = mine.get();
        if (!plugin.clans().perksFor(clan).glowUnlocked()) {
            msg(player, "glow.locked");
            return 1;
        }
        boolean on = plugin.glow().toggle(clan);
        msg(player, on ? "glow.enabled" : "glow.disabled-now");
        return 1;
    }

    private int menu(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            return 1;
        }
        plugin.menus().openMain(player);
        return 1;
    }

    private int help(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) {
            plugin.lang().send(ctx.getSource().getSender(), plugin.lang().defaultLocale(), "general.players-only");
            return 1;
        }
        String locale = plugin.lang().localeFor(player);
        send(player, locale, "help.header");
        HelpEntry[] entries = {
                new HelpEntry("/clan create", "help.entries.create", plugin.settings().createPermission()),
                new HelpEntry("/clan add <player>", "help.entries.add", "clans.use"),
                new HelpEntry("/clan accept <tag>", "help.entries.accept", "clans.use"),
                new HelpEntry("/clan deny <tag>", "help.entries.deny", "clans.use"),
                new HelpEntry("/clan invites", "help.entries.invites", "clans.use"),
                new HelpEntry("/clan info [tag]", "help.entries.info", "clans.use"),
                new HelpEntry("/clan list [page]", "help.entries.list", "clans.use"),
                new HelpEntry("/clan members", "help.entries.members", "clans.use"),
                new HelpEntry("/clan leave", "help.entries.leave", "clans.use"),
                new HelpEntry("/clan kick <player>", "help.entries.kick", "clans.use"),
                new HelpEntry("/clan transfer <player>", "help.entries.transfer", "clans.use"),
                new HelpEntry("/clan disband", "help.entries.disband", "clans.use"),
                new HelpEntry("/clan chat", "help.entries.chat", "clans.chat"),
                new HelpEntry("/cc <message>", "help.entries.cc", "clans.chat"),
                new HelpEntry("/clan ally <tag>", "help.entries.ally", "clans.use"),
                new HelpEntry("/clan unally <tag>", "help.entries.unally", "clans.use"),
                new HelpEntry("/clan enemy <tag>", "help.entries.enemy", "clans.use"),
                new HelpEntry("/clan unenemy <tag>", "help.entries.unenemy", "clans.use"),
                new HelpEntry("/clan bank", "help.entries.bank", "clans.use"),
                new HelpEntry("/clan deposit <amount>", "help.entries.deposit", "clans.use"),
                new HelpEntry("/clan withdraw <amount>", "help.entries.withdraw", "clans.use"),
                new HelpEntry("/clan glow", "help.entries.glow", "clans.use"),
                new HelpEntry("/clan menu", "help.entries.menu", "clans.use"),
                new HelpEntry("/clan help", "help.entries.help", "clans.use"),
                new HelpEntry("/clan reload", "help.entries.reload", "clans.admin"),
        };
        for (HelpEntry entry : entries) {
            if (player.hasPermission(entry.permission())) {
                send(player, locale, "help.line",
                        Placeholder.unparsed("command", entry.command()),
                        Placeholder.parsed("desc", plugin.lang().renderPlain(locale, entry.descKey())));
            }
        }
        return 1;
    }

    private record HelpEntry(String command, String descKey, String permission) {
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadEverything();
        plugin.lang().send(ctx.getSource().getSender(), plugin.lang().defaultLocale(), "general.reload-success");
        return 1;
    }

    // ---- suggestions --------------------------------------------------------

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

    private SuggestionProvider<CommandSourceStack> clanMemberNames() {
        return (ctx, builder) -> {
            if (ctx.getSource().getSender() instanceof Player player) {
                plugin.clans().getClanOf(player.getUniqueId()).ifPresent(clan -> {
                    String remaining = builder.getRemainingLowerCase();
                    for (ClanMember member : clan.members()) {
                        if (member.lastName() != null
                                && member.lastName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                            builder.suggest(member.lastName());
                        }
                    }
                });
            }
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> invitablePlayers() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!plugin.clans().isInClan(online.getUniqueId())
                        && online.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(online.getName());
                }
            }
            return builder.buildFuture();
        };
    }

    // ---- helpers ------------------------------------------------------------

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> acceptLiteral() {
        return Commands.literal("accept")
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(clanTags())
                        .executes(this::accept));
    }

    private Optional<Clan> resolveClan(String token) {
        Optional<Clan> byTag = plugin.clans().getClanByTag(token);
        return byTag.isPresent() ? byTag : plugin.clans().getClanByName(token);
    }

    private boolean belowMemberLimit(Clan clan) {
        int max = plugin.settings().maxMembers();
        return max < 0 || clan.memberCount() < max;
    }

    private ClanMember findMemberByName(Clan clan, String name) {
        for (ClanMember member : clan.members()) {
            if (name.equalsIgnoreCase(member.lastName())) {
                return member;
            }
        }
        return null;
    }

    private String memberName(Clan clan, UUID uuid) {
        ClanMember member = clan.member(uuid);
        if (member != null && member.lastName() != null) {
            return member.lastName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString();
    }

    private boolean isOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }

    private Player player(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            return player;
        }
        plugin.lang().send(ctx.getSource().getSender(), plugin.lang().defaultLocale(), "general.players-only");
        return null;
    }

    private void msg(Player player, String key, TagResolver... resolvers) {
        plugin.lang().send(player, plugin.lang().localeFor(player), key, resolvers);
    }

    private void send(Player player, String locale, String key, TagResolver... resolvers) {
        plugin.lang().send(player, locale, key, resolvers);
    }
}
