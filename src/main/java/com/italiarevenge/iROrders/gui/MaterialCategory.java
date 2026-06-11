package com.italiarevenge.iROrders.gui;

import org.bukkit.Material;

import java.util.*;

public enum MaterialCategory {

    BUILDING_BLOCKS("Building Blocks", Material.BRICKS),
    COLORED_BLOCKS("Colored Blocks",   Material.WHITE_WOOL),
    NATURAL_BLOCKS("Natural Blocks",   Material.GRASS_BLOCK),
    ORES("Ores",                       Material.DIAMOND_ORE),
    FUNCTIONAL_BLOCKS("Functional",    Material.CRAFTING_TABLE),
    REDSTONE("Redstone",               Material.REDSTONE),
    COMBAT("Combat & Tools",           Material.IRON_SWORD),
    FOOD("Food & Misc",                Material.APPLE);

    public final String displayName;
    public final Material icon;

    MaterialCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    // ── Helper sets — MUST be declared before the static block that calls classify() ──

    private static final Set<Material> OPERATOR_ITEMS = EnumSet.of(
            // Debug / operator tools
            Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK, Material.BARRIER,
            Material.LIGHT, Material.STRUCTURE_VOID, Material.STRUCTURE_BLOCK, Material.JIGSAW,
            // Command blocks
            Material.COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            // Spawners (creative / Silk Touch edge-case excluded intentionally)
            Material.SPAWNER, Material.TRIAL_SPAWNER,
            // Vault block (not pickable in survival)
            Material.VAULT,
            // World-generation-only blocks
            Material.END_PORTAL_FRAME, Material.BEDROCK, Material.REINFORCED_DEEPSLATE
    );

    private static final Set<Material> COMBAT_EXTRAS = EnumSet.of(
            Material.TRIDENT, Material.SHIELD, Material.MACE,
            Material.BOW, Material.CROSSBOW,
            Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW,
            Material.FISHING_ROD, Material.SHEARS, Material.FLINT_AND_STEEL,
            Material.NAME_TAG, Material.ELYTRA, Material.TOTEM_OF_UNDYING,
            Material.SPYGLASS, Material.WIND_CHARGE
    );

    private static final Set<Material> REDSTONE_EXTRAS = EnumSet.of(
            Material.REDSTONE, Material.REDSTONE_BLOCK, Material.REDSTONE_TORCH,
            Material.REDSTONE_LAMP,
            Material.PISTON, Material.STICKY_PISTON,
            Material.DISPENSER, Material.DROPPER, Material.HOPPER,
            Material.COMPARATOR, Material.REPEATER,
            Material.LEVER, Material.TNT,
            Material.DAYLIGHT_DETECTOR, Material.OBSERVER,
            Material.TRIPWIRE_HOOK, Material.TARGET,
            Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
            Material.SCULK_CATALYST, Material.SCULK_SHRIEKER,
            Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL,
            Material.MINECART, Material.HOPPER_MINECART, Material.TNT_MINECART,
            Material.CHEST_MINECART, Material.FURNACE_MINECART,
            Material.FIREWORK_ROCKET, Material.FIREWORK_STAR,
            Material.NOTE_BLOCK
    );

    private static final Set<Material> NATURAL_EXTRAS = buildNaturalExtras();

    private static Set<Material> buildNaturalExtras() {
        EnumSet<Material> s = EnumSet.of(
                Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT,
                Material.PODZOL, Material.MYCELIUM, Material.MUD, Material.MUDDY_MANGROVE_ROOTS,
                Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.CLAY,
                Material.SNOW, Material.POWDER_SNOW, Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE,
                Material.NETHERRACK, Material.SOUL_SAND, Material.SOUL_SOIL,
                Material.BASALT, Material.POLISHED_BASALT, Material.SMOOTH_BASALT,
                Material.BLACKSTONE, Material.BONE_BLOCK,
                Material.MAGMA_BLOCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.SPONGE, Material.WET_SPONGE,
                Material.VINE, Material.GLOW_LICHEN, Material.KELP, Material.DRIED_KELP_BLOCK,
                Material.SEAGRASS, Material.SEA_PICKLE,
                Material.CACTUS, Material.BAMBOO, Material.SUGAR_CANE,
                Material.CHORUS_PLANT, Material.CHORUS_FLOWER,
                Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
                Material.DEAD_BUSH, Material.LILY_PAD
        );
        s.addAll(EnumSet.of(
                Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
                Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
                Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER,
                Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.SUNFLOWER,
                Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
                Material.DRIPSTONE_BLOCK, Material.POINTED_DRIPSTONE,
                Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST, Material.AMETHYST_SHARD,
                Material.MOSS_BLOCK, Material.MOSS_CARPET,
                Material.AZALEA, Material.FLOWERING_AZALEA,
                Material.SPORE_BLOSSOM, Material.BIG_DRIPLEAF, Material.SMALL_DRIPLEAF,
                Material.HANGING_ROOTS, Material.FROGSPAWN,
                Material.SCULK, Material.SCULK_VEIN,
                Material.NETHER_WART, Material.NETHER_WART_BLOCK, Material.WARPED_WART_BLOCK,
                Material.SHROOMLIGHT, Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM,
                Material.NETHER_SPROUTS, Material.CRIMSON_ROOTS, Material.WARPED_ROOTS,
                Material.WEEPING_VINES, Material.TWISTING_VINES,
                Material.MUSHROOM_STEM, Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK,
                Material.RED_MUSHROOM, Material.BROWN_MUSHROOM,
                Material.MANGROVE_ROOTS, Material.MANGROVE_PROPAGULE,
                Material.TORCHFLOWER, Material.TORCHFLOWER_SEEDS,
                Material.PITCHER_PLANT, Material.PITCHER_POD
        ));
        return Collections.unmodifiableSet(s);
    }

    private static final Set<Material> DRINK_ITEMS = EnumSet.of(
            Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
            Material.MILK_BUCKET
    );

    private static final Set<Material> FUNCTIONAL_EXTRAS = buildFunctionalExtras();

    private static Set<Material> buildFunctionalExtras() {
        EnumSet<Material> s = EnumSet.of(
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.ENDER_CHEST,
                Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
                Material.CRAFTING_TABLE, Material.SMITHING_TABLE, Material.STONECUTTER,
                Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.LOOM,
                Material.FLETCHING_TABLE, Material.COMPOSTER,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.ENCHANTING_TABLE, Material.BOOKSHELF, Material.CHISELED_BOOKSHELF,
                Material.BREWING_STAND, Material.CAULDRON,
                Material.BEACON, Material.CONDUIT, Material.RESPAWN_ANCHOR,
                Material.ITEM_FRAME, Material.GLOW_ITEM_FRAME, Material.PAINTING,
                Material.FLOWER_POT, Material.ARMOR_STAND, Material.LECTERN,
                Material.LANTERN, Material.SOUL_LANTERN, Material.SEA_LANTERN, Material.GLOWSTONE,
                Material.TORCH, Material.SOUL_TORCH, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
                Material.CANDLE
        );
        s.addAll(EnumSet.of(
                Material.LADDER, Material.SCAFFOLDING,
                Material.BOOK, Material.MAP, Material.FILLED_MAP,
                Material.WRITTEN_BOOK, Material.WRITABLE_BOOK,
                Material.BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET,
                Material.POWDER_SNOW_BUCKET, Material.AXOLOTL_BUCKET,
                Material.COD_BUCKET, Material.SALMON_BUCKET, Material.TROPICAL_FISH_BUCKET,
                Material.PUFFERFISH_BUCKET, Material.TADPOLE_BUCKET,
                Material.SADDLE, Material.LEAD, Material.COMPASS, Material.CLOCK,
                Material.END_CRYSTAL, Material.ENDER_PEARL, Material.ENDER_EYE,
                Material.BELL, Material.LIGHTNING_ROD,
                Material.IRON_BARS,
                Material.JUKEBOX, Material.GOAT_HORN,
                Material.COBWEB, Material.BOWL, Material.GLASS_BOTTLE,
                Material.BUNDLE, Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY
        ));
        return Collections.unmodifiableSet(s);
    }

    private static final Set<Material> ORES_EXTRAS = EnumSet.of(
            // Gem / mineral drops
            Material.DIAMOND, Material.EMERALD, Material.COAL,
            Material.LAPIS_LAZULI, Material.QUARTZ, Material.AMETHYST_SHARD,
            // Raw metal forms
            Material.RAW_IRON, Material.RAW_COPPER, Material.RAW_GOLD,
            Material.NETHERITE_SCRAP,
            // Storage blocks
            Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK, Material.LAPIS_BLOCK, Material.COAL_BLOCK,
            Material.COPPER_BLOCK, Material.NETHERITE_BLOCK,
            Material.RAW_IRON_BLOCK, Material.RAW_COPPER_BLOCK, Material.RAW_GOLD_BLOCK,
            Material.QUARTZ_BLOCK, Material.ANCIENT_DEBRIS
    );

    // ── Category maps — built AFTER all helper sets are initialized ────────────

    private static final Map<Material, MaterialCategory> CATEGORY_MAP;
    private static final Map<MaterialCategory, List<Material>> CATEGORY_ITEMS;

    static {
        Map<Material, MaterialCategory> map = new EnumMap<>(Material.class);
        for (Material m : Material.values()) {
            if (m.isAir() || m.isLegacy() || !m.isItem()) continue;
            MaterialCategory cat = classify(m);
            if (cat != null) map.put(m, cat);
        }
        CATEGORY_MAP = Collections.unmodifiableMap(map);

        Map<MaterialCategory, List<Material>> items = new EnumMap<>(MaterialCategory.class);
        for (MaterialCategory c : values()) items.put(c, new ArrayList<>());
        for (Map.Entry<Material, MaterialCategory> entry : map.entrySet())
            items.get(entry.getValue()).add(entry.getKey());
        for (MaterialCategory c : values())
            items.get(c).sort(Comparator.comparing(Material::name));
        CATEGORY_ITEMS = Collections.unmodifiableMap(items);
    }

    public List<Material> getItems() {
        return CATEGORY_ITEMS.getOrDefault(this, List.of());
    }

    public static MaterialCategory of(Material m) {
        return CATEGORY_MAP.get(m);
    }

    // ── Classification logic ──────────────────────────────────────────────────

    private static MaterialCategory classify(Material m) {
        String n = m.name();

        // Excluded: spawn eggs + operator-only items
        if (n.endsWith("_SPAWN_EGG") || OPERATOR_ITEMS.contains(m)) return null;

        // Combat & Tools
        if (n.endsWith("_SWORD") || n.endsWith("_PICKAXE") || n.endsWith("_AXE") ||
                n.endsWith("_SHOVEL") || n.endsWith("_HOE") ||
                n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") ||
                n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") ||
                n.endsWith("_BOW") || n.endsWith("_HORSE_ARMOR") ||
                COMBAT_EXTRAS.contains(m))
            return COMBAT;

        // Redstone components
        if (n.endsWith("_BUTTON") || n.endsWith("_PRESSURE_PLATE") ||
                REDSTONE_EXTRAS.contains(m))
            return REDSTONE;

        // Colored blocks (color-suffixed decorative variants)
        if (n.endsWith("_WOOL") || n.endsWith("_CONCRETE") || n.endsWith("_CONCRETE_POWDER") ||
                n.endsWith("_GLAZED_TERRACOTTA") || n.endsWith("_TERRACOTTA") ||
                n.endsWith("_STAINED_GLASS") || n.endsWith("_STAINED_GLASS_PANE") ||
                n.endsWith("_CARPET") || n.endsWith("_BED") || n.endsWith("_BANNER") ||
                n.endsWith("_CANDLE") || n.endsWith("_SHULKER_BOX") || n.endsWith("_DYE") ||
                m == Material.SHULKER_BOX)
            return COLORED_BLOCKS;

        // Natural / organic blocks
        if (n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") ||
                n.endsWith("_HYPHAE") || n.endsWith("_LEAVES") || n.endsWith("_SAPLING") ||
                n.endsWith("_CORAL") || n.endsWith("_CORAL_BLOCK") || n.endsWith("_CORAL_FAN") ||
                NATURAL_EXTRAS.contains(m))
            return NATURAL_BLOCKS;

        // Food & Misc (edible + drinks)
        if (m.isEdible() || DRINK_ITEMS.contains(m))
            return FOOD;

        // Ores, ingots, raw resources and storage blocks
        if (n.endsWith("_ORE") || n.endsWith("_INGOT") || ORES_EXTRAS.contains(m))
            return ORES;

        // Functional blocks
        if (n.endsWith("_SIGN") || n.endsWith("_HANGING_SIGN") ||
                n.endsWith("_DOOR") || n.endsWith("_TRAPDOOR") ||
                n.endsWith("_BOAT") || n.endsWith("_CHEST_BOAT") ||
                n.endsWith("_RAFT") || n.endsWith("_CHEST_RAFT") ||
                n.startsWith("MUSIC_DISC_") || n.equals("MUSIC_DISC_5") ||
                FUNCTIONAL_EXTRAS.contains(m))
            return FUNCTIONAL_BLOCKS;

        // Default: blocks → Building Blocks, non-block items → Food & Misc
        return m.isBlock() ? BUILDING_BLOCKS : FOOD;
    }
}
