package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.model.CatalogItem;
import com.italiarevenge.iROrders.util.PDCUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Routes all inventory interactions to the correct GUI handler
 * and intercepts chat input for price entry.
 */
public class GuiListener implements Listener {

    private final IROrders plugin;
    private final ConcurrentHashMap<UUID, SessionData> sessions = new ConcurrentHashMap<>();

    public GuiListener(IROrders plugin) {
        this.plugin = plugin;
    }

    // ── Session helpers ───────────────────────────────────────────────────────

    public SessionData getOrCreateSession(UUID uuid) {
        return sessions.computeIfAbsent(uuid, k -> new SessionData());
    }

    // ── Inventory click routing ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof GuiHolder gui)) return;

        // Always cancel clicks in plugin inventories to prevent item theft
        e.setCancelled(true);

        // Ignore clicks in the player's own bottom inventory
        if (e.getClickedInventory() == player.getInventory()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        SessionData session = getOrCreateSession(player.getUniqueId());

        switch (gui.getType()) {
            case MAIN_MENU  -> handleMainMenu(player, e.getSlot(), session);
            case CATALOG    -> handleCatalog(player, e.getSlot(), clicked, session);
            case QUANTITY   -> handleQuantity(player, e.getSlot(), clicked, session);
            case MY_ORDERS  -> handleMyOrders(player, e.getSlot(), clicked, e.getClick(), session);
            case MARKET     -> handleMarket(player, e.getSlot(), clicked, e.getClick(), session);
            case BACKPACK   -> handleBackpack(player, e.getSlot(), clicked);
        }
    }

    // ── Main Menu ─────────────────────────────────────────────────────────────

    private void handleMainMenu(Player player, int slot, SessionData session) {
        switch (slot) {
            case 10 -> {                                          // Create Order
                session.reset();
                player.closeInventory();
                new CatalogGUI(plugin).open(player, session);
            }
            case 13 -> {                                          // My Orders
                player.closeInventory();
                new MyOrdersGUI(plugin).open(player, session);
            }
            case 16 -> {                                          // Global Market
                player.closeInventory();
                new MarketGUI(plugin).open(player, session);
            }
        }
    }

    // ── Catalog ───────────────────────────────────────────────────────────────

    private void handleCatalog(Player player, int slot, ItemStack clicked, SessionData session) {
        // Back button
        if (slot == 8) {
            player.closeInventory();
            new MainMenuGUI(plugin).open(player);
            return;
        }
        // Prev page (slot 45)
        if (slot == 45) {
            session.setCatalogPage(Math.max(0, session.getCatalogPage() - 1));
            new CatalogGUI(plugin).open(player, session);
            return;
        }
        // Next page (slot 53)
        if (slot == 53) {
            session.setCatalogPage(session.getCatalogPage() + 1);
            new CatalogGUI(plugin).open(player, session);
            return;
        }
        // Category tab (slots 0-6)
        if (slot < 7) {
            java.util.List<String> cats = plugin.getCatalogManager().getCategoryOrder();
            if (slot < cats.size()) {
                session.setCatalogCategory(cats.get(slot));
                session.setCatalogPage(0);
                new CatalogGUI(plugin).open(player, session);
            }
            return;
        }
        // Item click (slots 9-44)
        if (slot >= 9 && slot <= 44) {
            String pdc = PDCUtil.getItemId(clicked, plugin.getItemIdKey());
            if (pdc == null) return;
            Optional<CatalogItem> ci = plugin.getCatalogManager().getById(pdc);
            if (ci.isEmpty()) return;
            session.setSelectedItem(ci.get());
            player.closeInventory();
            new QuantityGUI(plugin).open(player, session);
        }
    }

    // ── Quantity ──────────────────────────────────────────────────────────────

    private static final int[] QTY_SLOTS    = {11, 12, 13, 14, 15};
    private static final int[] QUANTITIES   = {1, 8, 16, 32, 64};

    private void handleQuantity(Player player, int slot, ItemStack clicked, SessionData session) {
        // Back to catalog
        if (slot == 18) {
            player.closeInventory();
            new CatalogGUI(plugin).open(player, session);
            return;
        }
        for (int i = 0; i < QTY_SLOTS.length; i++) {
            if (slot == QTY_SLOTS[i]) {
                session.setSelectedQuantity(QUANTITIES[i]);
                player.closeInventory();
                askForPrice(player, session);
                return;
            }
        }
    }

    // ── Price input via chat ──────────────────────────────────────────────────

    private void askForPrice(Player player, SessionData session) {
        CatalogItem ci  = session.getSelectedItem();
        int qty         = session.getSelectedQuantity();
        if (ci == null || qty < 1) return;

        player.sendMessage(Component.text("─────────────────────────────", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("Enter the total price for ", NamedTextColor.YELLOW)
                .append(Component.text(qty + "x " + ci.getDisplayName().replaceAll("§.", ""),
                        NamedTextColor.WHITE))
                .append(Component.text(" in chat:", NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("(Type 'cancel' to abort)", NamedTextColor.GRAY));

        plugin.awaitChatInput(player.getUniqueId(), input -> {
            if ("cancel".equalsIgnoreCase(input.trim())) {
                player.sendMessage(Component.text("Order creation cancelled.",
                        NamedTextColor.GRAY));
                return;
            }
            double price;
            try {
                price = Double.parseDouble(input.trim().replace(",", "."));
            } catch (NumberFormatException ex) {
                player.sendMessage(Component.text("Invalid number. Order creation cancelled.",
                        NamedTextColor.RED));
                return;
            }
            plugin.getOrderManager().createOrder(player, ci, qty, price);
        });
    }

    // ── My Orders ─────────────────────────────────────────────────────────────

    private void handleMyOrders(Player player, int slot, ItemStack clicked,
                                ClickType click, SessionData session) {
        // Back button
        if (slot == 47) {
            player.closeInventory();
            new MainMenuGUI(plugin).open(player);
            return;
        }
        // Prev page
        if (slot == 45 && session.getMyOrdersPage() > 0) {
            session.setMyOrdersPage(session.getMyOrdersPage() - 1);
            new MyOrdersGUI(plugin).open(player, session);
            return;
        }
        // Next page
        if (slot == 53) {
            session.setMyOrdersPage(session.getMyOrdersPage() + 1);
            new MyOrdersGUI(plugin).open(player, session);
            return;
        }

        if (!click.isRightClick()) return;

        String pdc = readPdc(clicked);
        if (pdc == null || !pdc.startsWith("order:")) return;
        UUID orderId = parseOrderId(pdc);
        if (orderId == null) return;

        player.closeInventory();
        plugin.getOrderManager().cancelOrder(player, orderId);
    }

    // ── Market ────────────────────────────────────────────────────────────────

    private void handleMarket(Player player, int slot, ItemStack clicked,
                              ClickType click, SessionData session) {
        // Back button
        if (slot == 47) {
            player.closeInventory();
            new MainMenuGUI(plugin).open(player);
            return;
        }
        // Prev page
        if (slot == 45 && session.getMarketPage() > 0) {
            session.setMarketPage(session.getMarketPage() - 1);
            new MarketGUI(plugin).open(player, session);
            return;
        }
        // Next page
        if (slot == 53) {
            session.setMarketPage(session.getMarketPage() + 1);
            new MarketGUI(plugin).open(player, session);
            return;
        }

        if (!click.isRightClick()) return;

        String pdc = readPdc(clicked);
        if (pdc == null || !pdc.startsWith("order:")) return;
        UUID orderId = parseOrderId(pdc);
        if (orderId == null) return;

        // Close and attempt fulfillment on next tick so inventory is closed first
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getOrderManager().fulfillOrder(player, orderId));
    }

    // ── Backpack ──────────────────────────────────────────────────────────────

    private void handleBackpack(Player player, int slot, ItemStack clicked) {
        String pdc = readPdc(clicked);
        if ("backpack_display".equals(pdc)) {
            plugin.getBackpackManager().takeItem(player, slot);
            // Refresh the GUI to reflect the change
            plugin.getBackpackManager().openBackpack(player);
        }
    }

    // ── Inventory close ───────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Cancel pending chat input if the player closes a GUI
        if (!(e.getPlayer() instanceof Player player)) return;
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof GuiHolder)) return;
        // Don't clear session — preserve page state for when they reopen
        // Only clear pending chat input if the player closes during price entry
        // The chat event handler itself handles the 'cancel' case
    }

    // ── Chat input intercept ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Consumer<String> callback = plugin.consumeChatInput(uuid);
        if (callback == null) return;

        e.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(e.message());

        // Execute callback on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(message));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String readPdc(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getItemIdKey(), PersistentDataType.STRING);
    }

    private static UUID parseOrderId(String pdc) {
        try { return UUID.fromString(pdc.substring("order:".length())); }
        catch (IllegalArgumentException e) { return null; }
    }
}
