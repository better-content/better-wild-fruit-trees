package com.bettercontent.betterwildfruittrees.gametest;

import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictreesplus.block.CactusBranchBlock;
import com.ferreusveritas.dynamictreesplus.tree.CactusSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Nonshipping typed DT Plus fixture, reached only after confirming addon presence. */
final class CactusNativeFixture {
    /** Public construction primitives matching the pinned cactus fresh initializer, not woody radius one. */
    static void fresh(Level level, BlockPos sapling, Species source) {
        var cactus = (CactusSpecies)source;
        source.placeRootyDirtBlock(level, sapling.below(), 15);
        var branch = source.getFamily().getBranch().orElseThrow();
        level.setBlockAndUpdate(sapling, branch.defaultBlockState().setValue(CactusBranchBlock.TRUNK_TYPE,
            cactus.thicknessForBranchPlaced(level, sapling, false)));
    }
}
