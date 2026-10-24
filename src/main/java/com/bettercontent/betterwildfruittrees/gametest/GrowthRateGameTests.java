package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GrowthRateGameTests {
    // Adult-size/manual-pulse calibration is retired by explicit user direction.
    // The independent native cadence, constant-rate and persistence contract remains below.

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nativeGrowthFrequencyIsConstantWithoutAgeOrSizeTransitions(GameTestHelper h) {
        var level = h.getLevel();
        var root = h.absolutePos(new BlockPos(4, 1, 4));
        var config = com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_MULTIPLIER;
        var folding = com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_FOLDING;
        h.assertTrue(config.get() == .5 && folding.get() == 2, "Fixture must use pinned pack growth settings");
        long calendar = level.getDayTime();
        var ordinary = Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak"));
        float ordinaryBaseline = ordinary.getGrowthRate(level,root);
        try {
            for(String fruit : WildFruitPolicy.ENERGY.keySet()) {
                String id = fruit.equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:"+fruit;
                var tree = Species.REGISTRY.get(new ResourceLocation(id));
                level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
                tree.placeRootyDirtBlock(level, root, 0);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent(level,tree,root,
                    java.util.List.of(),SafeChunkBounds.ANY,Blocks.DIRT.defaultBlockState()));
                h.assertTrue(level.getBlockState(root).getValue(com.ferreusveritas.dynamictrees.block.rooty.RootyBlock.FERTILITY)==0,"Protected worldgen root was refueled " + id);
                ((com.ferreusveritas.dynamictrees.block.rooty.RootyBlock)level.getBlockState(root).getBlock()).setFertility(level,root,15);
                var entity = level.getBlockEntity(root);
                String obsoleteAgeKey = WildFruitTrees.MOD_ID+":maturation";
                h.assertTrue(!entity.getPersistentData().contains(obsoleteAgeKey), "Root placement created an age ledger " + id);
                float baseline = TreeGrowthRates.reference(id).nativeRate();
                float season = tree.seasonalGrowthFactor(com.ferreusveritas.dynamictrees.util.LevelContext.create(level), root);
                float rate = TreeGrowthRates.rate(id, baseline*season);
                h.assertTrue(Math.abs(tree.getGrowthRate(level,root)-rate)<.000001F, "Native getter missed constant normalization " + id);
                var branch = tree.getFamily().getBranch().orElseThrow();
                var trunk = root.above();
                for(var direction : Direction.values()) if(direction != Direction.DOWN)
                    level.setBlock(trunk.relative(direction), Blocks.STONE.defaultBlockState(), 2);
                var capture = new CountingGrowthPart();
                for(int draw=0;draw<4096;draw++) {
                    final float value=draw/4096F;
                    var random = new net.minecraft.world.level.levelgen.LegacyRandomSource(42) {
                        @Override public float nextFloat() { return value; }
                    };
                    tree.grow(level,(com.ferreusveritas.dynamictrees.block.rooty.RootyBlock)level.getBlockState(root).getBlock(),root,
                        15,capture,trunk,random,draw%2==0);
                }
                double expected=4096*rate*config.get()*folding.get();
                h.assertTrue(Math.abs(capture.pulses-expected)<2, "Native growth pulse cadence ignored normalized rate " + id);
                h.assertTrue(capture.energy == tree.getEnergy(level,root), "Young tree had an energy cap " + id);
                // An obsolete record from the superseded implementation must not affect growth.
                for(long age : new long[]{0,TreeGrowthRates.TARGET_TICKS-1,TreeGrowthRates.TARGET_TICKS,TreeGrowthRates.TARGET_TICKS+1}) {
                    var obsolete = new net.minecraft.nbt.CompoundTag();
                    obsolete.putLong("age",age);
                    entity.getPersistentData().put(obsoleteAgeKey,obsolete);
                    level.setDayTime(calendar+age);
                    h.assertTrue(tree.getGrowthRate(level,root)==rate, "Age/calendar introduced a rate transition " + id);
                    h.assertTrue(tree.getSignalEnergy()==WildFruitPolicy.ENERGY.get(fruit), "Age changed full-size energy " + id);
                    branch.setRadius(level,trunk,1,Direction.DOWN,2);
                    var signal=new GrowSignal(tree,root,tree.getEnergy(level,root),RandomSource.create(42),Direction.UP);
                    signal.radius=2;
                    var grown=branch.growSignal(level,trunk,signal);
                    h.assertTrue(Math.abs(grown.radius-(2+tree.getTapering()/2.5F))<.000001F, "Age changed full-size taper " + id);
                }
                level.setDayTime(calendar);
                entity.getPersistentData().remove(obsoleteAgeKey);
                var encoded=entity.saveWithFullMetadata();
                h.assertTrue(!encoded.contains(obsoleteAgeKey), "Growth saved an age ledger " + id);
                var decoded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(root,level.getBlockState(root),encoded.copy());
                level.setBlockEntity(decoded);
                h.assertTrue(tree.getGrowthRate(level,root)==rate, "Reload changed growth rate " + id);
            }
            h.assertTrue(ordinary.getGrowthRate(level,root)==ordinaryBaseline, "Ordinary growth rate changed");
            level.setBlock(root,Blocks.DIRT.defaultBlockState(),2);
            ordinary.placeRootyDirtBlock(level,root,0);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent(level,ordinary,root,
                java.util.List.of(),SafeChunkBounds.ANY,Blocks.DIRT.defaultBlockState()));
            h.assertTrue(level.getBlockState(root).getValue(com.ferreusveritas.dynamictrees.block.rooty.RootyBlock.FERTILITY)==0,"Worldgen fertility was refilled");
            h.succeed();
        } finally {level.setDayTime(calendar);}
    }

    private static final class CountingGrowthPart implements com.ferreusveritas.dynamictrees.api.treedata.TreePart {
        private int pulses;
        private float energy = Float.NaN;

        private static AssertionError unexpected() {
            return new AssertionError("Unexpected native growth-part query");
        }

        @Override
        public com.ferreusveritas.dynamictrees.api.cell.Cell getHydrationCell(
                net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                Direction direction, com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties leaves) {
            throw unexpected();
        }

        @Override
        public GrowSignal growSignal(net.minecraft.world.level.Level level, BlockPos pos, GrowSignal signal) {
            pulses++;
            energy = signal.energy;
            signal.success = false;
            return signal;
        }

        @Override
        public int probabilityForBlock(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.BlockGetter level, BlockPos pos,
                com.ferreusveritas.dynamictrees.block.branch.BranchBlock branch) {
            throw unexpected();
        }

        @Override
        public int getRadiusForConnection(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.BlockGetter level, BlockPos pos,
                com.ferreusveritas.dynamictrees.block.branch.BranchBlock branch, Direction direction, int radius) {
            throw unexpected();
        }

        @Override
        public int getRadius(net.minecraft.world.level.block.state.BlockState state) {
            throw unexpected();
        }

        @Override
        public boolean shouldAnalyse(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.BlockGetter level, BlockPos pos) {
            throw unexpected();
        }

        @Override
        public com.ferreusveritas.dynamictrees.api.network.MapSignal analyse(
                net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelAccessor level,
                BlockPos pos, Direction direction, com.ferreusveritas.dynamictrees.api.network.MapSignal signal) {
            throw unexpected();
        }

        @Override
        public com.ferreusveritas.dynamictrees.tree.family.Family getFamily(
                net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
            throw unexpected();
        }

        @Override
        public int branchSupport(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.BlockGetter level, com.ferreusveritas.dynamictrees.block.branch.BranchBlock branch,
                BlockPos pos, Direction direction, int radius) {
            throw unexpected();
        }

        @Override
        public com.ferreusveritas.dynamictrees.api.treedata.TreePart.TreePartType getTreePartType() {
            throw unexpected();
        }
    }
}
