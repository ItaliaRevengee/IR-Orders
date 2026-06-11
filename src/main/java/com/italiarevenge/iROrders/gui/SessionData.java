package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.model.CatalogItem;

/**
 * Per-player transient GUI state. Lives only as long as the player
 * is navigating the GUI flow; discarded on InventoryCloseEvent or command.
 */
public class SessionData {

    private CatalogItem selectedItem;
    private int selectedQuantity = -1;
    private String pickerSearch = null;
    private int pickerPage = 0;
    private MaterialCategory selectedCategory = MaterialCategory.BUILDING_BLOCKS;
    private int marketPage = 0;
    private int myOrdersPage = 0;

    public CatalogItem getSelectedItem() { return selectedItem; }
    public void setSelectedItem(CatalogItem item) { this.selectedItem = item; }

    public int getSelectedQuantity() { return selectedQuantity; }
    public void setSelectedQuantity(int q) { this.selectedQuantity = q; }

    public String getPickerSearch() { return pickerSearch; }
    public void setPickerSearch(String s) { this.pickerSearch = s; }

    public int getPickerPage() { return pickerPage; }
    public void setPickerPage(int p) { this.pickerPage = p; }

    public MaterialCategory getSelectedCategory() { return selectedCategory; }
    public void setSelectedCategory(MaterialCategory c) { this.selectedCategory = c; }

    public int getMarketPage() { return marketPage; }
    public void setMarketPage(int p) { this.marketPage = p; }

    public int getMyOrdersPage() { return myOrdersPage; }
    public void setMyOrdersPage(int p) { this.myOrdersPage = p; }

    public void reset() {
        selectedItem = null;
        selectedQuantity = -1;
        pickerSearch = null;
        pickerPage = 0;
        selectedCategory = MaterialCategory.BUILDING_BLOCKS;
    }
}
