package com.italiarevenge.iROrders.inventory;

import com.italiarevenge.iROrders.model.Order;
import com.italiarevenge.iROrders.validation.ItemValidator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimised O(n) inventory scanning with early exit.
 * All methods run on the main thread.
 */
public class InventoryService {

    private final ItemValidator validator;

    public InventoryService(ItemValidator validator) {
        this.validator = validator;
    }

    /**
     * Counts how many matching items the player holds.
     * Stops counting as soon as it reaches `cap` (for fast "has enough" checks).
     */
    public int countItems(PlayerInventory inv, Order order, int cap) {
        int total = 0;
        for (ItemStack item : inv.getStorageContents()) {
            if (item == null) continue;
            if (!validator.isValidForOrder(item, order)) continue;
            total += item.getAmount();
            if (total >= cap) return total; // early exit
        }
        return total;
    }

    /**
     * Atomically removes exactly `amount` matching items from the inventory.
     * Returns true on success; does NOT modify the inventory if the player
     * does not have enough items.
     */
    public boolean removeItems(PlayerInventory inv, Order order, int amount) {
        // --- Phase 1: collect slots with matching items ---
        List<int[]> slots = new ArrayList<>(); // [slotIndex, availableAmount]
        int found = 0;

        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            if (!validator.isValidForOrder(item, order)) continue;
            int qty = item.getAmount();
            slots.add(new int[]{i, qty});
            found += qty;
            if (found >= amount) break; // early exit
        }

        if (found < amount) return false; // not enough items

        // --- Phase 2: remove from collected slots ---
        int remaining = amount;
        for (int[] slot : slots) {
            int idx = slot[0];
            int qty = slot[1];
            if (qty <= remaining) {
                contents[idx] = null;
                remaining -= qty;
            } else {
                contents[idx] = contents[idx].clone();
                contents[idx].setAmount(qty - remaining);
                remaining = 0;
            }
            if (remaining == 0) break;
        }

        inv.setStorageContents(contents);
        return true;
    }
}
