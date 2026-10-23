package com.bettercontent.betterwildfruittrees;

import java.util.Map;

/** Stateless growth-frequency normalization, never a tree age, size cap or maturity timer. */
public final class TreeGrowthRates {
    public static final long TARGET_TICKS = 30 * ProductionRates.DAY_TICKS;
    public static final int RANDOM_TICK_SPEED = 3;
    public static final double PACK_GROWTH_MULTIPLIER = 0.5;
    public record Reference(float nativeRate, double pulses) {}
    /** Native rates are source facts; pulse references must be measured, not borrowed from other species. */
    public static final Map<String, Float> REQUIRED_OPTIONAL_REFERENCES = Map.of(
        "saguaro", 1F, "coconut", .8F, "ancient", .5F, "saguaro_mega", .6F);
    public static java.util.Set<String> missingOptionalReferences() {
        var missing = new java.util.TreeSet<>(REQUIRED_OPTIONAL_REFERENCES.keySet());
        missing.removeAll(REFERENCES.keySet());
        return java.util.Set.copyOf(missing);
    }
    // Legacy thirteen: eight seeded, sunlit fresh native trees, adult native maximum trunk radius.
    // V9 diagnostic 5c099226-736b-4bf2-8bcd-3c70f889673d: eight per optional form; cactus uses
    // native_upturned_four_arm_v1, coconut native_coconut_fruiting_crown_v1, ancient native maximum
    // trunk radius. Four measured references separately Source-admitted by the lead; not activation
    // or full-gate acceptance. Canopy filling may continue; no runtime maturity test exists.
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
        Map.entry("persimmon", new Reference(1.2F, 282.125)),
        Map.entry("saguaro", new Reference(1F, 26.75)),
        Map.entry("coconut", new Reference(.8F, 14.0)),
        Map.entry("ancient", new Reference(.5F, 89.125)),
        Map.entry("saguaro_mega", new Reference(.6F, 67.25)));
    private TreeGrowthRates() {}

    public static Reference reference(String species) {
        String fruit = WildFruitPolicy.speciesFruit(species);
        if (fruit == null) return null;
        return requiredReference(fruit);
    }
    public static float rate(String species, float nativeRate) {
        return apply(reference(species), nativeRate);
    }
    /** Called only for a root with persisted saguaro provenance, never ordinary mega cacti. */
    public static float megaRate(float nativeRate) {
        return apply(requiredReference("saguaro_mega"), nativeRate);
    }
    private static Reference requiredReference(String key) {
        var reference = REFERENCES.get(key);
        if (reference == null && REQUIRED_OPTIONAL_REFERENCES.containsKey(key))
            throw new IllegalStateException("Unmeasured wild fruit growth reference: " + key + "; source work is not ready for activation");
        return reference;
    }
    private static float apply(Reference ref, float nativeRate) {
        if (ref == null) return nativeRate;
        // Root random ticks select one of 4096 cells, 3 times/tick. DT divides root updates by
        // growthFolding and multiplies pulses/update by it: those factors cancel in expectation.
        double opportunities = TARGET_TICKS * RANDOM_TICK_SPEED / 4096.0 * PACK_GROWTH_MULTIPLIER;
        // Multiply, rather than replace, the native result to retain seasons and explicit boosts.
        return (float)(nativeRate * (ref.pulses / opportunities / ref.nativeRate));
    }
}
