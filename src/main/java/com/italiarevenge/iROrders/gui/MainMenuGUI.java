package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
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

public class MainMenuGUI {

    private final IROrders plugin;

    public MainMenuGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.MAIN_MENU);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("✦ IR-Orders Marketplace", NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.BOLD, true));
        holder.setInventory(inv);

        // ── Border ────────────────────────────────────────────────────────────
        ItemStack filler = ItemUtil.filler();
        for (int i : new int[]{0,1,2,3,4,5,6,7,8, 18,19,20,21,22,23,24,25,26}) {
            inv.setItem(i, filler);
        }

        // ── Create Order button ───────────────────────────────────────────────
        inv.setItem(10, ItemUtil.build(Material.EMERALD_BLOCK,
                Component.text("§a§l📦 Create Buy Order")
                        .decoration(TextDecoration.ITALIC, false),
                List.of(
                        ItemUtil.gray("Browse the catalog and place a"),
                        ItemUtil.gray("buy order for any item."),
                        Component.empty(),
                        Component.text("  ► Click to open catalog",
                                NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                )));

        // ── My Orders button ──────────────────────────────────────────────────
        inv.setItem(13, ItemUtil.build(Material.WRITTEN_BOOK,
                Component.text("§e§l📋 My Orders")
                        .decoration(TextDecoration.ITALIC, false),
                List.of(
                        ItemUtil.gray("View and manage your active"),
                        ItemUtil.gray("buy orders."),
                        Component.empty(),
                        Component.text("  ► Click to open",
                                NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                )));

        // ── Global Market button ──────────────────────────────────────────────
        inv.setItem(16, ItemUtil.build(Material.GOLD_BLOCK,
                Component.text("§6§l🌍 Global Market")
                        .decoration(TextDecoration.ITALIC, false),
                List.of(
                        ItemUtil.gray("Browse all active buy orders"),
                        ItemUtil.gray("and sell your items."),
                        Component.empty(),
                        Component.text("  ► Click to open",
                                NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                )));

        player.openInventory(inv);
    }
}
