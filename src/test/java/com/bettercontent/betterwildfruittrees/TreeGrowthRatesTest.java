package com.bettercontent.betterwildfruittrees;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeGrowthRatesTest {
    private static String id(String fruit) {return fruit.equals("apple")?"dynamictrees:apple_oak":"dtfruitsdelight:"+fruit;}
    @Test void coversExactlyTheProtectedSpecies() {
        assertEquals(WildFruitPolicy.ENERGY.keySet(),TreeGrowthRates.REFERENCES.keySet());
        assertEquals(1_500_000L,TreeGrowthRates.TARGET_TICKS);
        for(var fruit : WildFruitPolicy.ENERGY.keySet()) {
            var ref=TreeGrowthRates.reference(id(fruit));
            assertTrue(ref.nativeRate()>0 && ref.pulses()>0);
            assertTrue(Float.isFinite(TreeGrowthRates.rate(id(fruit),ref.nativeRate())));
        }
    }
    @Test void expectedNativeGrowthBudgetTakesThirtyExtendedDaysAtEveryFolding() {
        for(var fruit : WildFruitPolicy.ENERGY.keySet()) for(int folding : new int[]{1,2,7,400}) {
            var ref=TreeGrowthRates.reference(id(fruit));
            double rate=TreeGrowthRates.rate(id(fruit),ref.nativeRate());
            double updates=TreeGrowthRates.TARGET_TICKS*3/4096.0/folding;
            double pulses=updates*rate*.5*folding;
            assertEquals(ref.pulses(),pulses,.0001,fruit);
            assertEquals(30,ref.pulses()/(3/4096.0*rate*.5)/ProductionRates.DAY_TICKS,.00001,fruit);
        }
    }
    @Test void preservesNativeDormancySeasonsAndExplicitBoosts() {
        for(var fruit : WildFruitPolicy.ENERGY.keySet()) {
            var ref=TreeGrowthRates.reference(id(fruit));
            float ordinary=TreeGrowthRates.rate(id(fruit),ref.nativeRate());
            for(float factor:new float[]{0,.25F,.5F,1,2,4})
                assertEquals(ordinary*factor,TreeGrowthRates.rate(id(fruit),ref.nativeRate()*factor),.000001F);
        }
    }
    @Test void leavesOrdinaryAndUnsupportedIdentitiesUntouched() {
        for(String ordinary : new String[]{null,"dynamictrees:oak","dynamictrees:birch","dtfruitsdelight:apple","other:pear"}) {
            assertNull(TreeGrowthRates.reference(ordinary));
            for(float rate:new float[]{0,.7F,1,5}) assertEquals(rate,TreeGrowthRates.rate(ordinary,rate));
        }
    }
}
