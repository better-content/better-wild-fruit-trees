package com.bettercontent.betterwildfruittrees;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;

class WildFruitPolicyTest {
    @Test void exactSpeciesBoundaryAndEnergy() {
        assertEquals(13, WildFruitPolicy.ENERGY.size());
        for (var entry : WildFruitPolicy.ENERGY.entrySet()) {
            String species = entry.getKey().equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:" + entry.getKey();
            assertTrue(WildFruitPolicy.protectsSpecies(species));
            assertEquals(entry.getValue(), WildFruitPolicy.energy(species, 1F));
            assertTrue(entry.getValue() >= 20F);
        }
        assertEquals(20F, WildFruitPolicy.energy("dtfruitsdelight:mangosteen", 8));
        assertEquals(40F, WildFruitPolicy.energy("dtfruitsdelight:mango", 16));
        assertEquals(37.5F, WildFruitPolicy.energy("dtfruitsdelight:kiwi", 15));
    }
    @Test void ordinaryTreesAndBushesRemainOutsidePolicy() {
        for (String id : new String[]{"dynamictrees:oak", "dynamictrees:birch", "dynamictrees:spruce", "dynamictrees:jungle", "dtfruitsdelight:blueberry", "other:pear"}) {
            assertFalse(WildFruitPolicy.protectsSpecies(id), id);
            assertEquals(7F, WildFruitPolicy.energy(id, 7F));
        }
        for (String id : new String[]{"minecraft:oak_sapling", "minecraft:apple", "fruitsdelight:pear", "fruitsdelight:blueberry", "minecraft:wheat_seeds", "dynamictrees:oak_seed"})
            assertFalse(WildFruitPolicy.blockedPlantItem(id), id);
    }
    @Test void plantingStockAndMovableLeavesAreBlockedButFruitRemainsFood() {
        for (String name : WildFruitPolicy.ENERGY.keySet()) {
            assertTrue(WildFruitPolicy.blockedPlantItem("fruitsdelight:" + name + "_sapling"));
            assertTrue(WildFruitPolicy.blockedPlantItem("fruitsdelight:" + name + "_leaves"));
            assertTrue(WildFruitPolicy.blockedPlantBlock("dtfruitsdelight:" + name + "_sapling"));
            assertFalse(WildFruitPolicy.blockedPlantItem("fruitsdelight:" + name));
        }
        assertTrue(WildFruitPolicy.blockedPlantItem("dynamictrees:apple_oak_seed"));
        assertTrue(WildFruitPolicy.blockedPlantBlock("dynamictrees:apple_oak_sapling"));
    }
    @Test void hardnessAppliesFloorEvenToTwigsAndPreservesHigherResistance() {
        assertEquals(80F, WildFruitPolicy.hardness(0.25F));
        assertEquals(320F, WildFruitPolicy.hardness(20F));
    }
    @Test void noWorldgenOrGlobalConfigOverridesAreBundled() throws Exception {
        try (var files = Files.walk(Path.of("src/main/resources"))) {
            assertTrue(files.noneMatch(path -> path.toString().contains("jo_codes") || path.toString().contains("world_gen") || path.getFileName().toString().equals("dynamictrees-server.toml")));
        }
    }
}
