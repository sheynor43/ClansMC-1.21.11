package io.github.sheynor43.clans.gui;

import io.github.sheynor43.clans.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Generic yes/no confirmation for irreversible actions (kick, transfer, disband). */
public final class ConfirmMenu extends Menu {

    private static final int SLOT_YES = 11;
    private static final int SLOT_NO = 15;

    private final String actionName;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmMenu(MenuManager manager, Player viewer, String actionName,
                       Runnable onConfirm, Runnable onCancel) {
        super(manager, viewer);
        this.actionName = actionName;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void build() {
        create(3, manager.plugin().lang().render(locale, "gui.confirm-title"));
        var lang = manager.plugin().lang();
        inventory.setItem(SLOT_YES, Items.of(Material.GREEN_WOOL, lang.render(locale, "gui.item.confirm-yes-name")));
        inventory.setItem(SLOT_NO, Items.of(Material.RED_WOOL, lang.render(locale, "gui.item.confirm-no-name")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getSlot() == SLOT_YES) {
            onConfirm.run();
        } else if (event.getSlot() == SLOT_NO) {
            onCancel.run();
        }
    }

    public String actionName() {
        return actionName;
    }
}
