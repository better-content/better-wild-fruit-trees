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
        var selected = cir.getReturnValue();
        if (selected.isHandled() && WildFruitPolicy.protectsSpecies(selected.getSpecies().getRegistryName().toString())
                && random.nextInt(WildFruitPolicy.TREE_RARITY) != 0) {
            // Native SPLICE_BEFORE continues to the biome's ordinary selector, not a handled null tree.
            cir.setReturnValue(new SpeciesSelection());
        }
    }
}
