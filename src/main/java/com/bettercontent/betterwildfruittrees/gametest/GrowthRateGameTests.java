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
    private static final java.util.Map<String, Integer> sums = new java.util.LinkedHashMap<>();
    @GameTest(template = "growth", timeoutTicks = 2000)
    public static void nativeFreshGrowthCalibration(GameTestHelper h) { sums.clear(); probe(h,0); }
    private static void probe(GameTestHelper h, int index) {
        var fruits = new java.util.TreeSet<>(WildFruitPolicy.ENERGY.keySet()).stream().toList();
        if(index == fruits.size()*8) {h.succeed();return;}
        var level = h.getLevel();
        var root = h.absolutePos(new BlockPos(48, 160, 48));
        String fruit=fruits.get(index/8);
        int sample=index%8;
        String id = fruit.equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:" + fruit;
        var tree = Species.REGISTRY.get(new ResourceLocation(id));
        for (var pos : BlockPos.betweenClosed(root.offset(-44, 0, -44), root.offset(44, 60, 44)))
            if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
        tree.placeRootyDirtBlock(level, root, 15);
        var branch = tree.getFamily().getBranch().orElseThrow();
        branch.setRadius(level, root.above(), 1, Direction.DOWN, 2);
        level.setBlock(root.above(2), tree.getLeavesProperties().getDynamicLeavesState(4), 2);
        level.setDayTime(6000);
        level.updateSkyBrightness();
        // GameTest origins are below this native world's terrain. Use an above-ground,
        // unobstructed plot and let its real light settle; never bypass leaf suitability.
        h.runAfterDelay(5, () -> {
            h.assertTrue(level.getMaxLocalRawBrightness(root.above(3)) >= 13, "Calibration fixture is not sunlit " + id);
            var random = RandomSource.create(12345 + 104729L*sample);
            level.random.setSeed(54321 + 104729L*sample);
            int firstMax = -1;
            for (int pulse=1;pulse<=4096;pulse++) {
                branch.growSignal(level, root.above(), new GrowSignal(tree, root, tree.getEnergy(level, root), random, Direction.UP));
                if (pulse % 16 == 0) TreeHelper.ageVolume(level, root.above(), 20, 48, 1, SafeChunkBounds.ANY);
                if (branch.getRadius(level.getBlockState(root.above())) == tree.getMaxBranchRadius()) {firstMax=pulse; break;}
            }
            h.assertTrue(firstMax > 0, "Fresh native tree did not reach native full trunk radius " + id);
            sums.merge(fruit,firstMax,Integer::sum);
            System.out.println("GROWTH_SAMPLE " + fruit + " seed="+sample+" pulses="+firstMax);
            if(sample==7) {
                double measured = sums.get(fruit)/8.0;
                double reference = TreeGrowthRates.reference(id).pulses();
                System.out.println("GROWTH_REFERENCE " + fruit + " meanPulses="+measured+" calibrated="+reference);
                h.assertTrue(Math.abs(measured/reference-1) < .20, "Native adult-size calibration drifted " + id);
            }
            probe(h,index+1);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nativeGrowthFrequencyIsConstantWithoutAgeOrSizeTransitions(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var root = h.absolutePos(new BlockPos(4, 1, 4));
        var field = Species.class.getDeclaredField("growthRate");
        field.setAccessible(true);
        var config = com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_MULTIPLIER;
        var folding = com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_FOLDING;
        h.assertTrue(config.get() == .5 && folding.get() == 2, "Fixture must use pinned pack growth settings");
        long calendar = level.getDayTime();
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
                float baseline = field.getFloat(tree);
                h.assertTrue(baseline == TreeGrowthRates.reference(id).nativeRate(), "Pinned native base rate drifted " + id);
                float season = tree.seasonalGrowthFactor(com.ferreusveritas.dynamictrees.util.LevelContext.create(level), root);
                float rate = TreeGrowthRates.rate(id, baseline*season);
                h.assertTrue(Math.abs(tree.getGrowthRate(level,root)-rate)<.000001F, "Native getter missed constant normalization " + id);
                var branch = tree.getFamily().getBranch().orElseThrow();
                var trunk = root.above();
                for(var direction : Direction.values()) if(direction != Direction.DOWN)
                    level.setBlock(trunk.relative(direction), Blocks.STONE.defaultBlockState(), 2);
                int[] pulses={0};
                float[] emitted={Float.NaN};
                var capture = (com.ferreusveritas.dynamictrees.api.treedata.TreePart)java.lang.reflect.Proxy.newProxyInstance(
                    Species.class.getClassLoader(), new Class<?>[]{com.ferreusveritas.dynamictrees.api.treedata.TreePart.class}, (proxy,method,args)-> {
                        if(!method.getName().equals("growSignal")) throw new AssertionError("Unexpected native growth-part call "+method);
                        var signal=(GrowSignal)args[2];
                        pulses[0]++;
                        emitted[0]=signal.energy;
                        signal.success=false;
                        return signal;
                    });
                for(int draw=0;draw<4096;draw++) {
                    final float value=draw/4096F;
                    var random = new net.minecraft.world.level.levelgen.LegacyRandomSource(42) {
                        @Override public float nextFloat() { return value; }
                    };
                    tree.grow(level,(com.ferreusveritas.dynamictrees.block.rooty.RootyBlock)level.getBlockState(root).getBlock(),root,
                        15,capture,trunk,random,draw%2==0);
                }
                double expected=4096*rate*config.get()*folding.get();
                h.assertTrue(Math.abs(pulses[0]-expected)<2, "Native growth pulse cadence ignored normalized rate " + id);
                h.assertTrue(emitted[0] == tree.getEnergy(level,root), "Young tree had an energy cap " + id);
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
            var ordinary=Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak"));
            float original=field.getFloat(ordinary)*ordinary.seasonalGrowthFactor(com.ferreusveritas.dynamictrees.util.LevelContext.create(level),root);
            h.assertTrue(ordinary.getGrowthRate(level,root)==original, "Ordinary growth rate changed");
            level.setBlock(root,Blocks.DIRT.defaultBlockState(),2);
            ordinary.placeRootyDirtBlock(level,root,0);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent(level,ordinary,root,
                java.util.List.of(),SafeChunkBounds.ANY,Blocks.DIRT.defaultBlockState()));
            h.assertTrue(level.getBlockState(root).getValue(com.ferreusveritas.dynamictrees.block.rooty.RootyBlock.FERTILITY)==0,"Worldgen fertility was refilled");
            h.succeed();
        } finally {level.setDayTime(calendar);}
    }
}
