package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.model.Order;
import com.italiarevenge.iROrders.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shows the player's own active buy orders with cancel option.
 * Layout: rows 0-4 = orders (5×9 = 45 slots), row 5 = navigation.
 */
public class MyOrdersGUI {

    private static final int ORDERS_PER_PAGE = 45;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.systemDefault());

    private final IROrders plugin;

    public MyOrdersGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, SessionData session) {
        List<Order> orders = plugin.getOrderManager().getPlayerOrders(player.getUniqueId());
        int totalPages = Math.max(1, (int) Math.ceil(orders.size() / (double) ORDERS_PER_PAGE));
        int page = Math.min(session.getMyOrdersPage(), totalPages - 1);
        session.setMyOrdersPage(page);

        GuiHolder holder = new GuiHolder(GuiType.MY_ORDERS, session);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("My Buy Orders", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
        holder.setInventory(inv);

        ItemStack filler = ItemUtil.filler();
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        // Orders
        int start = page * ORDERS_PER_PAGE;
        int end   = Math.min(start + ORDERS_PER_PAGE, orders.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildOrderItem(orders.get(i)));
        }
        for (int slot = end - start; slot < 45; slot++) {
            inv.setItem(slot, filler);
        }

        if (orders.isEmpty()) {
            inv.setItem(22, ItemUtil.build(Material.BARRIER,
                    Component.text("No active orders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(ItemUtil.gray("Create one via the main menu."))));
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
                List.of(ItemUtil.gray(orders.size() + " active orders"))));
        if (page < totalPages - 1) {
            inv.setItem(53, ItemUtil.build(Material.ARROW,
                    Component.text("Next Page ->", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    List.of()));
        }

        // Back
        inv.setItem(45, page > 0 ? inv.getItem(45) : filler);
        inv.setItem(47, ItemUtil.build(Material.BARRIER,
                Component.text("<- Back to Menu", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                List.of(ItemUtil.gray("Return to main menu"))));

        player.openInventory(inv);
    }

    private ItemStack buildOrderItem(Order order) {
        ItemStack item = new ItemStack(order.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(order.getMaterial().name(), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Quantity: ", NamedTextColor.GRAY)
                .append(Component.text(order.getQuantity() + "x", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Price: ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getEconomyManager().format(order.getPrice()),
                        NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Item ID: ", NamedTextColor.GRAY)
                .append(Component.text(order.getItemId(), NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Created: ", NamedTextColor.GRAY)
                .append(Component.text(FMT.format(Instant.ofEpochMilli(order.getCreatedAt())),
                        NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        if (order.isStrictEnchants()) {
            lore.add(Component.text("⚠ Strict enchants required", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("  ► Right-click to CANCEL & refund", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // Store order UUID in PDC so click handler can retrieve it
        meta.getPersistentDataContainer().set(
                plugin.getItemIdKey(),
                org.bukkit.persistence.PersistentDataType.STRING,
                "order:" + order.getId().toString());

        item.setItemMeta(meta);
        return item;
    }
}
