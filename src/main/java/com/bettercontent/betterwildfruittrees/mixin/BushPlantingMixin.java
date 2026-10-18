package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildBushes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BushPlantingMixin {
    // place() is the common entry, including direct dispenser/fake-player BlockItem use.
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noBushPlanting(BlockPlaceContext context, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (WildBushes.isBush(((BlockItem)(Object)this).getBlock().defaultBlockState()))
            cir.setReturnValue(net.minecraft.world.InteractionResult.FAIL);
    }
}
