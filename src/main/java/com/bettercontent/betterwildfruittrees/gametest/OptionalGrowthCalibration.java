package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.systems.substance.MegaSubstance;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.*;

/** Diagnostic namespace only: never a substitute for the full acceptance gate or a source of invented references. */
@GameTestHolder("better_wild_fruit_calibration")
@PrefixGameTestTemplate(false)
public final class OptionalGrowthCalibration {
    private static final List<String> FORMS = List.of(WildFruitPolicy.SAGUARO, WildFruitPolicy.COCONUT,
        WildFruitPolicy.ANCIENT, WildFruitPolicy.MEGA_CACTUS);
    private static final Map<String, Integer> SUMS = new LinkedHashMap<>();
    @GameTest(template = "growth", templateNamespace = WildFruitTrees.MOD_ID, timeoutTicks = 2000)
    public static void nativeOptionalFreshGrowthDiagnostics(GameTestHelper h) {
        for (String id : FORMS) h.assertTrue(Species.REGISTRY.get(new ResourceLocation(id)).isValid(), "Required calibration addon missing " + id);
        SUMS.clear();
        // The diagnostic applies native growth pulses explicitly. Do not allow incidental root random ticks
        // to invoke the intentionally blocked, not-yet-calibrated normal rate getter.
        h.getLevel().getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, h.getLevel().getServer());
        probe(h, 0, false);
    }
    /** Shared endpoint for the ordinary woody optional forms, not a cactus maturity definition. */
    static void verifyWoodReferences(GameTestHelper h) { SUMS.clear(); probe(h, 0, true); }
    private static void probe(GameTestHelper h, int index, boolean verifyWood) {
        var forms = verifyWood ? List.of(WildFruitPolicy.COCONUT, WildFruitPolicy.ANCIENT) : FORMS;
        if (index == forms.size() * 8) {
            if (!verifyWood) System.out.println("OPTIONAL_GROWTH_DIAGNOSTIC_ONLY: cactus adult-reference definition and all four production references remain release-blocking until reviewed");
            h.succeed(); return;
        }
        // A separate acceptance plot avoids the established thirteen-species fixture's (48,160,48).
        var level = h.getLevel(); var root = new BlockPos(verifyWood ? -160 : 48, 160, 48);
        String id = forms.get(index / 8); int sample = index % 8;
        for (var pos : BlockPos.betweenClosed(root.offset(-44, 0, -44), root.offset(44, 60, 44)))
            if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        boolean cactus = id.equals(WildFruitPolicy.SAGUARO) || id.equals(WildFruitPolicy.MEGA_CACTUS);
        var source = Species.REGISTRY.get(new ResourceLocation(id.equals(WildFruitPolicy.MEGA_CACTUS) ? WildFruitPolicy.SAGUARO : id));
        level.setBlock(root, (cactus ? Blocks.SAND : Blocks.DIRT).defaultBlockState(), 2);
        // Use public typed construction primitives matching the inspected pinned fresh initializers.
        // The player transition route intentionally remains forbidden; no private lookup/bypass is used.
        if (cactus) CactusNativeFixture.fresh(level, root.above(), source);
        else {
            var family = source.getFamily();
            family.getBranch().orElseThrow().setRadius(level, root.above(), family.getPrimaryThickness(), null);
            level.setBlockAndUpdate(root.above(2), source.getLeavesProperties().getDynamicLeavesState());
            source.placeRootyDirtBlock(level, root, 15);
            if (source.doesRequireTileEntity(level, root.above())) {
                var entity = com.ferreusveritas.dynamictrees.init.DTRegistries.SPECIES_BLOCK_ENTITY.create(root, level.getBlockState(root));
                h.assertTrue(entity != null, "Native species root entity unavailable " + id);
                level.setBlockEntity(entity); entity.setSpecies(source);
            }
        }
        h.assertTrue(level.getBlockState(root).getBlock() instanceof RootyBlock, "Native fresh root missing " + id);
        if (id.equals(WildFruitPolicy.MEGA_CACTUS)) h.assertTrue(new MegaSubstance().apply(level, root), "Native mega conversion failed");
        var tree = Species.REGISTRY.get(new ResourceLocation(id));
        ((RootyBlock)level.getBlockState(root).getBlock()).setFertility(level, root, 0);
        level.setDayTime(6000); level.updateSkyBrightness();
        h.runAfterDelay(5, () -> {
            h.assertTrue(level.getMaxLocalRawBrightness(root.above(3)) >= 13, "Calibration plot not sunlit " + id);
            ((RootyBlock)level.getBlockState(root).getBlock()).setFertility(level, root, 15);
            var branch = tree.getFamily().getBranch().orElseThrow();
            var random = RandomSource.create(12345 + 104729L * sample);
            level.random.setSeed(54321 + 104729L * sample);
            int firstMax = -1, lastChange = 0;
            var initial = geometry(level, root, branch);
            System.out.println("OPTIONAL_GROWTH_INITIAL " + id + " sample=" + sample + " " + initial);
            Geometry previous = initial;
            for (int pulse = 1; pulse <= 4096; pulse++) {
                branch.growSignal(level, root.above(), new GrowSignal(tree, root, tree.getEnergy(level, root), random, Direction.UP));
                if (!cactus && pulse % 16 == 0) TreeHelper.ageVolume(level, root.above(), 20, 48, 1, SafeChunkBounds.ANY);
                if (!cactus && branch.getRadius(level.getBlockState(root.above())) == tree.getMaxBranchRadius()) { firstMax = pulse; break; }
                if (cactus && pulse % 32 == 0) {
                    var current = geometry(level, root, branch);
                    if (!current.equals(previous)) { lastChange = pulse; previous = current; }
                    System.out.println("OPTIONAL_CACTUS_TRACE " + id + " sample=" + sample + " pulse=" + pulse + " " + current);
                }
            }
            if (cactus) {
                // Saguaro keeps native fixed thickness. A radius-eight stop condition is invalid for it.
                // These bounded traces provide observations for defining/reviewing a maturity reference;
                // neither the last sampled change nor 4096 attempts is silently promoted to that reference.
                System.out.println("UNRESOLVED_CACTUS_ADULT_REFERENCE " + id + " sample=" + sample + " lastSampledChange=" + lastChange);
            } else {
                h.assertTrue(firstMax > 0, "Native full trunk radius not reached within bounded probe " + id);
                SUMS.merge(id, firstMax, Integer::sum);
                System.out.println("OPTIONAL_GROWTH_SAMPLE " + id + " sample=" + sample + " pulses=" + firstMax);
                if (sample == 7) {
                    double mean = SUMS.get(id) / 8.0;
                    System.out.println("OPTIONAL_GROWTH_MEAN " + id + " meanPulses=" + mean);
                    if (verifyWood) {
                        double reference = TreeGrowthRates.reference(id).pulses();
                        h.assertTrue(Math.abs(mean - reference) / reference < .20, "Optional native full-trunk reference drifted " + id);
                    }
                }
            }
            // Remove the diagnostic tree before yielding. There is no authored production-world scan.
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 2);
            probe(h, index + 1, verifyWood);
        });
    }
    private record Geometry(int branches, int height, int trunkRadius) {}
    private static Geometry geometry(Level level, BlockPos root, BranchBlock branch) {
        var pending = new ArrayDeque<BlockPos>(); var seen = new HashSet<BlockPos>();
        pending.add(root.above()); int count = 0, height = 0;
        while (!pending.isEmpty() && seen.size() < 4096) {
            var pos = pending.removeFirst();
            if (!seen.add(pos) || root.distManhattan(pos) > 128 || !level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).is(branch)) continue;
            count++; height = Math.max(height, pos.getY() - root.getY());
            for (var direction : Direction.values()) pending.add(pos.relative(direction));
        }
        if (!pending.isEmpty()) throw new IllegalStateException("Bounded calibration geometry lookup exhausted");
        return new Geometry(count, height, branch.getRadius(level.getBlockState(root.above())));
    }
}
