package io.github.sheynor43.clans.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all clan GUIs. Acts as its own {@link InventoryHolder} so the
 * {@link MenuManager} can identify and route events without external state.
 */
public abstract class Menu implements InventoryHolder {

    protected final MenuManager manager;
    protected final Player viewer;
    protected final String locale;
    protected Inventory inventory;

    protected Menu(MenuManager manager, Player viewer) {
        this.manager = manager;
        this.viewer = viewer;
        this.locale = manager.plugin().lang().localeFor(viewer);
    }

    protected Inventory create(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        return this.inventory;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        build();
        viewer.openInventory(inventory);
    }

    /** Populates {@link #inventory}. */
    protected abstract void build();

    /** Handles a click inside this menu's top inventory (already cancelled). */
    public abstract void handleClick(InventoryClickEvent event);
}
