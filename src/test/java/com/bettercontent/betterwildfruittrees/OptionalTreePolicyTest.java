package com.bettercontent.betterwildfruittrees;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OptionalTreePolicyTest {
    @Test void exactScopeAndNativeAdapters() {
        assertEquals(16, WildFruitPolicy.POLICIES.size());
        assertEquals(WildFruitPolicy.Adapter.CACTUS, WildFruitPolicy.policy(WildFruitPolicy.SAGUARO).adapter());
        assertEquals(WildFruitPolicy.Adapter.PALM, WildFruitPolicy.policy(WildFruitPolicy.COCONUT).adapter());
        assertEquals(WildFruitPolicy.Adapter.FRUIT, WildFruitPolicy.policy(WildFruitPolicy.ANCIENT).adapter());
        assertEquals(10F, WildFruitPolicy.energy(WildFruitPolicy.SAGUARO, 4));
        assertEquals(25F, WildFruitPolicy.energy(WildFruitPolicy.COCONUT, 10));
        assertEquals(60F, WildFruitPolicy.energy(WildFruitPolicy.ANCIENT, 24));
        assertEquals(.3F, WildFruitPolicy.growthTapering(WildFruitPolicy.SAGUARO, .3F));
        for (String id : Set.of(WildFruitPolicy.MEGA_CACTUS, "dynamictreesplus:pipe_cactus", "dynamictreesplus:pillar_cactus",
                "dynamictrees:cocoa", "dtarsnouveau:yellow_archwood", "dtaether:golden_oak", "dtnatures_spirit:olive")) {
            assertFalse(WildFruitPolicy.protectsSpecies(id), id);
            assertEquals(3F, WildFruitPolicy.energy(id, 3F));
        }
        assertNull(WildFruitPolicy.speciesFruit(null));
    }
    @Test void ediblePropagulesAreNotLootRecipeOrTradeContraband() {
        for (String id : Set.of("dynamictreesplus:saguaro_cactus_seed", "natures_spirit:coconut", "quark:ancient_fruit")) {
            assertTrue(WildFruitPolicy.ediblePropagule(id), id);
            assertFalse(WildFruitPolicy.blockedPlantItem(id), id);
        }
        for (String id : Set.of("dynamictreesplus:saguaro_cactus_sapling", "dtnatures_spirit:coconut_seed",
                "dtquark:ancient_seed", "quark:ancient_sapling", "quark:ancient_leaves"))
            assertTrue(WildFruitPolicy.blockedPlantItem(id), id);
        assertFalse(WildFruitPolicy.blockedPlantBlock("natures_spirit:coconut"));
        assertTrue(WildFruitPolicy.blockedPlantBlock("natures_spirit:coconut_sprout"));
        for (String id : Set.of("dynamictreesplus:saguaro", "dtnatures_spirit:coconut", "dtquark:ancient_fruit"))
            assertTrue(WildFruitPolicy.protectsFruit(id), id);
        assertFalse(WildFruitPolicy.protectsFruit("dtaether:golden_apple"));
    }
    /** Release-blocking by design until approved native measurements fill all four references. */
    @Test void everyNewGrowthFormRequiresItsOwnMeasuredReference() {
        assertEquals(Set.of("saguaro", "coconut", "ancient", "saguaro_mega"), TreeGrowthRates.REQUIRED_OPTIONAL_REFERENCES.keySet());
        assertTrue(TreeGrowthRates.missingOptionalReferences().isEmpty(),
            () -> "NOT READY: native growth references missing " + TreeGrowthRates.missingOptionalReferences());
        for (var entry : TreeGrowthRates.REQUIRED_OPTIONAL_REFERENCES.entrySet()) {
            var reference = TreeGrowthRates.REFERENCES.get(entry.getKey());
            assertEquals(entry.getValue(), reference.nativeRate());
            assertTrue(Double.isFinite(reference.pulses()) && reference.pulses() > 0);
        }
    }
}
