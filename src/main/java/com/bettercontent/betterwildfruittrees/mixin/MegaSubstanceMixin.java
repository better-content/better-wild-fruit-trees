package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.bettercontent.betterwildfruittrees.compat.RootLedger;
import com.ferreusveritas.dynamictrees.systems.substance.MegaSubstance;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The pinned native conversion replaces the species at the same root position. */
@Mixin(value = MegaSubstance.class, remap = false)
public abstract class MegaSubstanceMixin {
    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/tree/species/Species;placeRootyDirtBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)Z"))
    private boolean wildFruit$inherit(Species target, LevelAccessor level, BlockPos root, int fertility) {
        boolean saguaro = level instanceof ServerLevel server && WildFruitPolicy.SAGUARO.equals(RootLedger.speciesAt(server, root));
        if (saguaro && level instanceof ServerLevel server) RootLedger.get(server).requireSupported();
        boolean result = target.placeRootyDirtBlock(level, root, fertility);
        if (result && saguaro && level instanceof ServerLevel server) RootLedger.get(server).converted(server, root);
        return result;
    }
}
