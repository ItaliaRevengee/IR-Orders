package com.italiarevenge.iROrders.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CatalogItem {

    private final String id;
    private final Material material;
    private final String displayName;
    private final String category;
    private final Map<String, Integer> enchants;

    public CatalogItem(String id, Material material, String displayName,
                       String category, Map<String, Integer> enchants) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.category = category;
        this.enchants = enchants == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(enchants));
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String getCategory() { return category; }
    public Map<String, Integer> getEnchants() { return enchants; }

    /** True when the catalog entry specifies required enchantments. */
    public boolean isStrictEnchants() { return !enchants.isEmpty(); }
}
