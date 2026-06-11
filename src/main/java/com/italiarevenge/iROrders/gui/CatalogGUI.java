package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Shows items for the selected category (or search results).
 *
 * Layout (54 slots):
 *   Row 0 (0-8)    : [Search(0)] [filler×6] [ClearSearch(7)] [Back(8)]
 *   Rows 1-4 (9-44): item grid, 36 items per page
 *   Row 5 (45-53)  : [Prev(45)] [filler×3] [PageInfo(49)] [filler×3] [Next(53)]
 */
public class CatalogGUI {

    static final int ITEMS_PER_PAGE = 36;
    static final int ITEM_START = 9;
    static final int ITEM_END   = 44;

    private final IROrders plugin;

    public CatalogGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, SessionData session) {
        String search = session.getPickerSearch();
        boolean hasSearch = search != null && !search.isBlank();
        MaterialCategory selectedCat = session.getSelectedCategory();

        List<Material> items;
        if (hasSearch) {
            String lc = search.toLowerCase(Locale.ROOT);
            items = selectedCat.getItems().stream()
                    .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(lc)
                            || formatName(m).toLowerCase(Locale.ROOT).contains(lc))
                    .collect(Collectors.toList());
        } else {
            items = selectedCat.getItems();
        }

        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
        int page = Math.min(session.getPickerPage(), totalPages - 1);
        session.setPickerPage(page);

        Component title = hasSearch
                ? Component.text(selectedCat.displayName + " › ", NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.BOLD, true)
                        .append(Component.text("\"" + search + "\"", NamedTextColor.GOLD)
                                .decoration(TextDecoration.BOLD, false))
                : Component.text(selectedCat.displayName, NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.BOLD, true);

        GuiHolder holder = new GuiHolder(GuiType.CATALOG, session);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        ItemStack filler = ItemUtil.filler();

        // ── Row 0 ─────────────────────────────────────────────────────────────
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);

        // Slot 0: search within category
        if (hasSearch) {
            inv.setItem(0, ItemUtil.build(Material.COMPASS,
                    Component.text("Search: " + search, NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(
                            Component.text("Click to change search", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false),
                            Component.text("Results: " + items.size(), NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false)
                    )));
        } else {
            inv.setItem(0, ItemUtil.build(Material.COMPASS,
                    Component.text("Search...", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(
                            Component.text("Click to search in this category", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false),
                            Component.text("Total: " + items.size() + " items", NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false)
                    )));
        }

        // Slot 7: clear search (only when active)
        if (hasSearch) {
            inv.setItem(7, ItemUtil.build(Material.BARRIER,
                    Component.text("Clear Search", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Show all items in category", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
        }

        // Slot 8: back to category picker
        inv.setItem(8, ItemUtil.build(Material.ARROW,
                Component.text("<- Categories", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Return to category selection", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))));

        // ── Row 5 ─────────────────────────────────────────────────────────────
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        if (page > 0) {
            inv.setItem(45, ItemUtil.build(Material.ARROW,
                    Component.text("<- Previous", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Page " + page + " of " + totalPages, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
        }
        inv.setItem(49, ItemUtil.build(Material.PAPER,
                Component.text("Page " + (page + 1) + " / " + totalPages, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(ItemUtil.gray(items.size() + " items"))));
        if (page < totalPages - 1) {
            inv.setItem(53, ItemUtil.build(Material.ARROW,
                    Component.text("Next ->", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Page " + (page + 2) + " of " + totalPages, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
        }

        // ── Items grid ────────────────────────────────────────────────────────
        int start = page * ITEMS_PER_PAGE;
        int end   = Math.min(start + ITEMS_PER_PAGE, items.size());
        for (int i = start; i < end; i++) {
            inv.setItem(ITEM_START + (i - start), buildMaterialStack(items.get(i)));
        }
        for (int slot = ITEM_START + (end - start); slot <= ITEM_END; slot++) {
            inv.setItem(slot, filler);
        }

        player.openInventory(inv);
    }

    private ItemStack buildMaterialStack(Material mat) {
        try {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;

            meta.displayName(Component.text(formatName(mat), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, false));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("  > Click to create buy order", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(
                    plugin.getItemIdKey(),
                    PersistentDataType.STRING,
                    "mat:" + mat.name());

            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            return ItemUtil.filler();
        }
    }

    /** "NETHERITE_INGOT" -> "Netherite Ingot" */
    public static String formatName(Material mat) {
        return Arrays.stream(mat.name().split("_"))
                .map(w -> w.isEmpty() ? w
                        : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }
}
