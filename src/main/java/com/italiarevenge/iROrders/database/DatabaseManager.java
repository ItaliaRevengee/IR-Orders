package com.italiarevenge.iROrders.database;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.model.Order;
import com.italiarevenge.iROrders.model.OrderStatus;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * All DB access is funnelled through a single-threaded executor so SQLite
 * never receives concurrent writes. Critical-path operations (fulfillment)
 * expose synchronous variants that callers invoke from the main thread;
 * everything else is async via CompletableFuture.
 */
public class DatabaseManager {

    private final IROrders plugin;
    private final ExecutorService executor;
    private Connection connection;

    public DatabaseManager(IROrders plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IROrders-DB");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Initialization ────────────────────────────────────────────────────────

    public boolean initialize() {
        try {
            plugin.getDataFolder().mkdirs();
            File dbFile = new File(plugin.getDataFolder(),
                    plugin.getConfig().getString("database.file", "ir-orders.db"));
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            try (Statement s = connection.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL");
                s.execute("PRAGMA synchronous=NORMAL");
                s.execute("PRAGMA foreign_keys=ON");
                s.execute("PRAGMA cache_size=-8000");
            }
            createTables();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database initialization failed", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS buy_orders (
                    id             TEXT    PRIMARY KEY,
                    buyer_uuid     TEXT    NOT NULL,
                    item_id        TEXT    NOT NULL,
                    material       TEXT    NOT NULL,
                    quantity       INTEGER NOT NULL,
                    price          REAL    NOT NULL,
                    status         TEXT    NOT NULL DEFAULT 'ACTIVE',
                    strict_enchants INTEGER NOT NULL DEFAULT 0,
                    created_at     INTEGER NOT NULL
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS order_enchants (
                    order_id      TEXT    NOT NULL,
                    enchant_key   TEXT    NOT NULL,
                    enchant_level INTEGER NOT NULL,
                    PRIMARY KEY (order_id, enchant_key),
                    FOREIGN KEY (order_id) REFERENCES buy_orders(id) ON DELETE CASCADE
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS backpack_items (
                    player_uuid TEXT    NOT NULL,
                    slot        INTEGER NOT NULL,
                    item_data   TEXT    NOT NULL,
                    PRIMARY KEY (player_uuid, slot)
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS transaction_log (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id    TEXT    NOT NULL,
                    seller_uuid TEXT    NOT NULL,
                    buyer_uuid  TEXT    NOT NULL,
                    item_id     TEXT    NOT NULL,
                    quantity    INTEGER NOT NULL,
                    price       REAL    NOT NULL,
                    timestamp   INTEGER NOT NULL
                )""");

            s.execute("CREATE INDEX IF NOT EXISTS idx_orders_status  ON buy_orders(status)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_orders_buyer   ON buy_orders(buyer_uuid)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_bp_player      ON backpack_items(player_uuid)");
        }
    }

    // ── Synchronous writes (main-thread critical path) ────────────────────────

    public void saveOrderSync(Order order) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO buy_orders (id,buyer_uuid,item_id,material,quantity,price,status,strict_enchants,created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, order.getId().toString());
            ps.setString(2, order.getBuyerUUID().toString());
            ps.setString(3, order.getItemId());
            ps.setString(4, order.getMaterial().name());
            ps.setInt(5, order.getQuantity());
            ps.setDouble(6, order.getPrice());
            ps.setString(7, order.getStatus().name());
            ps.setInt(8, order.isStrictEnchants() ? 1 : 0);
            ps.setLong(9, order.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "saveOrderSync failed", e);
        }

        if (!order.getRequiredEnchants().isEmpty()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO order_enchants (order_id,enchant_key,enchant_level) VALUES (?,?,?)")) {
                for (Map.Entry<String, Integer> e : order.getRequiredEnchants().entrySet()) {
                    ps.setString(1, order.getId().toString());
                    ps.setString(2, e.getKey());
                    ps.setInt(3, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "saveOrderEnchants failed", e);
            }
        }
    }

    public void updateOrderStatusSync(UUID orderId, OrderStatus status) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE buy_orders SET status=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setString(2, orderId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "updateOrderStatusSync failed", e);
        }
    }

    public void saveBackpackItemSync(UUID playerUUID, int slot, ItemStack item) {
        String b64 = Base64.getEncoder().encodeToString(item.serializeAsBytes());
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO backpack_items (player_uuid,slot,item_data) VALUES (?,?,?)")) {
            ps.setString(1, playerUUID.toString());
            ps.setInt(2, slot);
            ps.setString(3, b64);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "saveBackpackItemSync failed", e);
        }
    }

    public void removeBackpackItemSync(UUID playerUUID, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM backpack_items WHERE player_uuid=? AND slot=?")) {
            ps.setString(1, playerUUID.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "removeBackpackItemSync failed", e);
        }
    }

    public void logTransactionSync(UUID orderId, UUID sellerUUID, UUID buyerUUID,
                                   String itemId, int quantity, double price) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO transaction_log (order_id,seller_uuid,buyer_uuid,item_id,quantity,price,timestamp) " +
                "VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, orderId.toString());
            ps.setString(2, sellerUUID.toString());
            ps.setString(3, buyerUUID.toString());
            ps.setString(4, itemId);
            ps.setInt(5, quantity);
            ps.setDouble(6, price);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "logTransactionSync failed", e);
        }
    }

    // ── Synchronous reads (startup) ───────────────────────────────────────────

    public List<Order> loadActiveOrdersSync() {
        // Bulk-load all enchants first to avoid N+1 queries
        Map<UUID, Map<String, Integer>> allEnchants = loadAllOrderEnchantsSync();
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM buy_orders WHERE status='ACTIVE'")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                orders.add(new Order(
                        id,
                        UUID.fromString(rs.getString("buyer_uuid")),
                        rs.getString("item_id"),
                        Material.valueOf(rs.getString("material")),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        allEnchants.getOrDefault(id, Collections.emptyMap()),
                        rs.getInt("strict_enchants") == 1,
                        rs.getLong("created_at")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "loadActiveOrdersSync failed", e);
        }
        return orders;
    }

    private Map<UUID, Map<String, Integer>> loadAllOrderEnchantsSync() {
        Map<UUID, Map<String, Integer>> result = new HashMap<>();
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery(
                    "SELECT order_id,enchant_key,enchant_level FROM order_enchants");
            while (rs.next()) {
                UUID oid = UUID.fromString(rs.getString(1));
                result.computeIfAbsent(oid, k -> new HashMap<>())
                      .put(rs.getString(2), rs.getInt(3));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "loadAllOrderEnchants failed", e);
        }
        return result;
    }

    public Map<UUID, Integer> loadBackpackSlotCountsSync() {
        Map<UUID, Integer> counts = new HashMap<>();
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery(
                    "SELECT player_uuid, COUNT(*) FROM backpack_items GROUP BY player_uuid");
            while (rs.next())
                counts.put(UUID.fromString(rs.getString(1)), rs.getInt(2));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "loadBackpackCounts failed", e);
        }
        return counts;
    }

    // ── Asynchronous reads ────────────────────────────────────────────────────

    public CompletableFuture<Map<Integer, ItemStack>> loadBackpackItemsAsync(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> loadBackpackItemsDirect(playerUUID), executor);
    }

    /**
     * Reads backpack items directly on the calling thread without going through the executor.
     * Use this on the main thread when data is needed immediately — avoids blocking the tick
     * waiting for the executor queue to drain.
     */
    public Map<Integer, ItemStack> loadBackpackItemsDirect(UUID playerUUID) {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT slot,item_data FROM backpack_items WHERE player_uuid=? ORDER BY slot")) {
            ps.setString(1, playerUUID.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                byte[] bytes = Base64.getDecoder().decode(rs.getString(2));
                items.put(rs.getInt(1), ItemStack.deserializeBytes(bytes));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "loadBackpackItemsDirect failed", e);
        }
        return items;
    }

    // ── Asynchronous writes (non-critical path) ───────────────────────────────

    public CompletableFuture<Void> saveOrderAsync(Order order) {
        return CompletableFuture.runAsync(() -> saveOrderSync(order), executor);
    }

    public CompletableFuture<Void> updateOrderStatusAsync(UUID orderId, OrderStatus status) {
        return CompletableFuture.runAsync(() -> updateOrderStatusSync(orderId, status), executor);
    }

    public CompletableFuture<Void> logTransactionAsync(UUID orderId, UUID sellerUUID,
                                                        UUID buyerUUID, String itemId,
                                                        int quantity, double price) {
        return CompletableFuture.runAsync(
                () -> logTransactionSync(orderId, sellerUUID, buyerUUID, itemId, quantity, price),
                executor);
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void shutdown() {
        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "DB close error", e); }
    }
}
