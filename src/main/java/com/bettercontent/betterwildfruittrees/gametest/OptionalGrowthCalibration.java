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
    private static final Map<String, Integer> SUMS = new LinkedHashMap<>();
    @GameTest(template = "growth", timeoutTicks = 2000)
    public static void nativeOptionalFreshGrowthDiagnostics(GameTestHelper h) {
        var plan = CalibrationPlan.select(System.getProperty(CalibrationPlan.PROPERTY));
        h.assertTrue(CalibrationPlan.NAMESPACE.equals(System.getProperty("forge.enabledGameTestNamespaces")),
            "Calibration requires its exact native namespace");
        NativeGrowthKitChecks.verify(h, true);
        for (var form : CalibrationPlan.Form.values()) h.assertTrue(Species.REGISTRY.get(new ResourceLocation(form.species())).isValid(),
            "Required calibration addon missing " + form.species());
        SUMS.clear();
        NativeCalibrationMarkers.emit("OPTIONAL_GROWTH_PROFILE profile=" + plan.selector() + " namespace=" + CalibrationPlan.NAMESPACE
            + " samples=" + plan.samples().size());
        // The diagnostic applies native growth pulses explicitly. Do not allow incidental root random ticks
        // to invoke the intentionally blocked, not-yet-calibrated normal rate getter.
        h.getLevel().getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, h.getLevel().getServer());
        probe(h, plan, 0, false);
    }
    /** Acceptance ALWAYS repeats all four original endpoints; diagnostic selection never weakens it. */
    static void verifyWoodReferences(GameTestHelper h) { SUMS.clear(); probe(h, CalibrationPlan.select("all"), 0, true); }
    private static void probe(GameTestHelper h, CalibrationPlan plan, int index, boolean verifyWood) {
        // A separate acceptance plot avoids the established thirteen-species fixture's (48,160,48).
        var level = h.getLevel(); var root = new BlockPos(verifyWood ? -160 : 48, 160, 48);
        CalibrationBirths.assertScope(h, root);
        // Retire the current root BEFORE native AIR removal can spawn asynchronous VOID handlers.
        // Leave this same plot unrooted until observed native births permanently retire themselves.
        level.setBlock(root, Blocks.AIR.defaultBlockState(), 2);
        for (var pos : BlockPos.betweenClosed(root.offset(-44, 0, -44), root.offset(44, 60, 44)))
            if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        h.runAfterDelay(1, () -> awaitRetirement(h, root, () -> {
            if (index == plan.samples().size()) {
                if (!verifyWood) NativeCalibrationMarkers.emit("OPTIONAL_GROWTH_DIAGNOSTIC_ONLY endpoint=" + CactusArchitecture.VERSION + "; measurements require source review before references are filled");
                h.succeed(); return;
            }
            startSample(h, plan, index, verifyWood, root);
        }));
    }
    private static void awaitRetirement(GameTestHelper h, BlockPos root, Runnable ready) {
        h.assertTrue(h.getTick() < 2000, "Fixture retirement exceeded original GameTest deadline");
        h.assertTrue(h.getLevel().isEmptyBlock(root), "Fixture retirement root unexpectedly reappeared");
        if (!CalibrationBirths.quiescent(h, root)) {
            h.runAfterDelay(1, () -> awaitRetirement(h, root, ready));
            return;
        }
        CalibrationBirths.releaseRetired();
        ready.run();
    }
    private static void startSample(GameTestHelper h, CalibrationPlan plan, int index, boolean verifyWood, BlockPos root) {
        var level = h.getLevel();
        var selected = plan.samples().get(index);
        String id = selected.form().species(); int sample = selected.sample();
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
            if (id.equals(WildFruitPolicy.COCONUT)) {
                float suitability = tree.biomeSuitability(level, root);
                float energy = tree.getEnergy(level, root);
                h.assertTrue(Float.isFinite(suitability) && suitability > 0 && suitability <= 1,
                    "Coconut fixed-reference suitability violates the inspected native bound (including event override): " + suitability);
                h.assertTrue(energy > 0 && energy <= 27 && tree.getFamily().getSecondaryThickness() == 2,
                    "Coconut fixed-reference energy/thickness does not match native geometry proof");
                NativeCalibrationMarkers.emit("OPTIONAL_PALM_NATIVE_BOUND suitability=" + suitability + " energy=" + energy
                    + " secondaryThickness=" + tree.getFamily().getSecondaryThickness() + " cap=" + tree.getMaxBranchRadius());
            }
            var random = RandomSource.create(selected.growthSeed());
            level.random.setSeed(selected.levelSeed());
            int firstMax = -1;
            var initial = geometry(level, root, branch);
            NativeCalibrationMarkers.emit("OPTIONAL_GROWTH_INITIAL " + id + " sample=" + sample + " " + initial);
            for (int pulse = 1; pulse <= 4096; pulse++) {
                branch.growSignal(level, root.above(), new GrowSignal(tree, root, tree.getEnergy(level, root), random, Direction.UP));
                if (!cactus && pulse % 16 == 0) TreeHelper.ageVolume(level, root.above(), 20, 48, 1, SafeChunkBounds.ANY);
                if (id.equals(WildFruitPolicy.COCONUT) && CoconutNativeFixture.adultArchitecture(level, root, tree)) { firstMax = pulse; break; }
                if (!cactus && !id.equals(WildFruitPolicy.COCONUT) && branch.getRadius(level.getBlockState(root.above())) == tree.getMaxBranchRadius()) { firstMax = pulse; break; }
                if (cactus && CactusNativeFixture.adultArchitecture(level, root, tree, id.equals(WildFruitPolicy.MEGA_CACTUS))) {
                    firstMax = pulse;
                    break;
                }
            }
            {
                h.assertTrue(firstMax > 0, "Predeclared native structural endpoint not reached within bounded probe " + id + " sample=" + sample
                    + " final=" + geometry(level, root, branch));
                SUMS.merge(id, firstMax, Integer::sum);
                NativeCalibrationMarkers.emit("OPTIONAL_GROWTH_SAMPLE " + id + " sample=" + sample + " pulses=" + firstMax
                    + " formOrdinal=" + selected.form().ordinal() + " canonicalIndex=" + selected.canonicalIndex()
                    + " endpoint=" + (cactus ? CactusArchitecture.VERSION : id.equals(WildFruitPolicy.COCONUT) ? CoconutArchitecture.VERSION : "native_maximum_trunk_radius") + " geometry=" + geometry(level, root, branch));
                if (sample == 7) {
                    double mean = SUMS.get(id) / 8.0;
                    NativeCalibrationMarkers.emit("OPTIONAL_GROWTH_MEAN " + id + " meanPulses=" + mean);
                    if (verifyWood) {
                        var measured = id.equals(WildFruitPolicy.MEGA_CACTUS) ? TreeGrowthRates.REFERENCES.get("saguaro_mega") : TreeGrowthRates.reference(id);
                        h.assertTrue(measured != null, "Missing measured reference " + id);
                        double reference = measured.pulses();
                        h.assertTrue(Math.abs(mean - reference) / reference < .20, "Optional native structural reference drifted " + id);
                    }
                }
            }
            // Remove the diagnostic tree before yielding. There is no authored production-world scan.
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 2);
            probe(h, plan, index + 1, verifyWood);
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
