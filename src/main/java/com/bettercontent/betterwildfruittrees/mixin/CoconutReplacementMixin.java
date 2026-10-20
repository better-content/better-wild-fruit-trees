package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.api.TreeRegistry;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** DT otherwise destroys a placed whole coconut before discovering that planting is forbidden. */
@Mixin(value = TreeRegistry.class, remap = false)
public abstract class CoconutReplacementMixin {
    @Inject(method = {"shouldReplaceSaplingWhenPlaced", "shouldReplaceSaplingWhenGrown"}, at = @At("HEAD"), cancellable = true)
    private static void wildFruit$foodIsNotASapling(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (new ResourceLocation("natures_spirit:coconut").equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()))
                && Species.REGISTRY.get(new ResourceLocation(WildFruitPolicy.COCONUT)).isValid()) cir.setReturnValue(false);
    }
}
