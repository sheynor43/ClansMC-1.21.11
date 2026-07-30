package io.github.sheynor43.clans.gui;

import io.github.sheynor43.clans.ClansPlugin;
import io.github.sheynor43.clans.model.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Optional;

/**
 * Opens clan GUIs and routes inventory events. Every menu is its own
 * {@link org.bukkit.inventory.InventoryHolder}, so clicks are identified by the
 * holder type; item movement and dragging are always cancelled.
 */
public final class MenuManager implements Listener {

    private final ClansPlugin plugin;

    public MenuManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public ClansPlugin plugin() {
        return plugin;
    }

    public void openMain(Player player) {
        Optional<Clan> clan = plugin.clans().getClanOf(player.getUniqueId());
        if (clan.isEmpty()) {
            plugin.lang().send(player, plugin.lang().localeFor(player), "clan.not-in-clan");
            return;
        }
        new MainMenu(this, player, clan.get()).open();
    }

    public void openMembers(Player player, Clan clan, int page) {
        new MembersMenu(this, player, clan, page).open();
    }

    public void openRelations(Player player, Clan clan) {
        new RelationsMenu(this, player, clan).open();
    }

    public void openBank(Player player, Clan clan) {
        new BankMenu(this, player, clan).open();
    }

    public void openSettings(Player player, Clan clan) {
        new SettingsMenu(this, player, clan).open();
    }

    public void openConfirm(Player player, Clan clan, String actionName, Runnable onConfirm, Runnable onCancel) {
        new ConfirmMenu(this, player, actionName, onConfirm, onCancel).open();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            event.setCancelled(true);
            // Only route clicks inside the menu itself, not the player's inventory.
            if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                menu.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
