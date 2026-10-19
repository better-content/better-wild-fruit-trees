package com.bettercontent.betterwildfruittrees;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductionRatesTest {
    @Test void referenceClockIsExtendedGameTicksNotSkippedCalendarTime() {
        assertEquals(50_000, ProductionRates.DAY_TICKS);
        assertEquals(2500, ProductionRates.FRUIT_INTERVAL);
    }
    @Test void allFourBushesNormalizeToFortyItemsAtMeanFieldSize() {
        for (String id : new String[]{"minecraft:sweet_berry_bush", "ars_nouveau:sourceberry_bush",
                "fruitsdelight:blueberry_bush", "fruitsdelight:cranberry_bush"}) {
            double yield = id.startsWith("fruitsdelight:") ? 1.5 : 2.5;
            double perPlant = ProductionRates.DAY_TICKS * 3.0 / 4096
                    * ProductionRates.bushGrowthChance(id) / 2 * yield;
            assertEquals(40, perPlant * 36, 1e-10);
            assertEquals(40.0 * 30 / 36, perPlant * 30, 1e-10);
            assertEquals(40.0 * 42 / 36, perPlant * 42, 1e-10);
        }
        assertEquals(0, ProductionRates.bushGrowthChance("minecraft:wheat"));
    }
    @Test void oneReferenceDayFundsTwentyReplacementsOfEightItems() {
        long credit = 0, previous = 0;
        int replacements = 0;
        for (long now = 2500; now <= ProductionRates.DAY_TICKS; now += 2500) {
            credit = ProductionRates.accrue(credit, previous, now);
            previous = now;
            if (credit >= ProductionRates.FRUIT_INTERVAL) {
                credit -= ProductionRates.FRUIT_INTERVAL;
                replacements++;
            }
        }
        assertEquals(160, replacements * 8);
        assertEquals(0, credit);
    }
    @Test void rarityIsAdditionalToNativePlacementFrequency() {
        assertEquals(128, WildFruitPolicy.FIELD_RARITY);
        assertEquals(256, WildFruitPolicy.TREE_RARITY);
        assertEquals(4096, 32 * WildFruitPolicy.FIELD_RARITY);
        assertEquals(49_152, 384 * WildFruitPolicy.FIELD_RARITY);
        assertEquals(10_496, 41 * WildFruitPolicy.TREE_RARITY);
    }
    @Test void bankIsBoundedAndDoesNotMultiplyWithTimeJumps() {
        assertEquals(5000, ProductionRates.accrue(0, 0, 50_000_000));
        assertEquals(5000, ProductionRates.accrue(999_999, 0, 50_000_000));
        assertEquals(0, ProductionRates.accrue(-999, 100, 100));
        assertEquals(2000, ProductionRates.accrue(2000, 100, 50));
        assertEquals(5000, ProductionRates.accrue(0, Long.MIN_VALUE, Long.MAX_VALUE));
    }
}
