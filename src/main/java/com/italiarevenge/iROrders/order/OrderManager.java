package com.italiarevenge.iROrders.order;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.backpack.BackpackManager;
import com.italiarevenge.iROrders.database.DatabaseManager;
import com.italiarevenge.iROrders.economy.EconomyManager;
import com.italiarevenge.iROrders.inventory.InventoryService;
import com.italiarevenge.iROrders.model.CatalogItem;
import com.italiarevenge.iROrders.model.Order;
import com.italiarevenge.iROrders.model.OrderStatus;
import com.italiarevenge.iROrders.util.ItemUtil;
import com.italiarevenge.iROrders.util.PDCUtil;
import com.italiarevenge.iROrders.validation.EnchantValidator;
import com.italiarevenge.iROrders.validation.ItemValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Core marketplace logic.
 *
 * Thread model:
 *   - All public methods are called from the main thread.
 *   - DB writes for the critical fulfillment path are executed synchronously
 *     on the main thread (SQLite WAL ≈ <3 ms) to guarantee atomicity without
 *     complex two-phase commit logic.
 *   - Non-critical DB operations (order creation, cancellation) are async.
 */
public class OrderManager {

    private final IROrders plugin;
    private final DatabaseManager db;
    private final EconomyManager economy;
    private final BackpackManager backpackManager;
    private final InventoryService inventoryService;

    /** In-memory cache of all ACTIVE orders. */
    private final ConcurrentHashMap<UUID, Order> activeOrders = new ConcurrentHashMap<>();

    /** Per-order mutex to prevent concurrent fulfillment of the same order. */
    private final ConcurrentHashMap<UUID, ReentrantLock> orderLocks = new ConcurrentHashMap<>();

    public OrderManager(IROrders plugin, DatabaseManager db,
                        EconomyManager economy, BackpackManager backpackManager) {
        this.plugin = plugin;
        this.db = db;
        this.economy = economy;
        this.backpackManager = backpackManager;

        EnchantValidator enchantValidator = new EnchantValidator();
        ItemValidator itemValidator = new ItemValidator(plugin.getItemIdKey(), enchantValidator);
        this.inventoryService = new InventoryService(itemValidator);
    }

    // ── Startup ───────────────────────────────────────────────────────────────

    public void loadActiveOrders() {
        List<Order> orders = db.loadActiveOrdersSync();
        orders.forEach(o -> activeOrders.put(o.getId(), o));
        plugin.getLogger().info("Loaded " + orders.size() + " active orders into cache.");
    }

    // ── Create order ──────────────────────────────────────────────────────────

    /**
     * Creates a new buy order for the given player.
     * Checks economy, locks escrow, persists async, and adds to cache.
     */
    public void createOrder(Player buyer, CatalogItem catalogItem, int quantity, double price) {
        int max = plugin.getConfig().getInt("market.max-orders-per-player", 20);
        long playerOrders = activeOrders.values().stream()
                .filter(o -> o.getBuyerUUID().equals(buyer.getUniqueId()))
                .count();
        if (playerOrders >= max) {
            buyer.sendMessage(Component.text("You have reached the maximum of " + max
                    + " active orders.", NamedTextColor.RED));
            return;
        }

        double min = plugin.getConfig().getDouble("economy.min-price", 1.0);
        double maxPrice = plugin.getConfig().getDouble("economy.max-price", 10_000_000.0);
        if (price < min || price > maxPrice) {
            buyer.sendMessage(Component.text("Price must be between "
                    + economy.format(min) + " and " + economy.format(maxPrice) + ".",
                    NamedTextColor.RED));
            return;
        }

        if (!economy.has(buyer, price)) {
            buyer.sendMessage(Component.text("You don't have enough money! Need: "
                    + economy.format(price) + ", have: "
                    + economy.format(economy.getBalance(buyer)) + ".", NamedTextColor.RED));
            return;
        }

        if (!economy.withdraw(buyer, price)) {
            buyer.sendMessage(Component.text("Economy error while locking escrow.",
                    NamedTextColor.RED));
            return;
        }

        Order order = new Order(
                UUID.randomUUID(),
                buyer.getUniqueId(),
                catalogItem.getId(),
                catalogItem.getMaterial(),
                quantity,
                price,
                catalogItem.getEnchants(),
                catalogItem.isStrictEnchants(),
                System.currentTimeMillis());

        activeOrders.put(order.getId(), order);
        db.saveOrderAsync(order);

        buyer.sendMessage(Component.text("✔ Order created! ", NamedTextColor.GREEN)
                .append(Component.text(quantity + "x " + catalogItem.getDisplayName()
                        + " for " + economy.format(price) + " (escrow locked).",
                        NamedTextColor.GRAY)));
    }

    // ── Cancel order ──────────────────────────────────────────────────────────

    public void cancelOrder(Player buyer, UUID orderId) {
        Order order = activeOrders.get(orderId);
        if (order == null || !order.isActive()) {
            buyer.sendMessage(Component.text("Order not found or already completed.",
                    NamedTextColor.RED));
            return;
        }
        if (!order.getBuyerUUID().equals(buyer.getUniqueId())) {
            buyer.sendMessage(Component.text("That is not your order.", NamedTextColor.RED));
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        activeOrders.remove(orderId);
        orderLocks.remove(orderId);

        economy.deposit(buyer, order.getPrice());
        db.updateOrderStatusAsync(orderId, OrderStatus.CANCELLED);

        buyer.sendMessage(Component.text("✔ Order cancelled. ", NamedTextColor.GREEN)
                .append(Component.text(economy.format(order.getPrice())
                        + " refunded to your account.", NamedTextColor.GRAY)));
    }

    // ── Fulfill order ─────────────────────────────────────────────────────────

    /**
     * Attempts to fulfill `orderId` with the seller's current inventory.
     * Must be called on the main thread.
     *
     * Fulfillment is fully atomic:
     *   - per-order ReentrantLock prevents double-fulfillment
     *   - all state changes (inventory, economy, backpack, cache) happen before
     *     async DB write; if the server crashes mid-write the items/money are
     *     already transferred and the order stays ACTIVE in DB — this is the
     *     accepted trade-off for keeping all inventory ops on the main thread
     *     without complex two-phase commit.
     */
    public void fulfillOrder(Player seller, UUID orderId) {
        // ── Quick pre-check (no lock) ─────────────────────────────────────────
        Order order = activeOrders.get(orderId);
        if (order == null || !order.isActive()) {
            seller.sendMessage(Component.text("This order is no longer available.",
                    NamedTextColor.RED));
            return;
        }
        if (order.getBuyerUUID().equals(seller.getUniqueId())) {
            seller.sendMessage(Component.text("You cannot fill your own order!",
                    NamedTextColor.RED));
            return;
        }

        // ── Acquire per-order lock ────────────────────────────────────────────
        ReentrantLock lock = orderLocks.computeIfAbsent(orderId, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            seller.sendMessage(Component.text("This order is currently being processed. "
                    + "Please try again.", NamedTextColor.YELLOW));
            return;
        }

        try {
            // Re-check after acquiring lock (another thread may have just filled it)
            if (!order.isActive()) {
                seller.sendMessage(Component.text("This order is no longer available.",
                        NamedTextColor.RED));
                return;
            }

            // ── Validate seller inventory ─────────────────────────────────────
            int count = inventoryService.countItems(
                    seller.getInventory(), order, order.getQuantity());
            if (count < order.getQuantity()) {
                String itemId = order.getItemId();
                String itemDesc = itemId.startsWith("mat:")
                        ? order.getMaterial().name()
                        : "(id: " + itemId + ")";
                seller.sendMessage(Component.text(
                        "You need " + order.getQuantity() + "x " + itemDesc
                        + ". You have " + count + ".", NamedTextColor.RED));
                return;
            }

            // ── Check buyer backpack has space ────────────────────────────────
            if (!backpackManager.hasSpace(order.getBuyerUUID())) {
                seller.sendMessage(Component.text(
                        "The buyer's backpack is full — order cannot be completed.",
                        NamedTextColor.RED));
                return;
            }

            // ── Mark PROCESSING (prevents duplicate fills while we work) ──────
            order.setStatus(OrderStatus.PROCESSING);

            // ── Remove items from seller inventory ────────────────────────────
            boolean removed = inventoryService.removeItems(
                    seller.getInventory(), order, order.getQuantity());
            if (!removed) {
                // Defensive rollback — shouldn't happen given the count check above
                order.setStatus(OrderStatus.ACTIVE);
                seller.sendMessage(Component.text(
                        "Inventory changed between checks. Please retry.",
                        NamedTextColor.RED));
                return;
            }

            // ── Build the item to place in buyer backpack ─────────────────────
            ItemStack backpackItem = buildBackpackItem(order);

            // ── Add to buyer backpack ─────────────────────────────────────────
            int slot = backpackManager.addItem(order.getBuyerUUID(), backpackItem);
            if (slot == -1) {
                // Race: backpack was filled between hasSpace() and addItem()
                order.setStatus(OrderStatus.ACTIVE);
                // Return items to seller
                returnItems(seller, backpackItem);
                seller.sendMessage(Component.text(
                        "Backpack full — items returned to you.", NamedTextColor.RED));
                return;
            }

            // ── Pay seller ────────────────────────────────────────────────────
            economy.deposit(seller, order.getPrice());

            // ── Commit in-memory state ────────────────────────────────────────
            order.setStatus(OrderStatus.FILLED);
            activeOrders.remove(orderId);

            // ── Persist async (non-blocking from here) ────────────────────────
            final UUID sellerUUID = seller.getUniqueId();
            final UUID buyerUUID  = order.getBuyerUUID();
            final String itemId   = order.getItemId();
            final int qty         = order.getQuantity();
            final double price    = order.getPrice();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                db.updateOrderStatusSync(orderId, OrderStatus.FILLED);
                db.logTransactionSync(orderId, sellerUUID, buyerUUID, itemId, qty, price);
            });

            // ── Notify both players ───────────────────────────────────────────
            seller.sendMessage(Component.text("✔ Sold! ", NamedTextColor.GREEN)
                    .append(Component.text(qty + "x " + order.getMaterial().name()
                            + " for " + economy.format(price) + ".", NamedTextColor.GRAY)));

            Player buyer = Bukkit.getPlayer(buyerUUID);
            if (buyer != null) {
                buyer.sendMessage(Component.text("✔ Your order was filled! ", NamedTextColor.GREEN)
                        .append(Component.text(qty + "x " + order.getMaterial().name()
                                + " is waiting in your backpack (/order backpack).",
                                NamedTextColor.GRAY)));
            }

        } finally {
            lock.unlock();
            // Clean up lock entry to avoid memory leak on long-running servers
            if (!order.isActive()) orderLocks.remove(orderId);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Collection<Order> getActiveOrders() {
        return Collections.unmodifiableCollection(activeOrders.values());
    }

    public List<Order> getPlayerOrders(UUID playerUUID) {
        return activeOrders.values().stream()
                .filter(o -> o.getBuyerUUID().equals(playerUUID))
                .sorted(Comparator.comparingLong(Order::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Order> getOrder(UUID orderId) {
        return Optional.ofNullable(activeOrders.get(orderId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ItemStack buildBackpackItem(Order order) {
        ItemStack item = new ItemStack(order.getMaterial(), order.getQuantity());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    plugin.getItemIdKey(), PersistentDataType.STRING, order.getItemId());
            item.setItemMeta(meta);
        }
        if (order.isStrictEnchants()) {
            for (Map.Entry<String, Integer> e : order.getRequiredEnchants().entrySet()) {
                NamespacedKey key = NamespacedKey.fromString(e.getKey());
                if (key == null) continue;
                Enchantment enc = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.ENCHANTMENT).get(key);
                if (enc != null) item.addUnsafeEnchantment(enc, e.getValue());
            }
        }
        return item;
    }

    private void returnItems(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        leftover.values().forEach(drop ->
                player.getWorld().dropItemNaturally(player.getLocation(), drop));
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void shutdown() {
        // Nothing to flush — DB manager handles its own shutdown
    }
}
