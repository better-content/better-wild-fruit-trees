package com.bettercontent.betterwildfruittrees;

/** Rates per 50,000 active game ticks: the pack's extended day/night, not skipped calendar time. */
public final class ProductionRates {
    public static final long DAY_TICKS = 50_000;
    public static final int FIELD_PLANTS = 36;
    public static final int FIELD_ITEMS = 20;
    public static final int TREE_ITEMS = 80;
    public static final long FRUIT_INTERVAL = DAY_TICKS * 8 / TREE_ITEMS;
    public static final long MAX_CREDIT = FRUIT_INTERVAL * 2;
    private ProductionRates() {}

    public static double bushGrowthChance(String id) {
        double yield = switch (id) {
            case "minecraft:sweet_berry_bush", "ars_nouveau:sourceberry_bush" -> 2.5;
            case "fruitsdelight:blueberry_bush", "fruitsdelight:cranberry_bush" -> 1.5;
            default -> 0;
        };
        // Native ripe picking resets two stages below ripe. At randomTickSpeed=3 each block
        // receives 3/4096 random ticks per game tick; keep the native per-pick item distribution.
        return yield == 0 ? 0 : FIELD_ITEMS * 2.0 * 4096 / (FIELD_PLANTS * yield * DAY_TICKS * 3);
    }

    public static long accrue(long credit, long previous, long now) {
        credit = Math.max(0, Math.min(MAX_CREDIT, credit));
        if (now <= previous) return credit;
        // Avoid overflow for hostile timestamps; never store more than two replacement fruits.
        long elapsed = now - previous;
        if (elapsed < 0 || elapsed >= MAX_CREDIT) return MAX_CREDIT;
        return Math.min(MAX_CREDIT, credit + elapsed);
    }
}
