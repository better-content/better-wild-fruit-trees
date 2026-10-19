package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.bettercontent.betterwildfruittrees.TreeMaturation;
import com.bettercontent.betterwildfruittrees.TreeGrowthSchedule;
import com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Growth-only tapering: JoCode's InflatorNode must keep the original species getter. */
@Mixin(value = BasicBranchBlock.class, remap = false)
public abstract class GrowSignalMixin {
    @Redirect(method = "growSignal", at = @At(value = "INVOKE",
        target = "Lcom/ferreusveritas/dynamictrees/tree/species/Species;getTapering()F"))
    private float wildFruit$growthTapering(Species species, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, com.ferreusveritas.dynamictrees.systems.GrowSignal signal) {
        float original = species.getTapering();
        String id = species.getRegistryName().toString();
        return WildFruitPolicy.protectsSpecies(id) ? TreeGrowthSchedule.interpolate(original,
                WildFruitPolicy.growthTapering(id, original), TreeMaturation.peek(level, signal.rootPos)) : original;
    }
}
