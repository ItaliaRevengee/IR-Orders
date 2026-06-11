package com.italiarevenge.iROrders.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private ItemUtil() {}

    /**
     * Creates a GUI filler pane that cannot be taken.
     */
    public static ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /**
     * Builds a simple display item with a legacy-color display name and lore lines.
     */
    public static ItemStack build(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(displayName));
        if (lore.length > 0) {
            List<Component> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(LEGACY.deserialize(line));
            }
            meta.lore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds a display item with a Component name and Component lore lines.
     */
    public static ItemStack build(Material material, Component displayName, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Converts a legacy-color string (§ codes) to an Adventure Component.
     */
    public static Component legacy(String text) {
        return LEGACY.deserialize(text);
    }

    /**
     * Returns a gray Component.
     */
    public static Component gray(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }
}
