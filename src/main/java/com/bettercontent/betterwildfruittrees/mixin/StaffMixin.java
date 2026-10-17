package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.bettercontent.betterwildfruittrees.LoadedTreeLookup;
import com.ferreusveritas.dynamictrees.item.Staff;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Staff.class)
public abstract class StaffMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noCopyOrPaste(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Species species = ((Staff)(Object)this).getSpecies(context.getItemInHand());
        var pos = context.getClickedPos();
        var state = context.getLevel().getBlockState(pos);
        // Match the native staff's foliage/replacement targeting before checking its source.
        if (state.canBeReplaced() || state.is(com.ferreusveritas.dynamictrees.data.DTBlockTags.FOLIAGE)) pos = pos.below();
        if (WildFruitPolicy.protectsSpecies(species.getRegistryName().toString())
            || LoadedTreeLookup.isProtected(context.getLevel(), pos))
            cir.setReturnValue(InteractionResult.FAIL);
    }
}
