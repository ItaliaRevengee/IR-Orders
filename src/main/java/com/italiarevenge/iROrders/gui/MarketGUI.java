package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.model.Order;
import com.italiarevenge.iROrders.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Global marketplace — shows all active buy orders.
 * Sellers right-click an order to fulfill it.
 *
 * Layout: rows 0-4 = orders, row 5 = navigation.
 */
public class MarketGUI {

    private static final int ORDERS_PER_PAGE = 45;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.systemDefault());

    private final IROrders plugin;

    public MarketGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, SessionData session) {
        Collection<Order> all = plugin.getOrderManager().getActiveOrders();
        // Sort by newest first, own orders last so sellers see others' orders at top
        List<Order> sorted = all.stream()
                .sorted(Comparator.comparingLong(Order::getCreatedAt).reversed())
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil(sorted.size() / (double) ORDERS_PER_PAGE));
        int page = Math.min(session.getMarketPage(), totalPages - 1);
        session.setMarketPage(page);

        GuiHolder holder = new GuiHolder(GuiType.MARKET, session);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("🌍 Global Market", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
        holder.setInventory(inv);

        ItemStack filler = ItemUtil.filler();
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        int start = page * ORDERS_PER_PAGE;
        int end   = Math.min(start + ORDERS_PER_PAGE, sorted.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildOrderItem(sorted.get(i), player));
        }
        for (int slot = end - start; slot < 45; slot++) {
            inv.setItem(slot, filler);
        }

        if (sorted.isEmpty()) {
            inv.setItem(22, ItemUtil.build(Material.COMPASS,
                    Component.text("No active orders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(ItemUtil.gray("Be the first to create one!"))));
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, ItemUtil.build(Material.ARROW,
                    Component.text("<- Previous Page", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    List.of()));
        }
        inv.setItem(49, ItemUtil.build(Material.PAPER,
                Component.text("Page " + (page + 1) + " / " + totalPages, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(ItemUtil.gray(sorted.size() + " total orders"))));
        if (page < totalPages - 1) {
            inv.setItem(53, ItemUtil.build(Material.ARROW,
                    Component.text("Next Page ->", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    List.of()));
        }

        inv.setItem(47, ItemUtil.build(Material.BARRIER,
                Component.text("<- Back to Menu", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                List.of(ItemUtil.gray("Return to main menu"))));

        player.openInventory(inv);
    }

    private ItemStack buildOrderItem(Order order, Player viewer) {
        ItemStack item = new ItemStack(order.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        boolean isOwn = order.getBuyerUUID().equals(viewer.getUniqueId());
        NamedTextColor nameColor = isOwn ? NamedTextColor.GRAY : NamedTextColor.AQUA;

        meta.displayName(Component.text(order.getMaterial().name(), nameColor)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        OfflinePlayer buyer = Bukkit.getOfflinePlayer(order.getBuyerUUID());
        String buyerName = buyer.getName() != null ? buyer.getName() : "Unknown";

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Buyer: ", NamedTextColor.GRAY)
                .append(Component.text(buyerName, NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Quantity: ", NamedTextColor.GRAY)
                .append(Component.text(order.getQuantity() + "x", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Pays: ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getEconomyManager().format(order.getPrice()),
                        NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Item ID: ", NamedTextColor.GRAY)
                .append(Component.text(order.getItemId(), NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        if (order.isStrictEnchants()) {
            lore.add(Component.text("⚠ Strict enchants required", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            order.getRequiredEnchants().forEach((key, lvl) ->
                    lore.add(Component.text("  " + key + " " + lvl, NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)));
        }
        lore.add(Component.text("Created: ", NamedTextColor.GRAY)
                .append(Component.text(FMT.format(Instant.ofEpochMilli(order.getCreatedAt())),
                        NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        if (isOwn) {
            lore.add(Component.text("  (Your order)", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            lore.add(Component.text("  ► Right-click to SELL", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        // Store order UUID so click handler can retrieve it
        meta.getPersistentDataContainer().set(
                plugin.getItemIdKey(),
                org.bukkit.persistence.PersistentDataType.STRING,
                "order:" + order.getId().toString());

        item.setItemMeta(meta);
        return item;
    }
}
