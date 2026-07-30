package io.github.sheynor43.clans.gui;

import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.util.ClanPlaceholders;
import io.github.sheynor43.clans.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Leader-only settings: toggle glow, disband. */
public final class SettingsMenu extends Menu {

    private static final int SLOT_GLOW = 11;
    private static final int SLOT_DISBAND = 15;
    private static final int SLOT_BACK = 22;

    private final Clan clan;

    public SettingsMenu(MenuManager manager, Player viewer, Clan clan) {
        super(manager, viewer);
        this.clan = clan;
    }

    @Override
    protected void build() {
        create(3, manager.plugin().lang().render(locale, "gui.settings-title"));
        var lang = manager.plugin().lang();
        boolean leader = clan.isLeader(viewer.getUniqueId());

        if (leader && manager.plugin().glow().isModuleEnabled()
                && manager.plugin().clans().perksFor(clan).glowUnlocked()) {
            inventory.setItem(SLOT_GLOW, Items.of(Material.GLOWSTONE_DUST, lang.render(locale, "gui.item.glow-name")));
        }
        if (leader) {
            inventory.setItem(SLOT_DISBAND, Items.of(Material.TNT, lang.render(locale, "gui.item.disband-name")));
        }
        inventory.setItem(SLOT_BACK, Items.of(Material.BARRIER, lang.render(locale, "gui.item.back-name")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case SLOT_BACK -> manager.openMain(viewer);
            case SLOT_GLOW -> {
                if (clan.isLeader(viewer.getUniqueId()) && manager.plugin().glow().isModuleEnabled()
                        && manager.plugin().clans().perksFor(clan).glowUnlocked()) {
                    boolean on = manager.plugin().glow().toggle(clan);
                    manager.plugin().lang().send(viewer, locale, on ? "glow.enabled" : "glow.disabled-now");
                    manager.openSettings(viewer, clan);
                }
            }
            case SLOT_DISBAND -> {
                if (clan.isLeader(viewer.getUniqueId())) {
                    manager.openConfirm(viewer, clan, "disband", this::doDisband,
                            () -> manager.openSettings(viewer, clan));
                }
            }
            default -> { /* decorative */ }
        }
    }

    private void doDisband() {
        var plugin = manager.plugin();
        plugin.broadcaster().broadcast(clan, "clan.disbanded", ClanPlaceholders.of(clan, plugin.settings()));
        for (var member : clan.members()) {
            Player online = Bukkit.getPlayer(member.uuid());
            if (online != null) {
                online.setGlowing(false);
            }
        }
        plugin.invites().clearClan(clan.id());
        plugin.allyRequests().clearClan(clan.id());
        plugin.clans().disband(clan);
        plugin.tab().applyAll();
        viewer.closeInventory();
    }
}
