package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import com.ferreusveritas.dynamictrees.api.treedata.TreePart;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MaturationGameTests {
    private static TreePart capture(float[] energy) {
        return (TreePart)java.lang.reflect.Proxy.newProxyInstance(TreePart.class.getClassLoader(), new Class<?>[]{TreePart.class}, (proxy, method, args) -> {
            if (!method.getName().equals("growSignal")) throw new AssertionError("Unexpected native TreePart call " + method);
            var signal = (GrowSignal)args[2];
            energy[0] = signal.energy;
            signal.success = false;
            return signal;
        });
    }
    private static float emit(Species tree, Level level, BlockPos root) throws Exception {
        var method = Species.class.getDeclaredMethod("sendGrowthSignal", TreePart.class, Level.class, BlockPos.class, BlockPos.class, Direction.class);
        method.setAccessible(true);
        float[] energy = {Float.NaN};
        method.invoke(tree, capture(energy), level, root.above(), root, Direction.UP);
        return energy[0];
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allProtectedNativeSignalsAndBranchTaperMatureOverThirtyDays(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var root = h.absolutePos(new BlockPos(4, 1, 4));
        var trunk = root.above();
        var originalField = Species.class.getDeclaredField("signalEnergy");
        originalField.setAccessible(true);
        // Exercise the real protochunk/DUMMY-block-entity birth path, not only fully loaded roots.
        var center = new net.minecraft.world.level.ChunkPos(root);
        var chunks = new java.util.ArrayList<net.minecraft.world.level.chunk.ChunkAccess>();
        for (int z=-1;z<=1;z++) for(int x=-1;x<=1;x++) {
            var proto = new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(center.x+x, center.z+z),
                    net.minecraft.world.level.chunk.UpgradeData.EMPTY, level,
                    level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME), null);
            proto.setStatus(net.minecraft.world.level.chunk.ChunkStatus.FEATURES);
            chunks.add(proto);
        }
        var region = new net.minecraft.server.level.WorldGenRegion(level, chunks, net.minecraft.world.level.chunk.ChunkStatus.FEATURES, 1);
        for (String fruit : WildFruitPolicy.ENERGY.keySet()) {
            String id = fruit.equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:" + fruit;
            var tree = Species.REGISTRY.get(new ResourceLocation(id));
            region.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
            h.assertTrue(tree.placeRootyDirtBlock(region, root, 0), "Native protochunk root placement failed " + id);
            var generated = region.getBlockEntity(root);
            h.assertTrue(generated != null && generated.getPersistentData().contains(TreeMaturation.KEY), "Worldgen root missed birth data " + id);
            var generatedState = region.getBlockState(root);
            h.assertTrue(generatedState.getValue(RootyBlock.FERTILITY) == 0, "Native JoCode-style root must start exhausted " + id);
            var generatedEvent = new com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent(region, tree, root,
                    java.util.List.of(), com.ferreusveritas.dynamictrees.util.SafeChunkBounds.ANY, Blocks.DIRT.defaultBlockState());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(generatedEvent);
            h.assertTrue(region.getBlockState(root).getValue(RootyBlock.FERTILITY) == 15, "Generated tree cannot start additional growth " + id);
            region.setBlock(root, region.getBlockState(root).setValue(RootyBlock.FERTILITY, 7), 2);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent(region, tree, root,
                    java.util.List.of(), com.ferreusveritas.dynamictrees.util.SafeChunkBounds.ANY, Blocks.DIRT.defaultBlockState()));
            h.assertTrue(region.getBlockState(root).getValue(RootyBlock.FERTILITY) == 7, "Repeated generation notification refueled soil " + id);
            var savedBirth = region.getChunk(root).getBlockEntityNbtForSaving(root);
            h.assertTrue(savedBirth != null && savedBirth.contains(TreeMaturation.KEY)
                    && savedBirth.getCompound(TreeMaturation.KEY).getLong("age") == 0,
                    "Protochunk serialization lost maturation birth data " + id);
            var decodedBirth = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(root, region.getBlockState(root), savedBirth.copy());
            h.assertTrue(decodedBirth != null && decodedBirth != generated && decodedBirth.getPersistentData().contains(TreeMaturation.KEY)
                    && decodedBirth.getPersistentData().getCompound(TreeMaturation.KEY).getLong("age") == 0
                    && decodedBirth.getPersistentData().getCompound(TreeMaturation.KEY).getBoolean("worldgenEstablished"),
                    "Fresh native protochunk entity lost its birth record or one-time fertility marker " + id);
            level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
            h.assertTrue(tree.placeRootyDirtBlock(level, root, 15), "Native root placement failed " + id);
            var entity = level.getBlockEntity(root);
            h.assertTrue(entity != null && entity.getPersistentData().contains(TreeMaturation.KEY), "New root lacks birth data " + id);
            var data = entity.getPersistentData().getCompound(TreeMaturation.KEY);
            h.assertTrue(data.getLong("age") == 0, "New root started mature " + id);
            var branch = tree.getFamily().getBranch().orElseThrow();
            for (var direction : Direction.values()) if (direction != Direction.DOWN)
                level.setBlock(trunk.relative(direction), Blocks.STONE.defaultBlockState(), 2);
            float original = originalField.getFloat(tree), mature = tree.getSignalEnergy(), taper = tree.getTapering();
            h.assertTrue(original > 0 && original < mature, "Pinned native/enlarged energy relation changed " + id);
            for (long age : new long[]{0, TreeGrowthSchedule.DURATION/2, TreeGrowthSchedule.DURATION-1, TreeGrowthSchedule.DURATION}) {
                data.putLong("age", age);
                data.putLong("lastInhabitedTick", level.getChunkAt(root).getInhabitedTime());
                float expected = TreeGrowthSchedule.energy(tree.getEnergy(level, root), original, mature, age);
                h.assertTrue(Math.abs(emit(tree, level, root) - expected) < .00001F, "Native emitted energy ignored maturation " + id + " age=" + age);
                if (age < TreeGrowthSchedule.DURATION)
                    h.assertTrue(expected < tree.getEnergy(level, root), "Full potential unlocked before day thirty " + id);
                branch.setRadius(level, trunk, 1, Direction.DOWN, 2);
                var signal = new GrowSignal(tree, root, mature, RandomSource.create(42), Direction.UP);
                signal.radius = 2;
                var grown = branch.growSignal(level, trunk, signal);
                float expectedRadius = 2 + TreeGrowthSchedule.interpolate(taper, taper/2.5F, age);
                h.assertTrue(Math.abs(grown.radius - expectedRadius) < .00001F,
                        "Native branch taper ignored maturation " + id + " age=" + age + " radius=" + grown.radius + " expected=" + expectedRadius);
                h.assertTrue(tree.getTapering() == taper, "Maturation changed JoCode/InflatorNode's native getter " + id);
                h.assertTrue(TreeMaturation.peek(level, root) == age, "Growth pulses awarded elapsed time " + id);
            }
            level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
            level.setBlock(trunk, Blocks.AIR.defaultBlockState(), 2);
        }
        var ordinary = Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak"));
        ordinary.placeRootyDirtBlock(level, root, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent(level, ordinary, root,
                java.util.List.of(), com.ferreusveritas.dynamictrees.util.SafeChunkBounds.ANY, Blocks.DIRT.defaultBlockState()));
        h.assertTrue(level.getBlockState(root).getValue(RootyBlock.FERTILITY) == 0, "Ordinary worldgen root was refueled");
        var ordinaryEntity = level.getBlockEntity(root);
        h.assertTrue(ordinaryEntity == null || !ordinaryEntity.getPersistentData().contains(TreeMaturation.KEY), "Ordinary tree got a maturation ledger");
        h.assertTrue(emit(ordinary, level, root) == ordinary.getEnergy(level, root), "Ordinary emitted energy changed");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void maturationPersistsWithoutCalendarBonemealOrOfflineAcceleration(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var root = h.absolutePos(new BlockPos(4, 1, 4));
        var tree = Species.REGISTRY.get(new ResourceLocation("dtfruitsdelight:pear"));
        level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
        tree.placeRootyDirtBlock(level, root, 15);
        var entity = level.getBlockEntity(root);
        var data = entity.getPersistentData().getCompound(TreeMaturation.KEY);
        var production = new net.minecraft.nbt.CompoundTag();
        production.putLong("credit", 1234);
        entity.getPersistentData().put(TreeFruitBudget.KEY, production);
        h.assertTrue(TreeMaturation.observe(level, root) == 0, "First observation minted age");
        data.putLong("lastInhabitedTick", level.getChunkAt(root).getInhabitedTime() - ProductionRates.DAY_TICKS);
        h.assertTrue(TreeMaturation.observe(level, root) == ProductionRates.DAY_TICKS, "Active chunk tick accounting failed");
        for (int i=0;i<10;i++) {
            float[] energy = {Float.NaN};
            var random = new LegacyRandomSource(42) { @Override public float nextFloat() { return 0; } };
            tree.grow(level, (RootyBlock)level.getBlockState(root).getBlock(), root, 15, capture(energy), root.above(), random, false);
            h.assertTrue(Float.isFinite(energy[0]), "Bonemeal fixture did not emit a native growth signal");
            h.assertTrue(TreeMaturation.peek(level, root) == ProductionRates.DAY_TICKS, "Bonemeal/repeated same-tick growth skipped age");
        }
        long calendar = level.getDayTime();
        try {
            level.setDayTime(calendar + 30 * 24_000L);
            h.assertTrue(TreeMaturation.observe(level, root) == ProductionRates.DAY_TICKS, "Skipped calendar time awarded maturation");
        } finally { level.setDayTime(calendar); }
        TreeMaturation.start(level, root);
        h.assertTrue(TreeMaturation.peek(level, root) == ProductionRates.DAY_TICKS, "Repeated root initialization reset age");
        data.putLong("lastInhabitedTick", Long.MIN_VALUE);
        var encoded = entity.saveWithFullMetadata();
        h.assertTrue(encoded.contains(TreeMaturation.KEY) && encoded.contains(TreeFruitBudget.KEY), "Native serialization omitted owned root records");
        var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(root, level.getBlockState(root), encoded.copy());
        h.assertTrue(restored != null && restored != entity, "Reload must reconstruct a fresh native root entity");
        level.setBlockEntity(restored);
        entity = restored;
        entity.setLevel(level);
        h.assertTrue(TreeMaturation.observe(level, root) == ProductionRates.DAY_TICKS, "Save/reload awarded offline growth or lost age");
        entity.getPersistentData().getCompound(TreeMaturation.KEY).putLong("lastInhabitedTick", Long.MIN_VALUE);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.ChunkEvent.Load(level.getChunkAt(root), false));
        h.assertTrue(TreeMaturation.observe(level, root) == ProductionRates.DAY_TICKS, "Existing-chunk reattachment skipped maturation");
        h.assertTrue(entity.getPersistentData().getCompound(TreeFruitBudget.KEY).getLong("credit") == 1234, "Maturation rewrote independent fruit allowance");
        entity.getPersistentData().remove(TreeMaturation.KEY);
        h.assertTrue(TreeMaturation.observe(level, root) == TreeGrowthSchedule.DURATION && emit(tree, level, root) == tree.getEnergy(level, root), "Legacy tree did not keep full potential");
        h.assertTrue(!entity.getPersistentData().contains(TreeMaturation.KEY), "Legacy observation created a new birth ledger");
        level.setBlock(root, level.getBlockState(root).setValue(RootyBlock.FERTILITY, 0), 2);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent(level, tree, root,
                java.util.List.of(), com.ferreusveritas.dynamictrees.util.SafeChunkBounds.ANY, Blocks.DIRT.defaultBlockState()));
        h.assertTrue(level.getBlockState(root).getValue(RootyBlock.FERTILITY) == 0, "Legacy root was refueled");
        var absent = new BlockPos(2_000_000, 100, 2_000_000);
        h.assertTrue(!level.hasChunkAt(absent), "Unloaded fixture chunk already exists");
        TreeMaturation.start(level, absent);
        h.assertTrue(TreeMaturation.observe(level, absent) == 0 && !level.hasChunkAt(absent), "Maturation force-loaded a missing root");
        h.succeed();
    }
}
