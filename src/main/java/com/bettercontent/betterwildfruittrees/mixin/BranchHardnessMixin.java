package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.LoadedTreeLookup;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasicBranchBlock.class, remap = false)
public abstract class BranchHardnessMixin {
    @Inject(method = "getHardness", at = @At("RETURN"), cancellable = true)
    private void wildFruit$hardWood(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (LoadedTreeLookup.isProtected(level, pos)) cir.setReturnValue(WildFruitPolicy.hardness(cir.getReturnValue()));
    }
}
