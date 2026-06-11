package com.italiarevenge.iROrders.validation;

import com.italiarevenge.iROrders.model.Order;
import com.italiarevenge.iROrders.util.PDCUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

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
     * 1. PDC item_id must match.
     * 2. If strictEnchants, enchants must match exactly.
     */
    public boolean isValidForOrder(ItemStack item, Order order) {
        if (!hasCorrectId(item, order.getItemId())) return false;
        if (order.isStrictEnchants()) {
            Map<String, Integer> required = order.getRequiredEnchants();
            return enchantValidator.enchantsMatch(item, required);
        }
        return true;
    }
}
