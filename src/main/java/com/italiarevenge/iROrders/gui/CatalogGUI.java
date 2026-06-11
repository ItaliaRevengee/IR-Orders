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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Creative-style item picker.
 *
 * Layout (54 slots):
 *   Row 0 (0-8)   : [Search] [filler x6] [Clear] [Back]
 *   Rows 1-4 (9-44): item grid, 36 items per page
 *   Row 5 (45-53) : [Prev] [filler x3] [PageInfo] [filler x3] [Next]
 */
public class CatalogGUI {

    static final int ITEMS_PER_PAGE = 36;
    static final int ITEM_START = 9;
    static final int ITEM_END   = 44;

    private static final List<Material> ALL_MATERIALS = Arrays.stream(Material.values())
            .filter(m -> !m.isAir() && m.isItem() && !m.isLegacy())
            .sorted(Comparator.comparing(Material::name))
            .collect(Collectors.toUnmodifiableList());

    private final IROrders plugin;

    public CatalogGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, SessionData session) {
        String search = session.getPickerSearch();
        boolean hasSearch = search != null && !search.isBlank();

        List<Material> filtered = hasSearch
                ? ALL_MATERIALS.stream()
                        .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                                || formatName(m).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList())
                : ALL_MATERIALS;

        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) ITEMS_PER_PAGE));
        int page = Math.min(session.getPickerPage(), totalPages - 1);
        session.setPickerPage(page);

        Component title = hasSearch
                ? Component.text("Search: ", NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.BOLD, true)
                        .append(Component.text("\"" + search + "\"", NamedTextColor.GOLD)
                                .decoration(TextDecoration.BOLD, false))
                : Component.text("Select an Item", NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.BOLD, true);

        GuiHolder holder = new GuiHolder(GuiType.CATALOG, session);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        ItemStack filler = ItemUtil.filler();

        // Row 0
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);

        // Slot 0: search button
        if (hasSearch) {
            inv.setItem(0, ItemUtil.build(Material.COMPASS,
                    Component.text("Search: " + search, NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(
                            Component.text("Click to change search", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false),
                            Component.text("Results: " + filtered.size(), NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false)
                    )));
        } else {
            inv.setItem(0, ItemUtil.build(Material.COMPASS,
                    Component.text("Search...", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(
                            Component.text("Click to search by name", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false),
                            Component.text("Total: " + filtered.size() + " items", NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false)
                    )));
        }

        // Slot 7: clear search (only when active)
        if (hasSearch) {
            inv.setItem(7, ItemUtil.build(Material.BARRIER,
                    Component.text("Clear Search", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Show all items", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
        }

        // Slot 8: back
        inv.setItem(8, ItemUtil.build(Material.ARROW,
                Component.text("<- Back to Menu", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Return to main menu", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))));

        // Row 5 (navigation)
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        // Items grid
        int start = page * ITEMS_PER_PAGE;
        int end   = Math.min(start + ITEMS_PER_PAGE, filtered.size());
        for (int i = start; i < end; i++) {
            inv.setItem(ITEM_START + (i - start), buildMaterialStack(filtered.get(i)));
        }
        for (int slot = ITEM_START + (end - start); slot <= ITEM_END; slot++) {
            inv.setItem(slot, filler);
        }

        // Navigation
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
                List.of(ItemUtil.gray(filtered.size() + " items"))));
        if (page < totalPages - 1) {
            inv.setItem(53, ItemUtil.build(Material.ARROW,
                    Component.text("Next ->", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Page " + (page + 2) + " of " + totalPages, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
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
