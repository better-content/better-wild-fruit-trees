package com.bettercontent.betterwildfruittrees;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeGrowthScheduleTest {
    @Test void thirtyExtendedDaysAreOneAndAHalfMillionActiveTicks() {
        assertEquals(1_500_000, TreeGrowthSchedule.DURATION);
        assertEquals(30 * ProductionRates.DAY_TICKS, TreeGrowthSchedule.DURATION);
    }
    @Test void additionalPotentialRampsMonotonicallyAndOnlyFinishesAtThirtyDays() {
        float previousEnergy = 10, previousTaper = .4F;
        for (long age = 0; age < TreeGrowthSchedule.DURATION; age += 1000) {
            float energy = TreeGrowthSchedule.energy(25, 10, 25, age);
            float taper = TreeGrowthSchedule.interpolate(.4F, .16F, age);
            assertTrue(energy >= previousEnergy && energy < 25);
            assertTrue(taper <= previousTaper && taper > .16F);
            previousEnergy = energy; previousTaper = taper;
        }
        assertEquals(17.5F, TreeGrowthSchedule.energy(25, 10, 25, TreeGrowthSchedule.DURATION / 2));
        assertEquals(10, TreeGrowthSchedule.energy(25, 10, 25, 0));
        assertTrue(TreeGrowthSchedule.energy(25, 10, 25, TreeGrowthSchedule.DURATION - 1) < 25);
        assertEquals(25, TreeGrowthSchedule.energy(25, 10, 25, TreeGrowthSchedule.DURATION));
        assertEquals(.16F, TreeGrowthSchedule.interpolate(.4F, .16F, TreeGrowthSchedule.DURATION));
    }
    @Test void energyRetainsNativePositionalLogicMultiplier() {
        assertEquals(5, TreeGrowthSchedule.energy(12.5F, 10, 25, 0));
        assertEquals(8.75F, TreeGrowthSchedule.energy(12.5F, 10, 25, TreeGrowthSchedule.DURATION / 2));
        assertEquals(12.5F, TreeGrowthSchedule.energy(12.5F, 10, 25, TreeGrowthSchedule.DURATION));
    }
    @Test void repeatedPausedReversedAndHostileClocksStayBounded() {
        assertEquals(50_000, TreeGrowthSchedule.advance(0, 100, 50_100));
        assertEquals(50_000, TreeGrowthSchedule.advance(50_000, 100, 100));
        assertEquals(50_000, TreeGrowthSchedule.advance(50_000, 100, 0));
        assertEquals(0, TreeGrowthSchedule.advance(-10, 100, 100));
        assertEquals(TreeGrowthSchedule.DURATION, TreeGrowthSchedule.advance(Long.MAX_VALUE, 100, 100));
        assertEquals(TreeGrowthSchedule.DURATION, TreeGrowthSchedule.advance(0, Long.MIN_VALUE, Long.MAX_VALUE));
        assertEquals(TreeGrowthSchedule.DURATION, TreeGrowthSchedule.advance(TreeGrowthSchedule.DURATION - 1, 0, 2));
    }
}
