package com.bettercontent.betterwildfruittrees;

import java.util.Map;
import java.util.Set;

/** Fixed, deliberately narrow identity boundary. No registry access or Minecraft initialization. */
public final class WildFruitPolicy {
    public static final Map<String, Float> ENERGY = Map.ofEntries(
        Map.entry("mangosteen", 20F), Map.entry("hawberry", 20F),
        Map.entry("apple", 25F), Map.entry("orange", 25F), Map.entry("pear", 25F), Map.entry("lychee", 25F),
        Map.entry("persimmon", 30F), Map.entry("peach", 30F), Map.entry("bayberry", 35F), Map.entry("fig", 35F),
        Map.entry("kiwi", 37.5F), Map.entry("durian", 40F), Map.entry("mango", 40F));
    public static final int HARVEST_MULTIPLIER = 8;
    public static final int SOIL_LONGEVITY_MULTIPLIER = 3;
    public static final int FRUIT_PLACEMENT_ATTEMPTS = 12;
    public static final float MIN_BRANCH_HARDNESS = 80F;
    public static final float HARDNESS_MULTIPLIER = 16F;
    private WildFruitPolicy() {}

    public static String speciesFruit(String id) {
        if ("dynamictrees:apple_oak".equals(id)) return "apple";
        if (id != null && id.startsWith("dtfruitsdelight:")) {
            String name = id.substring("dtfruitsdelight:".length());
            if (!name.equals("apple") && ENERGY.containsKey(name)) return name;
        }
        return null;
    }
    public static boolean protectsSpecies(String id) { return speciesFruit(id) != null; }
    public static boolean protectsFruit(String id) {
        return "dynamictrees:apple".equals(id) || speciesFruit(id) != null;
    }
    public static float energy(String id, float original) {
        String fruit = speciesFruit(id);
        return fruit == null ? original : ENERGY.get(fruit);
    }
    public static boolean blockedPlantItem(String id) {
        if ("dynamictrees:apple_oak_seed".equals(id)) return true;
        for (String fruit : ENERGY.keySet()) {
            if (id.equals("fruitsdelight:" + fruit + "_sapling")
                || id.equals("fruitsdelight:" + fruit + "_leaves")
                || id.equals("dtfruitsdelight:" + fruit + "_leaves")
                || (!fruit.equals("apple") && id.equals("dtfruitsdelight:" + fruit + "_seed"))) return true;
        }
        return false;
    }
    public static boolean blockedPlantBlock(String id) {
        if ("dynamictrees:apple_oak_sapling".equals(id)) return true;
        if (blockedPlantItem(id)) return true;
        return id.startsWith("dtfruitsdelight:") && id.endsWith("_sapling")
            && ENERGY.containsKey(id.substring("dtfruitsdelight:".length(), id.length() - "_sapling".length()));
    }
    public static float growthTapering(String id, float original) {
        return protectsSpecies(id) ? original / 2.5F : original;
    }
    public static float hardness(float original) {
        return Math.max(MIN_BRANCH_HARDNESS, original * HARDNESS_MULTIPLIER);
    }
}
