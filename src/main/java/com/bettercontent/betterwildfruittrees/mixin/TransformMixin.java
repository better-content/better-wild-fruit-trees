package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.systems.substance.TransformSubstance;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TransformSubstance.class, remap = false)
public abstract class TransformMixin {
    @Shadow @Final private Species toSpecies;
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noTransformInto(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (WildFruitPolicy.protectsSpecies(toSpecies.getRegistryName().toString())
                || com.bettercontent.betterwildfruittrees.LoadedTreeLookup.isProtected(level, pos)) cir.setReturnValue(false);
    }
}
