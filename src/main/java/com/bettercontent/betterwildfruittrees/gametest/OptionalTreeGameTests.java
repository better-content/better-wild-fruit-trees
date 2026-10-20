package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import com.bettercontent.betterwildfruittrees.compat.RootLedger;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.*;
import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import com.ferreusveritas.dynamictrees.systems.pod.Pod;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.ferreusveritas.dynamictrees.systems.substance.MegaSubstance;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.gametest.*;
import java.util.*;

/** Authored native contracts. They have not been executed for the optional-tree implementation. */
@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OptionalTreeGameTests {
    private static final List<String> IDS = List.of(WildFruitPolicy.SAGUARO, WildFruitPolicy.COCONUT, WildFruitPolicy.ANCIENT);
    private static Species species(String id) { return Species.REGISTRY.get(new ResourceLocation(id)); }
    private static List<String> present(GameTestHelper h) {
        String requested = System.getProperty("better_wild_fruit_trees.fixture.wild_addons");
        h.assertTrue(requested != null, "Missing optional addon fixture contract");
        boolean expected = Boolean.parseBoolean(requested);
        for (String mod : List.of("dynamictreesplus", "dtnatures_spirit", "dtquark"))
            h.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded(mod) == expected, "Wrong optional fixture presence " + mod);
        for (String id : IDS) h.assertTrue(species(id).isValid() == expected, "Wrong optional species availability " + id);
        return expected ? IDS : List.of();
    }
    private static void root(GameTestHelper h, String id, BlockPos root) {
        var level = h.getLevel(); var tree = species(id);
        level.setBlock(root, (id.equals(WildFruitPolicy.SAGUARO) ? Blocks.SAND : Blocks.DIRT).defaultBlockState(), 2);
        tree.placeRootyDirtBlock(level, root, 0);
        tree.getFamily().getBranch().orElseThrow().setRadius(level, root.above(), 8, Direction.DOWN, 2);
    }
    private static void clear(GameTestHelper h, BlockPos root) {
        for (var pos : BlockPos.betweenClosed(root.offset(-3, 0, -3), root.offset(3, 5, 3)))
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void optionalNativeStockAndDirectPlayerProtection(GameTestHelper h) {
        var level = h.getLevel(); var root = h.absolutePos(new BlockPos(4, 1, 4));
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
            new com.mojang.authlib.GameProfile(UUID.randomUUID(), "WildTreeGate"));
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
        try {
            for (String id : present(h)) {
                root(h, id, root); var tree = species(id); var trunk = root.above();
                h.assertTrue(tree.getSignalEnergy() == WildFruitPolicy.policy(id).energy(), "Native energy policy missing " + id);
                h.assertTrue(LoadedTreeLookup.isProtected(level, trunk), "Exact root protection missing " + id);
                h.assertTrue(level.getBlockState(trunk).getDestroySpeed(level, trunk) >= 80, "Native hardness floor missing " + id);
                h.assertTrue(!tree.plantSapling(level, root.east(2), true), "Force planting bypass " + id);
                h.assertTrue(tree.getPrimitiveSaplingRecipes().isEmpty(), "Primitive conversion survived " + id);
                tree.getSeed().ifPresent(seed -> {
                    h.assertTrue(!seed.shouldPlant(level, root.east(2), new ItemStack(seed)), "Automatic planting bypass " + id);
                    h.assertTrue(seed.getPlant(level, root.east(2)).isAir(), "Machine planting bypass " + id);
                });
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                h.assertTrue(level.getBlockState(trunk).getDestroyProgress(player, level, trunk) == 0, "Unsuitable native mining progress " + id);
                h.assertTrue(!player.gameMode.destroyBlock(trunk), "Real player removed protected trunk with iron " + id);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
                h.assertTrue(player.gameMode.destroyBlock(trunk), "Diamond native removal denied " + id);
                clear(h, root);
            }
            h.succeed();
        } finally { clear(h, root); }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void edibleProductsSurviveLootAndMatureHarvestIncludesFallingCoconut(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(4, 4, 4));
        var modifier = new PropagationLootModifier(new LootItemCondition[0]);
        try {
            for (String id : present(h)) {
                var policy = WildFruitPolicy.policy(id);
                var product = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(policy.product()));
                h.assertTrue(product != null && product != Items.AIR, "Native food/product lost " + id);
                if (!id.equals(WildFruitPolicy.COCONUT)) h.assertTrue(product.isEdible(), "Native edible behavior lost " + id);
                var loot = new ObjectArrayList<ItemStack>(); loot.add(new ItemStack(product));
                h.assertTrue(modifier.apply(loot, null).size() == 1, "Food filtered as stock " + id);
                if (id.equals(WildFruitPolicy.COCONUT)) {
                    var food = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("natures_spirit:coconut"));
                    var foodPos = pos.below(2);
                    level.setBlock(foodPos.below(), Blocks.SAND.defaultBlockState(), 2);
                    var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(product));
                    var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(foodPos.below()), Direction.UP, foodPos.below(), false);
                    var context = new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                    h.assertTrue(product instanceof BlockItem && ((BlockItem)product).place(context).consumesAction(), "Food-processing coconut placement denied");
                    h.assertTrue(level.getBlockState(foodPos).is(food), "DT destroyed/replaced placed food coconut");
                    var grower = (net.minecraft.world.level.block.BonemealableBlock)food;
                    h.assertTrue(!grower.isValidBonemealTarget(level, foodPos, level.getBlockState(foodPos), false), "Food coconut advertises germination");
                    grower.performBonemeal(level, level.random, foodPos, level.getBlockState(foodPos));
                    h.assertTrue(level.getBlockState(foodPos).is(food), "Direct native coconut germination bypass");
                    level.setBlock(foodPos, Blocks.AIR.defaultBlockState(), 2);
                }
                Block block; BlockState mature, young;
                if (id.equals(WildFruitPolicy.COCONUT)) {
                    var pod = Pod.REGISTRY.get(new ResourceLocation(policy.harvest())); block = pod.getBlock();
                    mature = block.defaultBlockState().setValue(pod.getAgeProperty(), pod.getMaxAge());
                    young = mature.setValue(pod.getAgeProperty(), 0);
                } else {
                    var fruit = Fruit.REGISTRY.get(new ResourceLocation(policy.harvest())); block = fruit.getBlock();
                    mature = block.defaultBlockState().setValue(fruit.getAgeProperty(), fruit.getMaxAge());
                    young = mature.setValue(fruit.getAgeProperty(), 0);
                }
                level.setBlock(pos, mature, 2);
                var drops = Block.getDrops(mature, level, pos, null);
                h.assertTrue(drops.stream().filter(stack -> stack.is(product)).mapToInt(ItemStack::getCount).sum() == 8, "Mature bounty wrong " + id);
                h.assertTrue(drops.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()), "Oversized harvest " + id);
                h.assertTrue(Block.getDrops(young, level, pos, null).stream().noneMatch(stack -> stack.is(product)), "Immature bounty " + id);
                if (id.equals(WildFruitPolicy.COCONUT)) {
                    var entity = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, pos, mature);
                    try {
                        var falling = CoconutNativeFixture.fallingDrop(block, entity);
                        h.assertTrue(falling.is(product) && falling.getCount() == 8, "Falling coconut amplified incorrectly");
                    } finally { entity.discard(); }
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            h.succeed();
        } finally { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeSaguaroMegaConversionPersistsOnlyKnownOrigin(GameTestHelper h) {
        if (present(h).isEmpty()) { h.succeed(); return; }
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(4, 1, 4));
        try {
            root(h, WildFruitPolicy.SAGUARO, pos);
            h.assertTrue(new MegaSubstance().apply(level, pos), "Native saguaro mega conversion failed");
            h.assertTrue(RootLedger.speciesAt(level, pos).equals(WildFruitPolicy.MEGA_CACTUS), "Conversion changed wrong native species");
            h.assertTrue(LoadedTreeLookup.isProtected(level, pos.above()), "Mega lost inherited protection");
            h.assertTrue(level.getBlockState(pos.above()).getDestroySpeed(level, pos.above()) >= 80, "Mega inherited hardness missing");
            var ledger = RootLedger.get(level); var record = ledger.record(level, pos);
            h.assertTrue(record != null && record.source.equals(WildFruitPolicy.SAGUARO), "Missing exact provenance");
            var encoded = ledger.save(new net.minecraft.nbt.CompoundTag()); var decoded = RootLedger.load(encoded);
            h.assertTrue(decoded.record(level, pos).source.equals(WildFruitPolicy.SAGUARO), "Native SavedData reload lost origin");
            h.assertTrue(!encoded.toString().contains("lastTick"), "Runtime clock serialized");
            var future = encoded.copy(); future.putInt("schema", 99); future.putString("future_owned_data", "retain");
            var unsupported = RootLedger.load(future);
            h.assertTrue(unsupported.save(new net.minecraft.nbt.CompoundTag()).equals(future), "Unknown schema lost provenance data");
            boolean rejected = false;
            try { unsupported.requireSupported(); } catch (IllegalStateException expected) { rejected = true; }
            h.assertTrue(rejected, "Unknown schema silently accepted as an empty ledger");
            h.assertTrue(!ledger.reserve(level, pos), "Mega gained invented fruit production");
            clear(h, pos);
            root(h, "dynamictreesplus:pipe_cactus", pos);
            h.assertTrue(new MegaSubstance().apply(level, pos), "Ordinary cactus mega conversion changed");
            h.assertTrue(!LoadedTreeLookup.isProtected(level, pos.above()), "Ordinary mega cactus inferred as protected");
            h.assertTrue(!RootLedger.inherited(level, pos, WildFruitPolicy.MEGA_CACTUS), "Removed root retained provenance");
            h.succeed();
        } finally { clear(h, pos); }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void directAndWeightedOptionalRarityHaveExactGatesAndCactusFallback(GameTestHelper h) {
        var ordinary = species("dynamictrees:oak");
        for (String id : present(h)) {
            for (boolean direct : new boolean[]{false, true}) {
                for (int roll = 0; roll < 256; roll++) {
                    for (int cactusChoice = 0; cactusChoice < (id.equals(WildFruitPolicy.SAGUARO) && roll != 0 ? 7 : 1); cactusChoice++) {
                        final int gate = roll, fallback = cactusChoice;
                        var draws = new ArrayList<Integer>();
                        var random = new net.minecraft.world.level.levelgen.LegacyRandomSource(42) {
                            @Override public int nextInt(int bound) {
                                draws.add(bound);
                                return bound == 256 ? gate : bound == 7 ? fallback : 0;
                            }
                        };
                        SpeciesSelector selector = direct ? new StaticSpeciesSelector(species(id)) : new RandomSpeciesSelector().add(species(id), 1);
                        var result = selector.getSpecies(BlockPos.ZERO, Blocks.SAND.defaultBlockState(), random);
                        h.assertTrue(Collections.frequency(draws, 256) == 1, "Selection consumed duplicate/missing rarity gate " + id);
                        if (roll == 0) h.assertTrue(result.isHandled() && result.getSpecies() == species(id), "Accepted species changed");
                        else if (id.equals(WildFruitPolicy.SAGUARO)) {
                            String expected = fallback < 6 ? "pillar_cactus" : "pipe_cactus";
                            h.assertTrue(result.isHandled() && result.getSpecies() == species("dynamictreesplus:" + expected), "Wrong 6:1 cactus fallback");
                            h.assertTrue(Collections.frequency(draws, 7) == 1, "Fallback draw changed");
                        } else h.assertTrue(!result.isHandled(), "Native fallback chain prevented " + id);
                    }
                }
            }
        }
        var random = new net.minecraft.world.level.levelgen.LegacyRandomSource(42) {
            @Override public int nextInt(int bound) { throw new AssertionError("Ordinary direct selector consumed RNG"); }
        };
        h.assertTrue(new StaticSpeciesSelector(ordinary).getSpecies(BlockPos.ZERO, Blocks.DIRT.defaultBlockState(), random).getSpecies() == ordinary,
            "Ordinary direct selector changed");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void optionalRootAllowancePersistsAndChargesOnlySuccessfulPlacement(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(4, 1, 4)); var candidate = pos.above(3);
        try {
            for (String id : present(h)) {
                root(h, id, pos);
                var ledger = RootLedger.get(level); var record = ledger.record(level, pos);
                h.assertTrue(!ledger.reserve(level, pos) && record.credit == 0, "New root minted credit " + id);
                record.credit = ProductionRates.MAX_CREDIT;
                TreeFruitBudget.attempt(level, pos, () -> TreeFruitBudget.placeBlock(Blocks.STONE, level, candidate, () -> {}));
                h.assertTrue(record.credit == ProductionRates.MAX_CREDIT, "Failed native placement spent credit");
                TreeFruitBudget.attempt(level, pos, () -> {
                    TreeFruitBudget.placeBlock(Blocks.STONE, level, candidate, () -> level.setBlock(candidate, Blocks.STONE.defaultBlockState(), 2));
                    TreeFruitBudget.placeBlock(Blocks.STONE, level, candidate, () -> {});
                });
                h.assertTrue(record.credit == ProductionRates.MAX_CREDIT - ProductionRates.FRUIT_INTERVAL, "Success/existing-block accounting wrong");
                var saved = ledger.save(new net.minecraft.nbt.CompoundTag()); var restored = RootLedger.load(saved);
                h.assertTrue(restored.record(level, pos).credit == record.credit, "Restart lost production credit");
                h.assertTrue(!saved.toString().contains("lastTick"), "Offline accrual clock persisted");
                ledger.suspendChunk(pos.getX() >> 4, pos.getZ() >> 4);
                long before = record.credit; ledger.reserve(level, pos);
                h.assertTrue(record.credit == before, "Chunk reattachment minted catch-up credit");
                clear(h, pos);
            }
            h.succeed();
        } finally { clear(h, pos); }
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void optionalNativePulseCadenceUsesEachMeasuredReference(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(4, 1, 4));
        var forms = new ArrayList<>(present(h));
        if (forms.contains(WildFruitPolicy.SAGUARO)) forms.add(WildFruitPolicy.MEGA_CACTUS);
        try {
            h.assertTrue(com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_MULTIPLIER.get() == .5
                && com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_FOLDING.get() == 2, "Wrong native rate fixture");
            for (String id : forms) {
                root(h, id.equals(WildFruitPolicy.MEGA_CACTUS) ? WildFruitPolicy.SAGUARO : id, pos);
                if (id.equals(WildFruitPolicy.MEGA_CACTUS)) h.assertTrue(new MegaSubstance().apply(level, pos), "Mega rate setup failed");
                var tree = species(id);
                var part = new OptionalCountingGrowthPart();
                // Missing references deliberately throw here. Do not treat native-rate fallback as calibration.
                float rate = tree.getGrowthRate(level, pos);
                for (int draw = 0; draw < 4096; draw++) {
                    final float value = draw / 4096F;
                    var random = new net.minecraft.world.level.levelgen.LegacyRandomSource(42) {
                        @Override public float nextFloat() { return value; }
                    };
                    tree.grow(level, (com.ferreusveritas.dynamictrees.block.rooty.RootyBlock)level.getBlockState(pos).getBlock(),
                        pos, 15, part, pos.above(), random, draw % 2 == 0);
                }
                h.assertTrue(Math.abs(part.pulses - 4096 * rate * .5 * 2) < 2, "Native optional pulse frequency drifted " + id);
                h.assertTrue(Float.isFinite(part.energy) && part.energy == tree.getEnergy(level, pos), "Optional full-size energy missing " + id);
                clear(h, pos);
            }
            if (forms.isEmpty()) h.succeed(); else OptionalGrowthCalibration.verifyWoodReferences(h);
        } finally { clear(h, pos); }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeCactusAndPalmOverrideSitesUseRootAllowance(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(4, 1, 4));
        try {
            var available = present(h);
            for (String id : List.of(WildFruitPolicy.SAGUARO, WildFruitPolicy.COCONUT)) {
                if (!available.contains(id)) continue;
                root(h, id, pos); var tree = species(id);
                if (id.equals(WildFruitPolicy.COCONUT)) level.setBlock(pos.above(2), tree.getLeavesProperties().getDynamicLeavesState(4), 2);
                var feature = tree.getGenFeatures().stream().filter(c -> c.getGenFeature().getRegistryName().getPath().equals(
                    id.equals(WildFruitPolicy.SAGUARO) ? "cactus_fruit" : "palm_fruit")).findFirst().orElseThrow();
                var record = RootLedger.get(level).record(level, pos);
                level.random.setSeed(67891);
                for (int i = 0; i < 128; i++) feature.generate(GenFeature.POST_GROW, new PostGrowContext(level, pos, tree, pos.above(), 0, true));
                h.assertTrue(record.credit == 0, "Empty optional allowance changed");
                record.credit = ProductionRates.MAX_CREDIT;
                for (int i = 0; i < 128; i++) feature.generate(GenFeature.POST_GROW, new PostGrowContext(level, pos, tree, pos.above(), 0, true));
                h.assertTrue(record.credit < ProductionRates.MAX_CREDIT && record.credit >= 0, "Real native override bypassed success accounting " + id);
                h.assertTrue((ProductionRates.MAX_CREDIT - record.credit) % ProductionRates.FRUIT_INTERVAL == 0, "Fractional replacement debit");
                clear(h, pos);
            }
            h.succeed();
        } finally { clear(h, pos); }
    }
}
