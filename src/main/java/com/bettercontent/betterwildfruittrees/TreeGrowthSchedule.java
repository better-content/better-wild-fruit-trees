package com.bettercontent.betterwildfruittrees;

/** Additional growth potential matures over thirty extended, actively ticking days. */
public final class TreeGrowthSchedule {
    public static final long DURATION = 30 * ProductionRates.DAY_TICKS;
    private TreeGrowthSchedule() {}

    public static long bounded(long age) { return Math.max(0, Math.min(DURATION, age)); }

    public static long advance(long age, long previous, long now) {
        age = bounded(age);
        if (now <= previous || age == DURATION) return age;
        long elapsed = now - previous;
        if (elapsed < 0 || elapsed >= DURATION - age) return DURATION;
        return age + elapsed;
    }

    public static float interpolate(float initial, float mature, long age) {
        age = bounded(age);
        if (age == 0) return initial;
        if (age == DURATION) return mature;
        return (float)(initial + (mature - initial) * ((double)age / DURATION));
    }

    public static float energy(float nativeEnergy, float originalSignal, float matureSignal, long age) {
        if (bounded(age) == DURATION) return nativeEnergy;
        // Preserve any native positional logic multiplier, rather than replacing it with a height.
        return nativeEnergy * (interpolate(originalSignal, matureSignal, age) / matureSignal);
    }
}
