package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.backpack.BackpackManager;
import com.italiarevenge.iROrders.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

/**
 * Virtual 54-slot backpack (double chest).
 * Read-only for the owner — items can only be TAKEN, not inserted.
 *
 * Each slot mirrors the DB; clicking an item gives it to the player's
 * real inventory (or drops it at their feet if full).
 */
public class BackpackGUI {

    private final IROrders plugin;
    private final BackpackManager backpackManager;

    public BackpackGUI(IROrders plugin, BackpackManager backpackManager) {
        this.plugin = plugin;
        this.backpackManager = backpackManager;
    }

    public void open(Player player) {
        Map<Integer, ItemStack> contents =
                backpackManager.getContents(player.getUniqueId());

        int used = backpackManager.getUsedSlots(player.getUniqueId());
        GuiHolder holder = new GuiHolder(GuiType.BACKPACK);
        Inventory inv = Bukkit.createInventory(holder,
                BackpackManager.BACKPACK_SIZE,
                Component.text("📦 Backpack  [" + used + "/" + BackpackManager.BACKPACK_SIZE + "]",
                        NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.BOLD, true));
        holder.setInventory(inv);

        ItemStack filler = ItemUtil.filler();

        for (int slot = 0; slot < BackpackManager.BACKPACK_SIZE; slot++) {
            if (contents.containsKey(slot)) {
                ItemStack item = contents.get(slot).clone();
                decorateBackpackItem(item);
                inv.setItem(slot, item);
            } else {
                inv.setItem(slot, filler);
            }
        }

        player.openInventory(inv);
    }

    /**
     * Adds a lore hint to items displayed in the backpack GUI.
     * Does NOT modify the actual stored item — operates on the clone.
     */
    private void decorateBackpackItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore() != null
                ? new java.util.ArrayList<>(meta.lore()) : new java.util.ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("  ► Click to take", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        // Mark as backpack display item so listener knows slot origin
        meta.getPersistentDataContainer().set(
                plugin.getItemIdKey(),
                PersistentDataType.STRING,
                "backpack_display");

        meta.lore(lore);
        item.setItemMeta(meta);
    }
}
