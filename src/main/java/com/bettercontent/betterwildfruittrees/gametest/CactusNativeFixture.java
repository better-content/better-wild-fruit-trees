package com.bettercontent.betterwildfruittrees.gametest;

import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictreesplus.block.CactusBranchBlock;
import com.ferreusveritas.dynamictreesplus.tree.CactusSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Nonshipping typed DT Plus fixture, reached only after confirming addon presence. */
final class CactusNativeFixture {
    static boolean adultArchitecture(net.minecraft.server.level.ServerLevel level, BlockPos root, Species species, boolean mega) {
        var branch = species.getFamily().getBranch().orElseThrow();
        var nodes = new java.util.ArrayList<CactusArchitecture.Node>();
        var pending = new java.util.ArrayDeque<BlockPos>();
        var seen = new java.util.HashSet<BlockPos>();
        pending.add(root.above());
        while (!pending.isEmpty()) {
            var pos = pending.removeFirst();
            if (!seen.add(pos)) continue;
            if (seen.size() > 4096 || root.distManhattan(pos) > 128)
                throw new IllegalStateException("Bounded cactus topology fixture exhausted");
            if (!level.hasChunkAt(pos)) throw new IllegalStateException("Cactus topology reached unloaded fixture boundary");
            var state = level.getBlockState(pos);
            if (!state.is(branch)) continue;
            var origin = state.getValue(CactusBranchBlock.ORIGIN);
            nodes.add(new CactusArchitecture.Node(new CactusArchitecture.Point(pos.getX()-root.getX(),
                pos.getY()-root.getY(), pos.getZ()-root.getZ()), origin.getStepX(), origin.getStepY(), origin.getStepZ()));
            for (var direction : net.minecraft.core.Direction.values()) pending.add(pos.relative(direction));
        }
        int stop = mega ? species.getGrowthLogicKit().get(
            com.ferreusveritas.dynamictrees.api.configuration.ConfigurationProperty.integer("stop_branching_height")) : 0;
        boolean proven = mega && com.bettercontent.betterwildfruittrees.compat.RootLedger.inherited(level, root,
            species.getRegistryName().toString());
        return CactusArchitecture.reached(nodes, mega, proven, stop);
    }

    /** Public construction primitives matching the pinned cactus fresh initializer, not woody radius one. */
    static void fresh(Level level, BlockPos sapling, Species source) {
        var cactus = (CactusSpecies)source;
        source.placeRootyDirtBlock(level, sapling.below(), 15);
        var branch = source.getFamily().getBranch().orElseThrow();
        level.setBlockAndUpdate(sapling, branch.defaultBlockState().setValue(CactusBranchBlock.TRUNK_TYPE,
            cactus.thicknessForBranchPlaced(level, sapling, false)));
    }
}
