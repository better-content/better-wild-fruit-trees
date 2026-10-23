package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import xueluoanping.dtnatures_spirit.systems.pods.IFallingFruit;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import xueluoanping.dtnatures_spirit.systems.cell.CoconutCellKit;
import xueluoanping.dtnatures_spirit.systems.featuregen.PalmFruitGenFeature;
import java.util.*;

/** Nonshipping optional adapter: original falling-product helper plus loaded-only structural observer.
 * The structural observer never calls fruit/growth/RNG APIs or loads chunks. */
final class CoconutNativeFixture {
    // Preserve the pre-existing public-native falling-product fixture and its unchanged consumer.
    static ItemStack fallingDrop(Block block, FallingBlockEntity entity) {
        return ((IFallingFruit)block).getDropOnFallItems(entity);
    }
    static boolean adultArchitecture(ServerLevel level, BlockPos root, Species species) {
        if (!species.getRegistryName().toString().equals(WildFruitPolicy.COCONUT) ||
                !species.getFamily().getRegistryName().toString().equals(WildFruitPolicy.COCONUT))
            throw new IllegalStateException("Coconut native species/family authority drift");
        var features = species.getGenFeatures().stream().filter(f ->
            f.getGenFeature().getClass() == PalmFruitGenFeature.class &&
            f.getGenFeature().getRegistryName().toString().equals("dtnatures_spirit:palm_fruit")).toList();
        if (features.size() != 1 || !species.getLeavesProperties().getRegistryName().toString().equals(WildFruitPolicy.COCONUT) ||
                species.getLeavesProperties().getCellKit().getClass() != CoconutCellKit.class ||
                !species.getLeavesProperties().getCellKit().getRegistryName().toString().equals(WildFruitPolicy.COCONUT) ||
                species.getFamily().getPrimaryThickness() != 1 || Math.abs(species.getTapering() - .55F) > .000001F)
            throw new IllegalStateException("Coconut native feature/cell/primary/taper authority drift");
        int fruitingRadius = features.get(0).get(GenFeature.FRUITING_RADIUS);
        int cap = species.getFamily().getMaxBranchRadius();
        if (species.getMaxBranchRadius() != cap) throw new IllegalStateException("Coconut species/family cap authority drift");
        int secondary = species.getFamily().getSecondaryThickness();
        // Exactly the public policy called by GrowSignalMixin's growth-only native taper redirect.
        float taper = WildFruitPolicy.growthTapering(WildFruitPolicy.COCONUT, species.getTapering());
        CoconutArchitecture.matches(Map.of(), Map.of(), fruitingRadius, cap, secondary, taper); // Live authority validation even before eligibility.
        var branch = species.getFamily().getBranch().orElseThrow();
        var leaves = species.getLeavesProperties().getDynamicLeavesBlock().orElseThrow();
        var base = root.above();
        if (!level.hasChunkAt(base)) throw new IllegalStateException("Unloaded Coconut fixture root");
        var baseState = level.getBlockState(base);
        if (!baseState.is(branch) || branch.getRadius(baseState) < fruitingRadius) return false;
        var nodes = new HashMap<CoconutArchitecture.Pos, CoconutArchitecture.Branch>();
        var crowns = new HashMap<CoconutArchitecture.Pos, CoconutArchitecture.Leaf>();
        var pending = new ArrayDeque<BlockPos>(); var seen = new HashSet<BlockPos>();
        pending.add(base);
        while (!pending.isEmpty()) {
            var pos = pending.removeFirst(); if (!seen.add(pos)) continue;
            if (seen.size() > 4096 || root.distManhattan(pos) > 128)
                throw new IllegalStateException("Bounded Coconut fixture observation exhausted");
            if (!level.hasChunkAt(pos)) throw new IllegalStateException("Coconut observation reached unloaded boundary");
            var state = level.getBlockState(pos);
            if (!state.is(branch)) {
                if (state.getBlock() instanceof BranchBlock) return false;
                continue;
            }
            nodes.put(relative(root, pos), new CoconutArchitecture.Branch(branch.getRadius(state), true));
            var crown = pos.above();
            if (root.distManhattan(crown) > 128 || !level.hasChunkAt(crown))
                throw new IllegalStateException("Coconut crown observation outside loaded bound");
            var crownState = level.getBlockState(crown);
            if (crownState.is(leaves))
                crowns.put(relative(root, crown), new CoconutArchitecture.Leaf(crownState.getValue(LeavesBlock.DISTANCE), true));
            for (var direction : Direction.values()) pending.add(pos.relative(direction));
        }
        return CoconutArchitecture.matches(nodes, crowns, fruitingRadius, cap, secondary, taper);
    }
    private static CoconutArchitecture.Pos relative(BlockPos root, BlockPos pos) {
        return new CoconutArchitecture.Pos(pos.getX()-root.getX(), pos.getY()-root.getY(), pos.getZ()-root.getZ());
    }
}
