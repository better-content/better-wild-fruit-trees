package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.TreeGrowthRates;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import java.util.List;
import java.util.Set;

/** Native-only fixture selection. Never referenced by a production gameplay class. */
public final class GrowthFixturePlan {
    public static final String PREFIX = "better_wild_fruit_trees.";
    public static final String BATCH = "wft_growth_v2_only";
    public static final String NAME = "nativefreshgrowthcalibration";
    public static final Set<String> LEGACY = Set.of("apple", "bayberry", "durian", "fig", "hawberry", "kiwi",
        "lychee", "mango", "mangosteen", "orange", "peach", "pear", "persimmon");
    private GrowthFixturePlan() {}
    public static String version() {
        String version = System.getProperty(PREFIX + "growthFixtureVersion", "v1");
        if (!Set.of("v1", "v2").contains(version)) throw new IllegalArgumentException("Unknown growth fixture");
        return version;
    }
    public static String scope() { return System.getProperty(PREFIX + "growthFixtureScope", "full"); }
    public static String control() { return System.getProperty(PREFIX + "growthFixtureControl", "none"); }
    public static boolean v2() { return version().equals("v2"); }
    public static List<String> species() {
        if (!WildFruitPolicy.ENERGY.keySet().equals(LEGACY)) throw new IllegalStateException("Legacy policy coverage drift");
        var references = new java.util.HashSet<>(TreeGrowthRates.REFERENCES.keySet());
        references.removeAll(TreeGrowthRates.REQUIRED_OPTIONAL_REFERENCES.keySet());
        if (!references.equals(LEGACY)) throw new IllegalStateException("Legacy reference coverage drift");
        if (!Set.of("full", "prototype").contains(scope()) ||
            !Set.of("none", "zero-energy", "double-cadence").contains(control()))
            throw new IllegalArgumentException("Unknown growth fixture scope/control");
        if (!v2() && (!scope().equals("full") || !control().equals("none")))
            throw new IllegalArgumentException("V2 scope/control on V1");
        return scope().equals("prototype") ? List.of("bayberry", "kiwi") : LEGACY.stream().sorted().toList();
    }
    public static String id(String fruit) { return fruit.equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:" + fruit; }
    public static float frequencyRate(float rate) { return v2() && control().equals("double-cadence") ? rate * 2 : rate; }
}
