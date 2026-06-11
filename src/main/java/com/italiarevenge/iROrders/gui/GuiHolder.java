package com.italiarevenge.iROrders.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder that identifies plugin-owned inventories and carries
 * optional contextual data (e.g. current page, selected item).
 */
public class GuiHolder implements InventoryHolder {

    private final GuiType type;
    private final Object data;
    private Inventory inventory;

    public GuiHolder(GuiType type) {
        this(type, null);
    }

    public GuiHolder(GuiType type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public GuiType getType() { return type; }

    @SuppressWarnings("unchecked")
    public <T> T getData() { return (T) data; }
}
