package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.TreeFruitBudget;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class RootBudgetLoadMixin {
    @Inject(method = {"load", "setLevel"}, at = @At("RETURN"))
    private void wildFruit$resumeProduction(CallbackInfo ci) {
        TreeFruitBudget.resume((BlockEntity)(Object)this);
        com.bettercontent.betterwildfruittrees.TreeMaturation.resume((BlockEntity)(Object)this);
    }
}
