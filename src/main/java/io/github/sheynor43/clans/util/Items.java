package io.github.sheynor43.clans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Builds GUI items with MiniMessage names/lore and no forced italics. */
public final class Items {

    private Items() {
    }

    public static ItemStack of(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
            }
        });
        return item;
    }

    public static ItemStack of(Material material, Component name) {
        return of(material, name, null);
    }
}
