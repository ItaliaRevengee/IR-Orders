package com.italiarevenge.iROrders.gui;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.model.CatalogItem;
import com.italiarevenge.iROrders.util.ItemUtil;
import com.italiarevenge.iROrders.util.PDCUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Paginated item catalog.
 * Layout (54 slots):
 *   Row 0 (0-8)   : category selector tabs + back button
 *   Row 1-4 (9-44): item grid (36 items per page)
 *   Row 5 (45-53) : navigation (prev / info / next)
 */
public class CatalogGUI {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int ITEM_START = 9;
    private static final int ITEM_END   = 44;

    private final IROrders plugin;

    public CatalogGUI(IROrders plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, SessionData session) {
        List<String> categories = plugin.getCatalogManager().getCategoryOrder();
        String activeCategory = session.getCatalogCategory();
        if (activeCategory == null && !categories.isEmpty()) {
            activeCategory = categories.get(0);
            session.setCatalogCategory(activeCategory);
        }

        List<CatalogItem> items = activeCategory == null
                ? plugin.getCatalogManager().getAll()
                : plugin.getCatalogManager().getByCategory(activeCategory);

        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
        int page = Math.min(session.getCatalogPage(), totalPages - 1);
        session.setCatalogPage(page);

        GuiHolder holder = new GuiHolder(GuiType.CATALOG, session);
        String catName = activeCategory != null
                ? plugin.getCatalogManager().getCategoryDisplayName(activeCategory)
                : "All Items";
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Catalog: " + stripColor(catName), NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.BOLD, true));
        holder.setInventory(inv);

        // ── Fill with filler ──────────────────────────────────────────────────
        ItemStack filler = ItemUtil.filler();
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        // ── Category tabs (row 0, slots 0-6) ─────────────────────────────────
        int tabSlot = 0;
        for (String cat : categories) {
            if (tabSlot >= 7) break;
            Material icon = plugin.getCatalogManager().getCategoryIcon(cat);
            String displayName = plugin.getCatalogManager().getCategoryDisplayName(cat);
            boolean isActive = cat.equalsIgnoreCase(activeCategory);
            ItemStack tab = ItemUtil.build(icon,
                    Component.text((isActive ? "§l" : "§7") + stripColor(displayName))
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(isActive
                            ? Component.text("Currently viewing", NamedTextColor.GREEN)
                                    .decoration(TextDecoration.ITALIC, false)
                            : Component.text("Click to filter", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)));
            inv.setItem(tabSlot++, tab);
        }

        // ── Back button (slot 8) ──────────────────────────────────────────────
        inv.setItem(8, ItemUtil.build(Material.BARRIER,
                Component.text("§c← Back to Menu").decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Return to main menu", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))));

        // ── Items ─────────────────────────────────────────────────────────────
        int start = page * ITEMS_PER_PAGE;
        int end   = Math.min(start + ITEMS_PER_PAGE, items.size());
        for (int i = start; i < end; i++) {
            CatalogItem ci = items.get(i);
            int slot = ITEM_START + (i - start);
            inv.setItem(slot, buildCatalogItemStack(ci));
        }
        // Fill remaining item slots with filler
        for (int slot = ITEM_START + (end - start); slot <= ITEM_END; slot++) {
            inv.setItem(slot, filler);
        }

        // ── Navigation (row 5) ────────────────────────────────────────────────
        if (page > 0) {
            inv.setItem(45, ItemUtil.build(Material.ARROW,
                    Component.text("§e← Previous Page").decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Page " + page + " of " + totalPages, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
        }
        inv.setItem(49, ItemUtil.build(Material.PAPER,
                Component.text("§fPage " + (page + 1) + " / " + totalPages)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(ItemUtil.gray(items.size() + " items in category"))));
        if (page < totalPages - 1) {
            inv.setItem(53, ItemUtil.build(Material.ARROW,
                    Component.text("§eNext Page →").decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Page " + (page + 2) + " of " + totalPages, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
        }

        player.openInventory(inv);
    }

    private ItemStack buildCatalogItemStack(CatalogItem ci) {
        ItemStack item = new ItemStack(ci.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ItemUtil.legacy(ci.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(ItemUtil.gray("ID: " + ci.getId()));
        if (ci.isStrictEnchants()) {
            lore.add(Component.text("Enchants required (strict):", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            for (Map.Entry<String, Integer> e : ci.getEnchants().entrySet()) {
                lore.add(Component.text("  " + e.getKey() + " " + toRoman(e.getValue()),
                                NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text("  ► Click to create buy order", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // Tag with itemId so the item is traceable
        meta.getPersistentDataContainer().set(
                IROrders.getInstance().getItemIdKey(),
                org.bukkit.persistence.PersistentDataType.STRING, ci.getId());

        item.setItemMeta(meta);
        return item;
    }

    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V"; case 6 -> "VI";
            case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX";
            case 10 -> "X"; default -> String.valueOf(n);
        };
    }
}
