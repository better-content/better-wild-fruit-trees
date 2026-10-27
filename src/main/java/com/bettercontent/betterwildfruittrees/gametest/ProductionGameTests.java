package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import com.ferreusveritas.dynamictrees.systems.genfeature.*;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProductionGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeBushGrowthAndHarvestMatchNormalizedRates(GameTestHelper h) {
        var level = h.getLevel();
        // Use the existing native marker's illuminated corner; do not alter marker/structure blocks.
        var pos = h.absolutePos(new BlockPos(0, 1, 1));
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var lightPos = h.absolutePos(new BlockPos(-1, -1, -1));
        h.startSequence().thenWaitUntil(() -> {
            var lightState = level.getBlockState(lightPos);
            var chunk = level.getChunkSource().getChunkNow(lightPos.getX() >> 4, lightPos.getZ() >> 4);
            h.assertTrue(level.getRawBrightness(pos.above(), 0) >= 9,
                    "Waiting for native bush light >=9 at " + pos.above() + " actual=" + level.getRawBrightness(pos.above(), 0)
                            + " lightPos=" + lightPos + " lightState=" + lightState
                            + " cachedEmission=" + lightState.getLightEmission() + " contextualEmission=" + lightState.getLightEmission(level, lightPos)
                            + " blockAtSource=" + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, lightPos)
                            + " blockAtSample=" + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos.above())
                            + " chunkLightCorrect=" + (chunk == null ? "not_loaded" : chunk.isLightCorrect()));
        }).thenExecute(() -> {
            System.out.println("Native bush light ready: normalizedRates pos=" + pos + " lightPos=" + lightPos
                    + " lightState=" + level.getBlockState(lightPos) + " brightnessAbove=" + level.getRawBrightness(pos.above(), 0)
                    + " blockAtSample=" + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos.above())
                    + " cropState=" + level.getBlockState(pos) + " soilState=" + level.getBlockState(pos.below()));
            for (String id : WildBushes.BLOCKS) {
                h.assertTrue(level.getRawBrightness(pos.above(), 0) >= 9, "Native growth light prerequisite lost before " + id);
                var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
                if (block == null || block == Blocks.AIR) continue;
                var ripe = WildBushes.mature(block.defaultBlockState());
                var age = (IntegerProperty)ripe.getProperties().stream().filter(p -> p.getName().equals("age")).findFirst().orElseThrow();
                level.setBlock(pos, ripe, 2);
                ripe.use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                int initialHarvest = take(level, pos); // Initial ripe stock remains outside renewable-rate accounting.
                var reset = level.getBlockState(pos);
                int initialLight = level.getRawBrightness(pos.above(), 0);
                h.assertTrue(initialHarvest > 0 && reset.is(block) && reset.getValue(age) < ripe.getValue(age),
                        "Native initial harvest/reset prerequisite failed " + id + " harvested=" + initialHarvest
                                + " reset=" + reset + " lightAbove=" + initialLight);
                if (id.equals("ars_nouveau:sourceberry_bush")) {
                    h.assertTrue(initialLight >= 9, "Native Ars growth requires light above >=9, actual=" + initialLight
                            + " reset=" + reset + " initialHarvest=" + initialHarvest);
                }
                var random = net.minecraft.util.RandomSource.create(0xBE77);
                int output = 0, trials = 50_000;
                for (int i = 0; i < trials; i++) {
                    block.randomTick(level.getBlockState(pos), level, pos, random);
                    var state = level.getBlockState(pos);
                    if (state.getValue(age).equals(ripe.getValue(age))) {
                        state.use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                        output += take(level, pos);
                    }
                }
                h.assertTrue(level.getRawBrightness(pos.above(), 0) >= 9, "Native growth light prerequisite lost after " + id);
                double yield = id.startsWith("fruitsdelight:") ? 1.5 : 2.5;
                double expected = trials * ProductionRates.bushGrowthChance(id) / 2 * yield;
                h.assertTrue(Math.abs(output - expected) < expected * .2,
                        "Native renewable output outside statistical band " + id + " expected=" + expected + " actual=" + output
                                + " initialHarvest=" + initialHarvest + " reset=" + reset + " initialLightAbove=" + initialLight
                                + " finalState=" + level.getBlockState(pos) + " finalLightAbove=" + level.getRawBrightness(pos.above(), 0)
                                + " lightPos=" + lightPos + " lightState=" + level.getBlockState(lightPos));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            // A season/other-mod veto must remain authoritative, as must explicit growth boosts.
            var hooks = new WildFruitTreesHooks();
            hooks.check(h, level, pos);
        }).thenSucceed();
    }

    private static int take(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        int count = 0;
        for (var entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) {
            count += entity.getItem().getCount();
            entity.discard();
        }
        return count;
    }

    private static final class NativeFruitFeature extends FruitGenFeature {
        NativeFruitFeature() { super(new ResourceLocation(WildFruitTrees.MOD_ID, "rate_fixture")); }
        void run(GenFeatureConfiguration config, PostGrowContext context) { super.postGrow(config, context); }
    }

    @GameTest(template = "field", timeoutTicks = 100)
    public static void nativeTreePlacementSpendsOnlySuccessfulReplacementsAndPersists(GameTestHelper h) {
        var level = h.getLevel();
        var root = h.absolutePos(new BlockPos(32, 1, 32));
        var tree = Species.REGISTRY.get(new ResourceLocation("dtfruitsdelight:pear"));
        var fruit = Fruit.REGISTRY.get(new ResourceLocation("dtfruitsdelight:pear"));
        level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
        tree.placeRootyDirtBlock(level, root, 15);
        var entity = level.getBlockEntity(root);
        h.assertTrue(entity != null, "Native fruit root lacks persistent storage");
        var branch = tree.getFamily().getBranch().orElseThrow();
        branch.setRadius(level, root.above(), 8, Direction.DOWN, 2);
        for (int y=2;y<=5;y++) branch.setRadius(level, root.above(y), 6-y, Direction.DOWN, 2);
        // Native rays approach branch ends from below/outside the canopy. Keep the end inside
        // dynamic foliage with exposed undersides above the ground, as on an actual fruit tree.
        for (int x=-7;x<=7;x++) for(int z=-7;z<=7;z++) for(int y=4;y<=6;y++)
            if(x!=0 || z!=0 || y>5) level.setBlock(root.offset(x,y,z), tree.getLeavesProperties().getDynamicLeavesState(4), 2);
        var leaf = root.offset(1,4,0);
        h.assertTrue(tree.getFamily().isCompatibleGenericLeaves(tree, level.getBlockState(leaf), level, leaf),
                "Native fruit placement must recognize the fixture's dynamic canopy");
        var feature = new NativeFruitFeature();
        var config = feature.createDefaultConfiguration().with(FruitGenFeature.FRUIT, fruit);
        var context = new PostGrowContext(level, root, tree, root.above(), 15, true);
        feature.run(config, context);
        var data = entity.getPersistentData().getCompound(TreeFruitBudget.KEY);
        h.assertTrue(data.contains("lastTick") && data.getLong("credit") == 0, "No unpaid initial replacement");
        int output = 0;
        for (int pulse = 0; pulse < 20; pulse++) {
            // Controlled elapsed active time, without changing this shared GameTest world's clock.
            data.putLong("lastTick", level.getGameTime() - ProductionRates.FRUIT_INTERVAL);
            boolean[] funded = {false};
            TreeFruitBudget.attempt(level, root, () -> funded[0] = true);
            h.assertTrue(funded[0], "Elapsed native allowance was not available: " + data + " actual=" + entity.getPersistentData().getCompound(TreeFruitBudget.KEY));
            for (int retry = 0; retry < 256; retry++) {
                feature.run(config, context);
                if (data.getLong("credit") < ProductionRates.FRUIT_INTERVAL) break;
            }
            h.assertTrue(data.getLong("credit") == 0, "Accessible native canopy failed to spend one replacement allowance, pulse=" + pulse + " data=" + data
                    + " candidate=" + com.ferreusveritas.dynamictrees.util.CoordUtils.getRayTraceFruitPos(level, tree, root.above(), root.above(5), com.ferreusveritas.dynamictrees.util.SafeChunkBounds.ANY));
            for (int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) {
                var pos = root.offset(x,3,z);
                var state = level.getBlockState(pos);
                if (state.is(fruit.getBlock())) {
                    var ripe = state.setValue(fruit.getAgeProperty(), fruit.getMaxAge());
                    output += Block.getDrops(ripe, level, pos, null).stream().mapToInt(ItemStack::getCount).sum();
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        h.assertTrue(output == 160, "Twenty actual native replacements must fund exactly 160 items, got " + output);
        // Occluded leaf undersides are not fruit sites and must not consume production allowance.
        for (int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) if(x!=0 || z!=0)
            level.setBlock(root.offset(x,3,z), Blocks.STONE.defaultBlockState(), 2);
        data.putLong("credit", ProductionRates.FRUIT_INTERVAL);
        data.putLong("lastTick", level.getGameTime());
        for (int i=0;i<64;i++) feature.run(config, context);
        h.assertTrue(data.getLong("credit") == ProductionRates.FRUIT_INTERVAL, "Blocked geometry consumed replacement allowance");
        data.putLong("credit", 2000);
        data.putLong("lastTick", level.getGameTime() - 50_000_000);
        var encoded = entity.saveWithFullMetadata();
        h.assertTrue(encoded.contains(TreeFruitBudget.KEY), "Native root serialization omitted fruit credit");
        var decoded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(root, level.getBlockState(root), encoded.copy());
        h.assertTrue(decoded != null && decoded != entity, "Credit reload must reconstruct a fresh native entity");
        level.setBlockEntity(decoded);
        entity = decoded;
        entity.setLevel(level);
        var restored = entity.getPersistentData().getCompound(TreeFruitBudget.KEY);
        h.assertTrue(!restored.contains("lastTick"), "Data/level reattachment retained an offline timestamp");
        restored.putLong("lastTick", level.getGameTime() - 50_000_000);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.event.level.ChunkEvent.Load(level.getChunkAt(root), false));
        h.assertTrue(!restored.contains("lastTick"), "Existing-chunk reattachment retained an offline timestamp");
        TreeFruitBudget.attempt(level, root, () -> { throw new AssertionError("Reload minted replacement credit"); });
        h.assertTrue(entity.getPersistentData().getCompound(TreeFruitBudget.KEY).getLong("credit") == 2000,
                "Native save/load must preserve fractional allowance without unloaded-time catch-up");
        h.succeed();
    }

    private static final class WildFruitTreesHooks {
        void check(GameTestHelper h, net.minecraft.server.level.ServerLevel level, BlockPos pos) {
            for (var result : new net.minecraftforge.eventbus.api.Event.Result[]{
                    net.minecraftforge.eventbus.api.Event.Result.DENY, net.minecraftforge.eventbus.api.Event.Result.ALLOW}) {
                var event = new net.minecraftforge.event.level.BlockEvent.CropGrowEvent.Pre(level, pos,
                        Blocks.SWEET_BERRY_BUSH.defaultBlockState());
                event.setResult(result);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                h.assertTrue(event.getResult() == result, "Growth veto/boost overwritten");
            }
        }
    }
}
