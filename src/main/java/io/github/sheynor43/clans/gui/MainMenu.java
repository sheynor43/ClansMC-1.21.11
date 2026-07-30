package io.github.sheynor43.clans.gui;

import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.util.ClanPlaceholders;
import io.github.sheynor43.clans.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Root clan menu: info, members, relations, bank, settings. */
public final class MainMenu extends Menu {

    private static final int SLOT_INFO = 4;
    private static final int SLOT_MEMBERS = 10;
    private static final int SLOT_RELATIONS = 12;
    private static final int SLOT_BANK = 14;
    private static final int SLOT_SETTINGS = 16;
    private static final int SLOT_CLOSE = 22;

    private final Clan clan;

    public MainMenu(MenuManager manager, org.bukkit.entity.Player viewer, Clan clan) {
        super(manager, viewer);
        this.clan = clan;
    }

    @Override
    protected void build() {
        create(3, name("gui.main-title"));
        var lang = manager.plugin().lang();
        var settings = manager.plugin().settings();
        TagResolver base = ClanPlaceholders.of(clan, settings);

        inventory.setItem(SLOT_INFO, Items.of(Material.NETHER_STAR,
                lang.render(locale, "gui.item.info-name", base),
                lang.renderList(locale, "gui.item.info-lore",
                        Placeholder.unparsed("leader", leaderName()),
                        Placeholder.unparsed("level", String.valueOf(clan.level())),
                        Placeholder.unparsed("members", String.valueOf(clan.memberCount())))));
        inventory.setItem(SLOT_MEMBERS, Items.of(Material.PLAYER_HEAD,
                name("gui.item.members-name"), lang.renderList(locale, "gui.item.members-lore")));
        inventory.setItem(SLOT_RELATIONS, Items.of(Material.DIAMOND_SWORD,
                name("gui.item.relations-name"), lang.renderList(locale, "gui.item.relations-lore")));
        if (manager.plugin().bank().isEnabled()) {
            inventory.setItem(SLOT_BANK, Items.of(Material.GOLD_INGOT,
                    name("gui.item.bank-name"),
                    lang.renderList(locale, "gui.item.bank-lore",
                            Placeholder.unparsed("amount", manager.plugin().bank().format(clan.balance())))));
        }
        inventory.setItem(SLOT_SETTINGS, Items.of(Material.COMPARATOR,
                name("gui.item.settings-name"), lang.renderList(locale, "gui.item.settings-lore")));
        inventory.setItem(SLOT_CLOSE, Items.of(Material.BARRIER, name("gui.item.close-name")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case SLOT_MEMBERS -> manager.openMembers(viewer, clan, 0);
            case SLOT_RELATIONS -> manager.openRelations(viewer, clan);
            case SLOT_BANK -> {
                if (manager.plugin().bank().isEnabled()) {
                    manager.openBank(viewer, clan);
                }
            }
            case SLOT_SETTINGS -> manager.openSettings(viewer, clan);
            case SLOT_CLOSE -> viewer.closeInventory();
            default -> { /* decorative */ }
        }
    }

    private String leaderName() {
        var member = clan.member(clan.leader());
        return member != null && member.lastName() != null ? member.lastName() : "?";
    }

    private Component name(String key) {
        return manager.plugin().lang().render(locale, key);
    }
}
