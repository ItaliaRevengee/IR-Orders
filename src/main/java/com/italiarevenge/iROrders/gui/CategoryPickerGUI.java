package com.italiarevenge.iROrders.gui;

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

import java.util.ArrayList;
import java.util.List;

/**
 * First screen of the order-creation flow: one icon per category.
 *
 * Layout (54 slots — 6×9 chest), 8 categories in two rows with spacing:
 *
 *   Row 0 (0-8)    : filler
 *   Row 1 (9-17)   : [f] [Cat0] [f] [Cat1] [f] [Cat2] [f] [Cat3] [f]
 *   Row 2 (18-26)  : filler
 *   Row 3 (27-35)  : [f] [Cat4] [f] [Cat5] [f] [Cat6] [f] [Cat7] [f]
 *   Row 4 (36-44)  : filler
 *   Row 5 (45-53)  : [filler×4] [Back(49)] [filler×4]
 *
 * Category slots: 10, 12, 14, 16, 28, 30, 32, 34
 */
public class CategoryPickerGUI {

    static final int[] CATEGORY_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34};

    public void open(Player player, SessionData session) {
        Component title = Component.text("Select Category", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.BOLD, true);

        GuiHolder holder = new GuiHolder(GuiType.CATEGORY_PICKER, session);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        ItemStack filler = ItemUtil.filler();
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        MaterialCategory[] cats = MaterialCategory.values();
        for (int i = 0; i < cats.length && i < CATEGORY_SLOTS.length; i++) {
            inv.setItem(CATEGORY_SLOTS[i], buildCategoryIcon(cats[i]));
        }

        inv.setItem(49, ItemUtil.build(Material.ARROW,
                Component.text("<- Back to Menu", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Return to main menu", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))));

        player.openInventory(inv);
    }

    private ItemStack buildCategoryIcon(MaterialCategory cat) {
        ItemStack item = new ItemStack(cat.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(cat.displayName, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(cat.getItems().size() + " items", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to browse", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
