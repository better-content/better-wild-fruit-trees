package com.bettercontent.betterwildfruittrees;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeGrowthRatesTest {
    private static String id(String fruit) {return fruit.equals("apple")?"dynamictrees:apple_oak":"dtfruitsdelight:"+fruit;}
    @Test void coversExactlyTheProtectedSpecies() {
        var expected = new java.util.HashSet<>(WildFruitPolicy.ENERGY.keySet());
        expected.addAll(java.util.Set.of("saguaro", "coconut", "ancient", "saguaro_mega"));
        assertEquals(expected, TreeGrowthRates.REFERENCES.keySet());
        assertEquals(1_500_000L,TreeGrowthRates.TARGET_TICKS);
        for(var fruit : WildFruitPolicy.ENERGY.keySet()) {
            var ref=TreeGrowthRates.reference(id(fruit));
            assertTrue(ref.nativeRate()>0 && ref.pulses()>0);
            assertTrue(Float.isFinite(TreeGrowthRates.rate(id(fruit),ref.nativeRate())));
        }
    }
    @Test void separatelyAdmittedOptionalReferencesRetainExactNativeMeasurements() {
        var admitted = java.util.Map.of(
            "saguaro", new TreeGrowthRates.Reference(1F, 26.75),
            "coconut", new TreeGrowthRates.Reference(.8F, 14.0),
            "ancient", new TreeGrowthRates.Reference(.5F, 89.125),
            "saguaro_mega", new TreeGrowthRates.Reference(.6F, 67.25));
        assertEquals(admitted.keySet(), TreeGrowthRates.REQUIRED_OPTIONAL_REFERENCES.keySet());
        assertTrue(TreeGrowthRates.missingOptionalReferences().isEmpty());
        admitted.forEach((key, reference) -> {
            assertEquals(reference, TreeGrowthRates.REFERENCES.get(key));
            assertEquals(reference.nativeRate(), TreeGrowthRates.REQUIRED_OPTIONAL_REFERENCES.get(key));
        });
        assertEquals(admitted.get("saguaro"), TreeGrowthRates.reference(WildFruitPolicy.SAGUARO));
        assertEquals(admitted.get("coconut"), TreeGrowthRates.reference(WildFruitPolicy.COCONUT));
        assertEquals(admitted.get("ancient"), TreeGrowthRates.reference(WildFruitPolicy.ANCIENT));
        // Ordinary mega cacti do not acquire managed provenance from their species identity.
        assertNull(TreeGrowthRates.reference(WildFruitPolicy.MEGA_CACTUS));
        assertEquals(0F, TreeGrowthRates.megaRate(0F));
        assertTrue(TreeGrowthRates.megaRate(.6F) > 0F);
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
