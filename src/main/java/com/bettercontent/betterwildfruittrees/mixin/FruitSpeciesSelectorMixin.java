package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.RandomSpeciesSelector;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.SpeciesSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RandomSpeciesSelector.class, remap = false)
public abstract class FruitSpeciesSelectorMixin {
    @Inject(method = "getSpecies", at = @At("RETURN"), cancellable = true)
    private void wildFruit$rareSpecies(BlockPos pos, BlockState soil, RandomSource random,
                                      CallbackInfoReturnable<SpeciesSelection> cir) {
        cir.setReturnValue(com.bettercontent.betterwildfruittrees.compat.RareResources.filter(cir.getReturnValue(), random, false));
    }
}
