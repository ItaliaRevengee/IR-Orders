package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.model.CatalogItem;
import com.italiarevenge.iROrders.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 3-row GUI for selecting quantity (1 / 8 / 16 / 32 / 64).
 */
public class QuantityGUI {

    private static final int[] QUANTITIES   = {1, 8, 16, 32, 64};
    private static final int[] QTY_SLOTS    = {11, 12, 13, 14, 15};
    private static final Material[] QTY_MAT = {
            Material.WHITE_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE
    };

    private final IROrders plugin;

    public QuantityGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, SessionData session) {
        CatalogItem ci = session.getSelectedItem();
        if (ci == null) return;

        GuiHolder holder = new GuiHolder(GuiType.QUANTITY, session);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Select Quantity — " + stripColor(ci.getDisplayName()),
                        NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.BOLD, true));
        holder.setInventory(inv);

        ItemStack filler = ItemUtil.filler();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Quantity buttons
        for (int i = 0; i < QUANTITIES.length; i++) {
            int qty = QUANTITIES[i];
            inv.setItem(QTY_SLOTS[i], ItemUtil.build(QTY_MAT[i],
                    Component.text("§l" + qty + "x").decoration(TextDecoration.ITALIC, false),
                    List.of(
                            ItemUtil.gray("Order " + qty + "x"),
                            ItemUtil.legacy(ci.getDisplayName())
                                    .decoration(TextDecoration.ITALIC, false),
                            Component.empty(),
                            Component.text("  ► Click to select", NamedTextColor.GREEN)
                                    .decoration(TextDecoration.ITALIC, false)
                    )));
        }

        // Back button
        inv.setItem(18, ItemUtil.build(Material.BARRIER,
                Component.text("§c← Back to Catalog").decoration(TextDecoration.ITALIC, false),
                List.of(ItemUtil.gray("Return to item catalog"))));

        player.openInventory(inv);
    }

    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
