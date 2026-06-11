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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Creative-style item picker with category tabs.
 *
 * Layout (54 slots):
 *   Row 0 (0-8)   : [Search] [Cat1..Cat7] [Back]
 *   Rows 1-4 (9-44): item grid, 36 items per page
 *   Row 5 (45-53) : [Prev] [filler] [ClearSearch] [filler] [PageInfo] [filler x3] [Next]
 */
public class CatalogGUI {

    static final int ITEMS_PER_PAGE = 36;
    static final int ITEM_START = 9;
    static final int ITEM_END   = 44;

    // All categorised materials (spawn eggs excluded via MaterialCategory)
    private static final List<Material> ALL_MATERIALS = Arrays.stream(Material.values())
            .filter(m -> !m.isAir() && m.isItem() && !m.isLegacy()
                    && MaterialCategory.of(m) != null)
            .sorted(Comparator.comparing(Material::name))
            .collect(Collectors.toUnmodifiableList());

    private final IROrders plugin;

    public CatalogGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, SessionData session) {
        String search = session.getPickerSearch();
        boolean hasSearch = search != null && !search.isBlank();
        MaterialCategory selectedCat = session.getSelectedCategory();

        // Items to display
        List<Material> items;
        if (hasSearch) {
            String lc = search.toLowerCase(Locale.ROOT);
            items = ALL_MATERIALS.stream()
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
                ? Component.text("Search: ", NamedTextColor.DARK_PURPLE)
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

        // Slot 0: search
        inv.setItem(0, buildSearchButton(search, hasSearch, items.size()));

        // Slots 1-7: category tabs
        MaterialCategory[] cats = MaterialCategory.values();
        for (int i = 0; i < cats.length; i++) {
            boolean isSelected = !hasSearch && cats[i] == selectedCat;
            inv.setItem(1 + i, buildCategoryTab(cats[i], isSelected));
        }

        // Slot 8: back
        inv.setItem(8, ItemUtil.build(Material.ARROW,
                Component.text("<- Back to Menu", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Return to main menu", NamedTextColor.GRAY)
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
        if (hasSearch) {
            inv.setItem(47, ItemUtil.build(Material.BARRIER,
                    Component.text("Clear Search", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Show category items", NamedTextColor.GRAY)
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

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildCategoryTab(MaterialCategory cat, boolean selected) {
        ItemStack item = new ItemStack(cat.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(cat.displayName,
                        selected ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, selected));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(cat.getItems().size() + " items", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        if (selected) {
            lore.add(Component.empty());
            lore.add(Component.text("Currently viewing", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("Click to browse", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        if (selected) meta.setEnchantmentGlintOverride(true);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSearchButton(String search, boolean hasSearch, int resultCount) {
        if (hasSearch) {
            return ItemUtil.build(Material.COMPASS,
                    Component.text("Search: " + search, NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(
                            Component.text("Click to change search", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false),
                            Component.text("Results: " + resultCount, NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false)
                    ));
        }
        return ItemUtil.build(Material.COMPASS,
                Component.text("Search...", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Click to search by name", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
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
