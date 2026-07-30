package io.github.sheynor43.clans.gui;

import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.model.ClanMember;
import io.github.sheynor43.clans.util.ClanPlaceholders;
import io.github.sheynor43.clans.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Paginated member list. Leaders can kick (left-click) or transfer (right-click) with confirmation. */
public final class MembersMenu extends Menu {

    private static final int PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;

    private final Clan clan;
    private final int page;
    private final Map<Integer, UUID> slotToMember = new HashMap<>();
    private List<ClanMember> members;

    public MembersMenu(MenuManager manager, Player viewer, Clan clan, int page) {
        super(manager, viewer);
        this.clan = clan;
        this.page = Math.max(0, page);
    }

    @Override
    protected void build() {
        create(6, manager.plugin().lang().render(locale, "gui.members-title",
                Placeholder.unparsed("page", String.valueOf(page + 1))));
        slotToMember.clear();
        members = new ArrayList<>(clan.members());

        int from = page * PER_PAGE;
        int slot = 0;
        for (int i = from; i < members.size() && slot < PER_PAGE; i++, slot++) {
            ClanMember member = members.get(i);
            inventory.setItem(slot, memberItem(member));
            slotToMember.put(slot, member.uuid());
        }

        var lang = manager.plugin().lang();
        if (page > 0) {
            inventory.setItem(SLOT_PREV, Items.of(Material.ARROW, lang.render(locale, "gui.item.prev-page")));
        }
        inventory.setItem(SLOT_BACK, Items.of(Material.BARRIER, lang.render(locale, "gui.item.back-name")));
        if (from + PER_PAGE < members.size()) {
            inventory.setItem(SLOT_NEXT, Items.of(Material.ARROW, lang.render(locale, "gui.item.next-page")));
        }
    }

    private org.bukkit.inventory.ItemStack memberItem(ClanMember member) {
        var lang = manager.plugin().lang();
        boolean online = isOnline(member.uuid());
        String nameKey = online ? "gui.item.member-online-name" : "gui.item.member-offline-name";
        Component name = lang.render(locale, nameKey, Placeholder.unparsed("player", String.valueOf(member.lastName())));
        boolean leaderViewer = clan.isLeader(viewer.getUniqueId()) && !member.uuid().equals(viewer.getUniqueId());
        String loreKey = leaderViewer ? "gui.item.member-lore-leader" : "gui.item.member-lore-plain";
        List<Component> lore = lang.renderList(locale, loreKey, Placeholder.unparsed("role", member.role().name()));
        return Items.of(online ? Material.PLAYER_HEAD : Material.SKELETON_SKULL, name, lore);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == SLOT_BACK) {
            manager.openMain(viewer);
            return;
        }
        if (slot == SLOT_PREV && page > 0) {
            manager.openMembers(viewer, clan, page - 1);
            return;
        }
        if (slot == SLOT_NEXT) {
            manager.openMembers(viewer, clan, page + 1);
            return;
        }
        UUID target = slotToMember.get(slot);
        if (target == null || !clan.isLeader(viewer.getUniqueId()) || target.equals(viewer.getUniqueId())) {
            return;
        }
        if (event.isLeftClick()) {
            confirmKick(target);
        } else if (event.isRightClick()) {
            confirmTransfer(target);
        }
    }

    private void confirmKick(UUID target) {
        manager.openConfirm(viewer, clan, "kick", () -> {
            ClanMember member = clan.member(target);
            if (member == null) {
                manager.openMembers(viewer, clan, page);
                return;
            }
            manager.plugin().clans().removeMember(clan, target, true);
            manager.plugin().broadcaster().broadcast(clan, "clan.member-kicked",
                    Placeholder.unparsed("player", String.valueOf(member.lastName())));
            Player online = Bukkit.getPlayer(target);
            if (online != null) {
                manager.plugin().lang().send(online, manager.plugin().lang().localeFor(online),
                        "clan.you-were-kicked", ClanPlaceholders.of(clan, manager.plugin().settings()));
                manager.plugin().tab().apply(online);
                online.setGlowing(false);
            }
            manager.openMembers(viewer, clan, page);
        }, () -> manager.openMembers(viewer, clan, page));
    }

    private void confirmTransfer(UUID target) {
        manager.openConfirm(viewer, clan, "transfer", () -> {
            ClanMember member = clan.member(target);
            if (member == null) {
                manager.openMembers(viewer, clan, page);
                return;
            }
            manager.plugin().clans().transferLeadership(clan, target);
            manager.plugin().lang().send(viewer, locale, "clan.transferred",
                    Placeholder.unparsed("player", String.valueOf(member.lastName())));
            Player online = Bukkit.getPlayer(target);
            if (online != null) {
                manager.plugin().lang().send(online, manager.plugin().lang().localeFor(online),
                        "clan.transfer-received", ClanPlaceholders.of(clan, manager.plugin().settings()));
            }
            viewer.closeInventory();
        }, () -> manager.openMembers(viewer, clan, page));
    }

    private boolean isOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }
}
