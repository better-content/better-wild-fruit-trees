package com.bettercontent.betterwildfruittrees;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

/** Exact identities. Edible products are not interchangeable with disposable planting stock. */
public final class WildFruitPolicy {
    /** Legacy calibration inventory, retained for the existing thirteen-species fixtures. */
    public static final Map<String, Float> ENERGY = Map.ofEntries(
        Map.entry("mangosteen", 20F), Map.entry("hawberry", 20F),
        Map.entry("apple", 25F), Map.entry("orange", 25F), Map.entry("pear", 25F), Map.entry("lychee", 25F),
        Map.entry("persimmon", 30F), Map.entry("peach", 30F), Map.entry("bayberry", 35F), Map.entry("fig", 35F),
        Map.entry("kiwi", 37.5F), Map.entry("durian", 40F), Map.entry("mango", 40F));
    public static final String SAGUARO = "dynamictreesplus:saguaro_cactus";
    public static final String MEGA_CACTUS = "dynamictreesplus:mega_cactus";
    public static final String COCONUT = "dtnatures_spirit:coconut";
    public static final String ANCIENT = "dtquark:ancient";
    public enum Adapter { FRUIT, CACTUS, PALM }
    public record Policy(String species, String key, String harvest, String product, float energy,
                         Adapter adapter, boolean savedRoot, Set<String> stock, Set<String> plantingBlocks) {}
    public static final Map<String, Policy> POLICIES = policies();
    public static final int HARVEST_MULTIPLIER = 8;
    public static final int FIELD_RARITY = 128;
    public static final int TREE_RARITY = 256;
    public static final int SOIL_LONGEVITY_MULTIPLIER = 3;
    public static final int FRUIT_PLACEMENT_ATTEMPTS = 12;
    public static final float MIN_BRANCH_HARDNESS = 80F;
    public static final float HARDNESS_MULTIPLIER = 16F;
    private WildFruitPolicy() {}

    private static Map<String, Policy> policies() {
        var result = new LinkedHashMap<String, Policy>();
        ENERGY.forEach((name, energy) -> {
            String species = name.equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:" + name;
            String seed = name.equals("apple") ? "dynamictrees:apple_oak_seed" : species + "_seed";
            result.put(species, new Policy(species, name, name.equals("apple") ? "dynamictrees:apple" : species,
                name.equals("apple") ? "minecraft:apple" : "fruitsdelight:" + name, energy, Adapter.FRUIT, false,
                Set.of(seed, "fruitsdelight:" + name + "_sapling", "fruitsdelight:" + name + "_leaves", "dtfruitsdelight:" + name + "_leaves"),
                name.equals("apple") ? Set.of(species + "_sapling", "dtfruitsdelight:apple_sapling") : Set.of(species + "_sapling")));
        });
        result.put(SAGUARO, new Policy(SAGUARO, "saguaro", "dynamictreesplus:saguaro", SAGUARO + "_seed", 10F,
            Adapter.CACTUS, true, Set.of(SAGUARO + "_sapling"), Set.of(SAGUARO + "_sapling")));
        result.put(COCONUT, new Policy(COCONUT, "coconut", COCONUT, "natures_spirit:coconut", 25F,
            Adapter.PALM, true, Set.of(COCONUT + "_seed", COCONUT + "_sapling", COCONUT + "_leaves", "natures_spirit:coconut_leaves", "natures_spirit:coconut_sprout"),
            Set.of(COCONUT + "_sapling", "natures_spirit:coconut_sprout")));
        result.put(ANCIENT, new Policy(ANCIENT, "ancient", "dtquark:ancient_fruit", "quark:ancient_fruit", 60F,
            Adapter.FRUIT, true, Set.of(ANCIENT + "_seed", ANCIENT + "_sapling", ANCIENT + "_leaves", "quark:ancient_sapling", "quark:ancient_leaves"),
            Set.of(ANCIENT + "_sapling", "quark:ancient_sapling")));
        return Map.copyOf(result);
    }
    public static Policy policy(String id) { return id == null ? null : POLICIES.get(id); }
    public static String speciesFruit(String id) {
        var policy = policy(id);
        return policy == null ? null : policy.key();
    }
    public static boolean protectsSpecies(String id) { return policy(id) != null; }
    public static boolean savedRoot(String id) { var p = policy(id); return p != null && p.savedRoot(); }
    public static boolean protectsFruit(String id) {
        // Retain the legacy apple-oak alias as well as its real dynamictrees:apple fruit ID.
        return "dynamictrees:apple_oak".equals(id) || POLICIES.values().stream().anyMatch(p -> p.harvest().equals(id));
    }
    public static float energy(String id, float original) { var p = policy(id); return p == null ? original : p.energy(); }
    public static boolean ediblePropagule(String id) {
        return Set.of(SAGUARO + "_seed", "natures_spirit:coconut", "quark:ancient_fruit").contains(id);
    }
    public static boolean blockedPlantItem(String id) { return POLICIES.values().stream().anyMatch(p -> p.stock().contains(id)); }
    public static boolean blockedPlantBlock(String id) {
        return blockedPlantItem(id) || POLICIES.values().stream().anyMatch(p -> p.plantingBlocks().contains(id));
    }
    public static float growthTapering(String id, float original) {
        var p = policy(id);
        return p != null && p.adapter() != Adapter.CACTUS ? original / 2.5F : original;
    }
    public static float hardness(float original) { return Math.max(MIN_BRANCH_HARDNESS, original * HARDNESS_MULTIPLIER); }
}
