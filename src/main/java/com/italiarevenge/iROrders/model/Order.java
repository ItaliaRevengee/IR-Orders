package com.italiarevenge.iROrders.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Order {

    private final UUID id;
    private final UUID buyerUUID;
    private final String itemId;
    private final Material material;
    private final int quantity;
    private final double price;
    private volatile OrderStatus status;
    private final Map<String, Integer> requiredEnchants;
    private final boolean strictEnchants;
    private final long createdAt;

    public Order(UUID id, UUID buyerUUID, String itemId, Material material,
                 int quantity, double price,
                 Map<String, Integer> requiredEnchants, boolean strictEnchants,
                 long createdAt) {
        this.id = id;
        this.buyerUUID = buyerUUID;
        this.itemId = itemId;
        this.material = material;
        this.quantity = quantity;
        this.price = price;
        this.status = OrderStatus.ACTIVE;
        this.requiredEnchants = requiredEnchants == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(requiredEnchants));
        this.strictEnchants = strictEnchants;
        this.createdAt = createdAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getBuyerUUID() { return buyerUUID; }
    public String getItemId() { return itemId; }
    public Material getMaterial() { return material; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Map<String, Integer> getRequiredEnchants() { return requiredEnchants; }
    public boolean isStrictEnchants() { return strictEnchants; }
    public long getCreatedAt() { return createdAt; }

    public boolean isActive() { return status == OrderStatus.ACTIVE; }
}
