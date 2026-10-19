package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native JoCode inflation stays unchanged; only subsequent branch growth uses enlarged taper. */
@Mixin(value = BasicBranchBlock.class, remap = false)
public abstract class GrowSignalMixin {
    @Redirect(method = "growSignal", at = @At(value = "INVOKE",
        target = "Lcom/ferreusveritas/dynamictrees/tree/species/Species;getTapering()F"))
    private float wildFruit$growthTapering(Species species) {
        return WildFruitPolicy.growthTapering(species.getRegistryName().toString(), species.getTapering());
    }
}
