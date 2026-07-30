package io.github.sheynor43.clans.gui;

import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.util.Items;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Read-only bank balance display. Deposits/withdrawals use the commands. */
public final class BankMenu extends Menu {

    private static final int SLOT_INFO = 13;
    private static final int SLOT_BACK = 22;

    private final Clan clan;

    public BankMenu(MenuManager manager, Player viewer, Clan clan) {
        super(manager, viewer);
        this.clan = clan;
    }

    @Override
    protected void build() {
        create(3, manager.plugin().lang().render(locale, "gui.bank-title"));
        var lang = manager.plugin().lang();
        var bank = manager.plugin().bank();
        inventory.setItem(SLOT_INFO, Items.of(Material.GOLD_INGOT,
                lang.render(locale, "gui.item.bank-name"),
                lang.renderList(locale, "gui.item.bank-lore",
                        Placeholder.unparsed("amount", bank.format(clan.balance())))));
        inventory.setItem(SLOT_BACK, Items.of(Material.BARRIER, lang.render(locale, "gui.item.back-name")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getSlot() == SLOT_BACK) {
            manager.openMain(viewer);
        }
    }
}
