package com.bettercontent.betterwildfruittrees.gametest;

import java.util.Map;
import java.util.TreeMap;

/** Bounded observations of already evaluated native arguments/results; no world/RNG/native calls. */
public final class GrowthPulseMetrics {
    public static long legacyState(long seed) { return (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1); }
    private int calls, successful, choked, maximumSteps;
    private long steps, previousWorldAfter, previousGrowthAfter;
    private int betweenWorldChanges, betweenGrowthChanges, growWorldChanges, growGrowthChanges, ageWorldChanges;
    private float minimumEnergy = Float.POSITIVE_INFINITY, maximumEnergy = Float.NEGATIVE_INFINITY;
    private final Map<String,Integer> energies = new TreeMap<>();
    public void observe(float suppliedEnergy, boolean success, boolean choke, int nativeSteps) {
        if (!Float.isFinite(suppliedEnergy) || nativeSteps < 0 || calls >= GrowthFixtureController.PULSE_LIMIT)
            throw new IllegalStateException("Invalid/unbounded native pulse observation");
        calls++; if (success) successful++; if (choke) choked++;
        steps += nativeSteps; maximumSteps = Math.max(maximumSteps, nativeSteps);
        minimumEnergy = Math.min(minimumEnergy, suppliedEnergy); maximumEnergy = Math.max(maximumEnergy, suppliedEnergy);
        // Exact float-bit distribution, bounded by the existing4096 pulse cap, not rounded means.
        energies.merge(Integer.toHexString(Float.floatToRawIntBits(suppliedEnergy)), 1, Integer::sum);
    }
    public void observeRandom(long worldBefore, long growthBefore, long worldAfterGrow, long growthAfterGrow, long worldAfterAge) {
        if (calls > 1) {
            if (worldBefore != previousWorldAfter) betweenWorldChanges++;
            if (growthBefore != previousGrowthAfter) betweenGrowthChanges++;
        }
        if (worldBefore != worldAfterGrow) growWorldChanges++;
        if (growthBefore != growthAfterGrow) growGrowthChanges++;
        if (worldAfterGrow != worldAfterAge) ageWorldChanges++;
        previousWorldAfter = worldAfterAge; previousGrowthAfter = growthAfterGrow;
    }
    public record RandomSummary(int betweenWorldChanges, int betweenGrowthChanges, int growWorldChanges,
                                int growGrowthChanges, int ageWorldChanges) {}
    public RandomSummary randomSummary() { return new RandomSummary(betweenWorldChanges, betweenGrowthChanges,
        growWorldChanges, growGrowthChanges, ageWorldChanges); }
    public record Summary(int calls, int successful, int choked, long totalSteps, int maximumSteps,
                          Float minimumEnergy, Float maximumEnergy, Map<String,Integer> suppliedEnergyBits) {}
    public Summary summary() { return new Summary(calls, successful, choked, steps, maximumSteps,
        calls == 0 ? null : minimumEnergy, calls == 0 ? null : maximumEnergy, Map.copyOf(energies)); }
}
