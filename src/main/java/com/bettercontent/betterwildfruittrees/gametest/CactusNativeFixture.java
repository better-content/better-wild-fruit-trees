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

    /** One connected upturned native BRANCH arm with a hash-eligible fruit site.
     * A bare TRUNK is deliberately ineligible in CactusFruitGenFeature.shouldPlace;
     * its deterministic coordinate veto cannot be overcome by retrying RNG. */
    static BlockPos fruitingArm(Level level, BlockPos root, Species species) {
        var branch = species.getFamily().getBranch().orElseThrow();
        for (var direction : com.ferreusveritas.dynamictrees.util.CoordUtils.HORIZONTALS) {
            for (int distance = 1; distance <= 3; distance++) for (int height = 2; height <= 4; height++) {
                var tip = root.relative(direction, distance).above(height);
                if ((com.ferreusveritas.dynamictrees.util.CoordUtils.coordHashCode(tip.above(), 0) & 6) != 0) continue;
                var state = branch.defaultBlockState().setValue(CactusBranchBlock.TRUNK_TYPE,
                    CactusBranchBlock.CactusThickness.BRANCH);
                for (int step = 1; step <= distance; step++)
                    level.setBlock(root.relative(direction, step).above(), state.setValue(CactusBranchBlock.ORIGIN, direction.getOpposite()), 2);
                for (int y = 2; y <= height; y++)
                    level.setBlock(root.relative(direction, distance).above(y), state.setValue(CactusBranchBlock.ORIGIN, net.minecraft.core.Direction.DOWN), 2);
                return tip.above();
            }
        }
        throw new IllegalStateException("No hash-eligible native cactus fruit site in bounded fixture");
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
