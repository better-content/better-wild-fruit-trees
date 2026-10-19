package com.bettercontent.betterwildfruittrees;

import java.util.Map;

/** Stateless growth-frequency normalization, never a tree age, size cap or maturity timer. */
public final class TreeGrowthRates {
    public static final long TARGET_TICKS = 30 * ProductionRates.DAY_TICKS;
    public static final int RANDOM_TICK_SPEED = 3;
    public static final double PACK_GROWTH_MULTIPLIER = 0.5;
    public record Reference(float nativeRate, double pulses) {}
    // Eight seeded, sunlit fresh native trees per species, using their native maximum trunk radius
    // as the adult-size reference. Canopy filling may continue; no runtime maturity test exists.
    public static final Map<String, Reference> REFERENCES = Map.ofEntries(
        Map.entry("apple", new Reference(.7F, 204.375)),
        Map.entry("bayberry", new Reference(.9F, 273.625)),
        Map.entry("durian", new Reference(.9F, 241.5)),
        Map.entry("fig", new Reference(.8F, 209.75)),
        Map.entry("hawberry", new Reference(.8F, 303.625)),
        Map.entry("kiwi", new Reference(.8F, 396.375)),
        Map.entry("lychee", new Reference(.8F, 428.375)),
        Map.entry("mango", new Reference(.9F, 420.75)),
        Map.entry("mangosteen", new Reference(.8F, 287.75)),
        Map.entry("orange", new Reference(1.2F, 395.5)),
        Map.entry("peach", new Reference(.8F, 326.5)),
        Map.entry("pear", new Reference(.7F, 219.25)),
        Map.entry("persimmon", new Reference(1.2F, 282.125)));
    private TreeGrowthRates() {}

    public static Reference reference(String species) {
        String fruit = WildFruitPolicy.speciesFruit(species);
        return fruit == null ? null : REFERENCES.get(fruit);
    }
    public static float rate(String species, float nativeRate) {
        var ref = reference(species);
        if (ref == null) return nativeRate;
        // Root random ticks select one of 4096 cells, 3 times/tick. DT divides root updates by
        // growthFolding and multiplies pulses/update by it: those factors cancel in expectation.
        double opportunities = TARGET_TICKS * RANDOM_TICK_SPEED / 4096.0 * PACK_GROWTH_MULTIPLIER;
        // Multiply, rather than replace, the native result to retain seasons and explicit boosts.
        return (float)(nativeRate * (ref.pulses / opportunities / ref.nativeRate));
    }
}
