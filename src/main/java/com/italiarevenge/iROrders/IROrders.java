package com.italiarevenge.iROrders;

import com.italiarevenge.iROrders.backpack.BackpackManager;
import com.italiarevenge.iROrders.catalog.CatalogManager;
import com.italiarevenge.iROrders.database.DatabaseManager;
import com.italiarevenge.iROrders.economy.EconomyManager;
import com.italiarevenge.iROrders.gui.GuiListener;
import com.italiarevenge.iROrders.gui.MainMenuGUI;
import com.italiarevenge.iROrders.order.OrderManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public final class IROrders extends JavaPlugin {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static IROrders instance;

    public static IROrders getInstance() { return instance; }

    // ── Core services ─────────────────────────────────────────────────────────
    private NamespacedKey itemIdKey;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private CatalogManager catalogManager;
    private BackpackManager backpackManager;
    private OrderManager orderManager;

    /** Chat-input callbacks: populated while a player is entering a price. */
    private final Map<UUID, Consumer<String>> pendingChatInputs = new HashMap<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        itemIdKey = new NamespacedKey(this, "item_id");

        // Database
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Database failed to initialize — disabling IR-Orders.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Economy (Vault)
        economyManager = new EconomyManager(this);
        if (!economyManager.setup()) {
            getLogger().severe("Vault economy not found — disabling IR-Orders.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Catalog
        catalogManager = new CatalogManager(this);
        catalogManager.load();

        // Backpack
        backpackManager = new BackpackManager(this, databaseManager);
        backpackManager.loadAllBackpacks();

        // Order manager
        orderManager = new OrderManager(this, databaseManager, economyManager, backpackManager);
        orderManager.loadActiveOrders();

        // Events
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        // Commands via Paper Brigadier lifecycle API
        LifecycleEventManager<Plugin> lifecycle = getLifecycleManager();
        lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();

            registrar.register(
                Commands.literal("order")
                    .executes(ctx -> {
                        if (ctx.getSource().getSender() instanceof Player player) {
                            new MainMenuGUI(this).open(player);
                        } else {
                            ctx.getSource().getSender().sendMessage(
                                    Component.text("Only players can use this command.",
                                            NamedTextColor.RED));
                        }
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("backpack")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                backpackManager.openBackpack(player);
                            } else {
                                ctx.getSource().getSender().sendMessage(
                                        Component.text("Only players can use this command.",
                                                NamedTextColor.RED));
                            }
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        }))
                    .build(),
                "Open the marketplace menu",
                List.of("market", "orders")
            );
        });

        getLogger().info("IR-Orders v" + getPluginMeta().getVersion() + " enabled — "
                + catalogManager.getAll().size() + " catalog items.");
    }

    @Override
    public void onDisable() {
        if (orderManager    != null) orderManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();
        getLogger().info("IR-Orders disabled.");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public NamespacedKey   getItemIdKey()        { return itemIdKey; }
    public DatabaseManager getDatabaseManager()  { return databaseManager; }
    public EconomyManager  getEconomyManager()   { return economyManager; }
    public CatalogManager  getCatalogManager()   { return catalogManager; }
    public BackpackManager getBackpackManager()  { return backpackManager; }
    public OrderManager    getOrderManager()     { return orderManager; }

    // ── Chat-input helpers ────────────────────────────────────────────────────

    /**
     * Registers a one-shot callback that fires when the player sends their
     * next chat message (used for price entry).
     */
    public void awaitChatInput(UUID playerUUID, Consumer<String> callback) {
        pendingChatInputs.put(playerUUID, callback);
    }

    /**
     * Removes and returns the pending callback for the player, or null.
     */
    public Consumer<String> consumeChatInput(UUID playerUUID) {
        return pendingChatInputs.remove(playerUUID);
    }

    public boolean hasPendingInput(UUID playerUUID) {
        return pendingChatInputs.containsKey(playerUUID);
    }
}
