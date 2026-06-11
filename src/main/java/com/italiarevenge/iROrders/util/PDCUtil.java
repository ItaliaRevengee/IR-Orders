package com.italiarevenge.iROrders.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PDCUtil {

    private PDCUtil() {}

    /** Returns the item_id stored in the item's PDC, or null if absent. */
    public static String getItemId(ItemStack item, NamespacedKey itemIdKey) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    /** Sets the item_id in the item's PDC. */
    public static void setItemId(ItemStack item, NamespacedKey itemIdKey, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    /** Returns true if the item has the given item_id in its PDC. */
    public static boolean hasItemId(ItemStack item, NamespacedKey itemIdKey, String expectedId) {
        String actual = getItemId(item, itemIdKey);
        return expectedId.equals(actual);
    }
}
