package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.RandomSpeciesSelector;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.SpeciesSelection;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDatabase;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RarityGameTests {
    /** Exact native RNG inputs; field geometry can delegate subsequent draws to a seeded source. */
    public static final class Rolls extends LegacyRandomSource {
        private final boolean strict;
        private final int[] pairs;
        private int cursor;
        public Rolls(long seed, boolean strict, int... pairs) {
            super(seed);
            this.strict = strict;
            this.pairs = pairs;
        }
        @Override public int nextInt(int bound) {
            if (cursor < pairs.length) {
                if (bound != pairs[cursor]) throw new AssertionError("Unexpected native RNG bound " + bound + " expected " + pairs[cursor]);
                int value = pairs[cursor + 1];
                cursor += 2;
                if (value < 0 || value >= bound) throw new AssertionError("Invalid fixture RNG value");
                return value;
            }
            if (strict) throw new AssertionError("Unexpected extra native RNG draw " + bound);
            return super.nextInt(bound);
        }
        public boolean consumed() { return cursor == pairs.length; }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void pinnedFruitSelectorsAreRareAndFallBackToOrdinaryTrees(GameTestHelper h) throws Exception {
        var path = ModList.get().getModFileById("dtfruitsdelight").getFile()
                .findResource("trees", "dtfruitsdelight", "world_gen", "default.json");
        var rules = JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonArray();
        var covered = new java.util.HashSet<String>();
        var soil = Blocks.GRASS_BLOCK.defaultBlockState();
        var oak = Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak"));
        for (var element : rules) {
            var config = element.getAsJsonObject().getAsJsonObject("apply").getAsJsonObject("species");
            if (config == null || !config.has("random")) continue;
            var weights = config.getAsJsonObject("random");
            for (var item : weights.entrySet()) {
                String id = item.getKey().contains(":") ? item.getKey() : "dynamictrees:" + item.getKey();
                if (!WildFruitPolicy.protectsSpecies(id)) continue;
                h.assertTrue(config.has("method") && config.get("method").getAsString().equals("SPLICE_BEFORE"),
                        "Pinned protected tree rule no longer has an ordinary fallback " + id);
                h.assertTrue(item.getValue().getAsInt() == 1 && weights.size() == 2 && weights.has("..."),
                        "Pinned protected tree lottery changed " + id);
                int pass = weights.get("...").getAsInt();
                var fruit = Species.REGISTRY.get(new ResourceLocation(id));
                var selector = new RandomSpeciesSelector().add(pass).add(fruit, 1);
                var entry = new BiomeDatabase().getEntry(Biomes.FOREST);
                entry.setSpeciesSelector((p,s,r) -> new SpeciesSelection(oak));
                var chance = entry.getChanceSelector();
                var density = entry.getDensitySelector();
                entry.setSpeciesSelector(selector, BiomeDatabase.Operation.SPLICE_BEFORE);
                for (int roll=0;roll<256;roll++) {
                    var random = new Rolls(1, true, pass+1, pass, 256, roll);
                    var result = entry.getSpeciesSelector().getSpecies(BlockPos.ZERO, soil, random);
                    h.assertTrue(random.consumed() && result.isHandled() && result.getSpecies() == (roll==0 ? fruit : oak),
                            "Protected fruit lottery/fallback changed " + id + " roll=" + roll);
                }
                var passRandom = new Rolls(1, true, pass+1, 0);
                h.assertTrue(!selector.getSpecies(BlockPos.ZERO, soil, passRandom).isHandled() && passRandom.consumed(),
                        "Native pass-through was altered " + id);
                h.assertTrue(chance == entry.getChanceSelector() && density == entry.getDensitySelector(),
                        "Fruit rarity changed global tree chance/density");
                covered.add(id);
            }
        }
        var expected = new java.util.HashSet<String>();
        for (String fruit : WildFruitPolicy.ENERGY.keySet())
            expected.add(fruit.equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:" + fruit);
        h.assertTrue(covered.equals(expected), "Pinned rarity coverage must include all protected species " + covered);
        var ordinary = new RandomSpeciesSelector().add(oak, 1);
        var random = new Rolls(1, true, 1, 0);
        h.assertTrue(ordinary.getSpecies(BlockPos.ZERO, soil, random).getSpecies() == oak && random.consumed(),
                "Ordinary species were thinned or consumed an extra rarity draw");
        h.succeed();
    }
}
