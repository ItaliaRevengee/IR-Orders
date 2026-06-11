package com.italiarevenge.iROrders.validation;

import com.italiarevenge.iROrders.model.Order;
import com.italiarevenge.iROrders.util.PDCUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;


/**
 * Validates items against orders using PDC item_id + optional strict enchants.
 * Display name, lore, and rename are explicitly ignored.
 */
public class ItemValidator {

    private final NamespacedKey itemIdKey;
    private final EnchantValidator enchantValidator;

    public ItemValidator(NamespacedKey itemIdKey, EnchantValidator enchantValidator) {
        this.itemIdKey = itemIdKey;
        this.enchantValidator = enchantValidator;
    }

    /**
     * Returns true if the item has the correct PDC item_id.
     * No enchant check here.
     */
    public boolean hasCorrectId(ItemStack item, String requiredItemId) {
        return PDCUtil.hasItemId(item, itemIdKey, requiredItemId);
    }

    /**
     * Full order validation:
     * - "mat:MATERIAL" orders match any item of that material (no PDC required).
     * - All other orders require a matching PDC item_id, plus strict enchants if set.
     */
    public boolean isValidForOrder(ItemStack item, Order order) {
        String itemId = order.getItemId();
        if (itemId.startsWith("mat:")) {
            try {
                return item.getType() == Material.valueOf(itemId.substring(4));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        if (!hasCorrectId(item, itemId)) return false;
        if (order.isStrictEnchants()) {
            return enchantValidator.enchantsMatch(item, order.getRequiredEnchants());
        }
        return true;
    }
}
