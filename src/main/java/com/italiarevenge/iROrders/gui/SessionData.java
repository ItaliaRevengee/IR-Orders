package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.model.CatalogItem;

/**
 * Per-player transient GUI state. Lives only as long as the player
 * is navigating the GUI flow; discarded on InventoryCloseEvent or command.
 */
public class SessionData {

    private CatalogItem selectedItem;
    private int selectedQuantity = -1;
    private int catalogPage = 0;
    private String catalogCategory;
    private int marketPage = 0;
    private int myOrdersPage = 0;

    public CatalogItem getSelectedItem() { return selectedItem; }
    public void setSelectedItem(CatalogItem item) { this.selectedItem = item; }

    public int getSelectedQuantity() { return selectedQuantity; }
    public void setSelectedQuantity(int q) { this.selectedQuantity = q; }

    public int getCatalogPage() { return catalogPage; }
    public void setCatalogPage(int p) { this.catalogPage = p; }

    public String getCatalogCategory() { return catalogCategory; }
    public void setCatalogCategory(String c) { this.catalogCategory = c; }

    public int getMarketPage() { return marketPage; }
    public void setMarketPage(int p) { this.marketPage = p; }

    public int getMyOrdersPage() { return myOrdersPage; }
    public void setMyOrdersPage(int p) { this.myOrdersPage = p; }

    public void reset() {
        selectedItem = null;
        selectedQuantity = -1;
    }
}
