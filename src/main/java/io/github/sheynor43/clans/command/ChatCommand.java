package io.github.sheynor43.clans.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.sheynor43.clans.ClansPlugin;
import io.github.sheynor43.clans.model.Clan;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.Optional;

/** {@code /cc <message>} — send a single clan-chat message. */
public final class ChatCommand {

    private final ClansPlugin plugin;

    public ChatCommand(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("cc")
                .requires(src -> src.getSender().hasPermission("clans.chat"))
                .executes(ctx -> usage(ctx))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(this::send))
                .build();
    }

    private int usage(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            plugin.lang().send(player, plugin.lang().localeFor(player), "chat.no-message");
        }
        return 1;
    }

    private int send(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            plugin.lang().send(ctx.getSource().getSender(), plugin.lang().defaultLocale(), "general.players-only");
            return 1;
        }
        Optional<Clan> clan = plugin.clans().getClanOf(player.getUniqueId());
        if (clan.isEmpty()) {
            plugin.lang().send(player, plugin.lang().localeFor(player), "clan.not-in-clan");
            return 1;
        }
        String message = StringArgumentType.getString(ctx, "message");
        plugin.clanChat().send(player, clan.get(), message);
        return 1;
    }
}
