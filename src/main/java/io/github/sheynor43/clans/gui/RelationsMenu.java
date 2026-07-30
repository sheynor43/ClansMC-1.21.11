package io.github.sheynor43.clans.gui;

import io.github.sheynor43.clans.api.RelationStatus;
import io.github.sheynor43.clans.api.RelationType;
import io.github.sheynor43.clans.model.Clan;
import io.github.sheynor43.clans.util.Items;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Read-only view of allies (top) and enemies (bottom). */
public final class RelationsMenu extends Menu {

    private static final int SLOT_BACK = 49;

    private final Clan clan;

    public RelationsMenu(MenuManager manager, Player viewer, Clan clan) {
        super(manager, viewer);
        this.clan = clan;
    }

    @Override
    protected void build() {
        create(6, manager.plugin().lang().render(locale, "gui.relations-title"));
        var lang = manager.plugin().lang();
        var settings = manager.plugin().settings();

        int allySlot = 0;
        int enemySlot = 27;
        for (var relation : clan.relations()) {
            if (relation.status() != RelationStatus.ACTIVE) {
                continue;
            }
            Clan other = manager.plugin().clans().getClanById(relation.otherClanId()).orElse(null);
            if (other == null) {
                continue;
            }
            if (relation.type() == RelationType.ALLY && allySlot < 27) {
                inventory.setItem(allySlot++, Items.of(Material.LIME_WOOL,
                        lang.render(locale, "gui.item.ally-name", Placeholder.unparsed("clan", other.name()))));
            } else if (relation.type() == RelationType.ENEMY && enemySlot < 45) {
                inventory.setItem(enemySlot++, Items.of(Material.RED_WOOL,
                        lang.render(locale, "gui.item.enemy-name", Placeholder.unparsed("clan", other.name()))));
            }
        }
        inventory.setItem(SLOT_BACK, Items.of(Material.BARRIER, lang.render(locale, "gui.item.back-name")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getSlot() == SLOT_BACK) {
            manager.openMain(viewer);
        }
    }
}
