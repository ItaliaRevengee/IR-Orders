package com.italiarevenge.iROrders.validation;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Strict enchantment validation:
 * the item must carry EXACTLY the required enchants—no more, no less.
 */
public class EnchantValidator {

    /**
     * Returns the enchantments on an item as a Map<enchant-key-string, level>.
     * Handles both regular items and enchanted books.
     */
    public Map<String, Integer> getEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Map.of();
        ItemMeta meta = item.getItemMeta();
        Map<String, Integer> result = new HashMap<>();

        if (meta instanceof EnchantmentStorageMeta esm) {
            esm.getStoredEnchants().forEach((e, lvl) ->
                    result.put(e.getKey().toString(), lvl));
        } else {
            meta.getEnchants().forEach((e, lvl) ->
                    result.put(e.getKey().toString(), lvl));
        }
        return result;
    }

    /**
     * Returns true if the item's enchants exactly equal the required map.
     * An empty required map means: item must have NO enchants.
     */
    public boolean enchantsMatch(ItemStack item, Map<String, Integer> required) {
        return getEnchants(item).equals(required);
    }
}
