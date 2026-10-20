package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.compat.RareResources;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.StaticSpeciesSelector;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.SpeciesSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StaticSpeciesSelector.class, remap = false)
public abstract class StaticSpeciesSelectorMixin {
    @Inject(method = "getSpecies", at = @At("RETURN"), cancellable = true)
    private void wildFruit$directGate(BlockPos pos, BlockState soil, RandomSource random, CallbackInfoReturnable<SpeciesSelection> cir) {
        cir.setReturnValue(RareResources.filter(cir.getReturnValue(), random, true));
    }
}
