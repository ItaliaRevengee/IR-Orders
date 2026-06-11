package com.italiarevenge.iROrders.backpack;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.database.DatabaseManager;
import com.italiarevenge.iROrders.gui.BackpackGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Virtual double-chest (54 slots) per player. Receives items from filled orders
 * and allows owners to manually withdraw them.
 *
 * In-memory architecture:
 *  - slotCounts  → how many slots are occupied per player (loaded on startup)
 *  - loadedData  → full item map per player (loaded lazily on GUI open / fulfil)
 *
 * The critical-path operations (hasSpace + addItem) are O(1) and main-thread safe.
 */
public class BackpackManager {

    public static final int BACKPACK_SIZE = 54;

    private final IROrders plugin;
    private final DatabaseManager db;

    /** How many slots are occupied per player. */
    private final ConcurrentHashMap<UUID, Integer> slotCounts = new ConcurrentHashMap<>();

    /** Fully loaded backpack contents. Populated lazily. */
    private final ConcurrentHashMap<UUID, Map<Integer, ItemStack>> loadedData = new ConcurrentHashMap<>();

    public BackpackManager(IROrders plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    // ── Startup ───────────────────────────────────────────────────────────────

    /** Loads slot-count summaries from DB so hasSpace() works without loading full data. */
    public void loadAllBackpacks() {
        Map<UUID, Integer> counts = db.loadBackpackSlotCountsSync();
        slotCounts.putAll(counts);
        plugin.getLogger().info("Backpack slot-counts loaded for " + counts.size() + " players.");
    }

    // ── Critical path (main thread) ───────────────────────────────────────────

    /** Returns true if the player's backpack has at least one free slot. */
    public boolean hasSpace(UUID playerUUID) {
        return slotCounts.getOrDefault(playerUUID, 0) < BACKPACK_SIZE;
    }

    /**
     * Adds an item to the first free slot in the player's backpack.
     * Must be called on the main thread.
     *
     * @return the slot index where the item was placed, or -1 if no space.
     */
    public int addItem(UUID playerUUID, ItemStack item) {
        // Ensure data is loaded for this player
        ensureLoaded(playerUUID);

        Map<Integer, ItemStack> data = loadedData.get(playerUUID);
        for (int slot = 0; slot < BACKPACK_SIZE; slot++) {
            if (!data.containsKey(slot)) {
                data.put(slot, item.clone());
                slotCounts.merge(playerUUID, 1, Integer::sum);
                db.saveBackpackItemSync(playerUUID, slot, item);
                return slot;
            }
        }
        return -1; // full
    }

    /**
     * Removes the item at the given slot and attempts to give it to the player.
     * If the player's inventory is full, drops the items at their feet.
     * Must be called on the main thread.
     */
    public void takeItem(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        ensureLoaded(uuid);
        Map<Integer, ItemStack> data = loadedData.get(uuid);
        ItemStack item = data.remove(slot);
        if (item == null) return;

        slotCounts.merge(uuid, -1, (a, b) -> Math.max(0, a + b));
        db.removeBackpackItemSync(uuid, slot);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        leftover.values().forEach(drop ->
                player.getWorld().dropItemNaturally(player.getLocation(), drop));
    }

    // ── GUI ───────────────────────────────────────────────────────────────────

    /**
     * Opens the backpack GUI for the player.
     * Loads data from DB if not yet cached, then opens synchronously.
     */
    public void openBackpack(Player player) {
        UUID uuid = player.getUniqueId();
        if (loadedData.containsKey(uuid)) {
            new BackpackGUI(plugin, this).open(player);
        } else {
            db.loadBackpackItemsAsync(uuid).thenAccept(items -> {
                loadedData.put(uuid, new LinkedHashMap<>(items));
                // Switch back to main thread to open inventory
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        new BackpackGUI(plugin, this).open(player));
            });
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable snapshot of a player's backpack contents.
     * Loads from DB synchronously if not yet cached.
     */
    public Map<Integer, ItemStack> getContents(UUID playerUUID) {
        ensureLoaded(playerUUID);
        return Collections.unmodifiableMap(loadedData.get(playerUUID));
    }

    public int getUsedSlots(UUID playerUUID) {
        return slotCounts.getOrDefault(playerUUID, 0);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void ensureLoaded(UUID playerUUID) {
        if (!loadedData.containsKey(playerUUID)) {
            // Synchronous fallback — called rarely (first access only)
            db.loadBackpackItemsAsync(playerUUID).thenAccept(items ->
                    loadedData.put(playerUUID, new LinkedHashMap<>(items))).join();
        }
    }
}
