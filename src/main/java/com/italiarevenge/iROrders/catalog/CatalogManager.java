package com.italiarevenge.iROrders.catalog;

import com.italiarevenge.iROrders.IROrders;
import com.italiarevenge.iROrders.model.CatalogItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CatalogManager {

    private final IROrders plugin;
    private final List<CatalogItem> items = new ArrayList<>();
    private final Map<String, CatalogItem> byId = new HashMap<>();
    private final List<String> categoryOrder = new ArrayList<>();
    private final Map<String, String> categoryDisplayNames = new LinkedHashMap<>();
    private final Map<String, Material> categoryIcons = new LinkedHashMap<>();

    public CatalogManager(IROrders plugin) {
        this.plugin = plugin;
    }

    public void load() {
        items.clear();
        byId.clear();
        categoryOrder.clear();
        categoryDisplayNames.clear();
        categoryIcons.clear();

        ConfigurationSection catalog = plugin.getConfig().getConfigurationSection("catalog");
        if (catalog == null) {
            plugin.getLogger().warning("No 'catalog' section found in config.yml");
            return;
        }

        // Load category metadata
        ConfigurationSection cats = catalog.getConfigurationSection("categories");
        if (cats != null) {
            for (String key : cats.getKeys(false)) {
                categoryOrder.add(key);
                ConfigurationSection cat = cats.getConfigurationSection(key);
                categoryDisplayNames.put(key, cat != null
                        ? cat.getString("display-name", key) : key);
                String iconName = cat != null ? cat.getString("icon", "CHEST") : "CHEST";
                Material icon = safeMaterial(iconName, Material.CHEST);
                categoryIcons.put(key, icon);
            }
        }

        // Load items
        List<Map<?, ?>> itemList = catalog.getMapList("items");
        for (Map<?, ?> raw : itemList) {
            try {
                String id = (String) raw.get("id");
                String materialName = (String) raw.get("material");
                String displayName = (String) raw.get("display-name");
                Object rawCat = raw.get("category");
                String category = rawCat instanceof String s ? s : "misc";

                if (id == null || materialName == null) continue;

                Material material = safeMaterial(materialName, null);
                if (material == null) {
                    plugin.getLogger().warning("Unknown material '" + materialName + "' for catalog item '" + id + "'");
                    continue;
                }

                Map<String, Integer> enchants = new HashMap<>();
                Object rawEnchants = raw.get("enchants");
                if (rawEnchants instanceof Map<?, ?> enchantMap) {
                    for (Map.Entry<?, ?> entry : enchantMap.entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        int level = entry.getValue() instanceof Number num
                                ? num.intValue() : 1;
                        enchants.put(key, level);
                    }
                }

                CatalogItem item = new CatalogItem(id, material,
                        displayName != null ? displayName : material.name(),
                        category, enchants);
                items.add(item);
                byId.put(id, item);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load catalog item: " + raw, e);
            }
        }

        plugin.getLogger().info("Catalog loaded: " + items.size() + " items in "
                + categoryOrder.size() + " categories.");
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<CatalogItem> getAll() { return Collections.unmodifiableList(items); }

    public Optional<CatalogItem> getById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<CatalogItem> getByCategory(String category) {
        return items.stream()
                .filter(i -> category.equalsIgnoreCase(i.getCategory()))
                .collect(Collectors.toList());
    }

    public List<String> getCategoryOrder() { return Collections.unmodifiableList(categoryOrder); }

    public String getCategoryDisplayName(String category) {
        return categoryDisplayNames.getOrDefault(category, category);
    }

    public Material getCategoryIcon(String category) {
        return categoryIcons.getOrDefault(category, Material.CHEST);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return fallback; }
    }
}
