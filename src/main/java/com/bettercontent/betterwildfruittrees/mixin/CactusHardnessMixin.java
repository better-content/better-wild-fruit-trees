package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.LoadedTreeLookup;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.block.BlockWithDynamicHardness;
import com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pinned CactusBranchBlock extends BranchBlock directly and inherits this base hardness getter. */
@Mixin(value = BlockWithDynamicHardness.class, remap = false)
public abstract class CactusHardnessMixin {
    @Inject(method = "getHardness", at = @At("RETURN"), cancellable = true)
    private void wildFruit$nonBasicBranch(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        // Basic branches retain their established hook; never multiply their hardness twice.
        if (state.getBlock() instanceof BranchBlock && !(state.getBlock() instanceof BasicBranchBlock)
                && LoadedTreeLookup.isProtected(level, pos)) cir.setReturnValue(WildFruitPolicy.hardness(cir.getReturnValue()));
    }
}
