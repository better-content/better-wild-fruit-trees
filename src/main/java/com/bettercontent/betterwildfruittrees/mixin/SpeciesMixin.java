package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.systems.SeedSaplingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.Set;

@Mixin(value = Species.class, remap = false)
public abstract class SpeciesMixin {
    @Unique private String wildFruit$id() { return ((Species)(Object)this).getRegistryName().toString(); }
    @Unique private boolean wildFruit$protected() { return WildFruitPolicy.protectsSpecies(wildFruit$id()); }

    @Inject(method = "getSignalEnergy", at = @At("RETURN"), cancellable = true)
    private void wildFruit$energy(CallbackInfoReturnable<Float> cir) {
        if (wildFruit$protected()) cir.setReturnValue(WildFruitPolicy.energy(wildFruit$id(), cir.getReturnValue()));
    }
    @Inject(method = "getSoilLongevity", at = @At("RETURN"), cancellable = true)
    private void wildFruit$longevity(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (wildFruit$protected()) cir.setReturnValue(cir.getReturnValue() * WildFruitPolicy.SOIL_LONGEVITY_MULTIPLIER);
    }
    @Inject(method = {"shouldDropSeeds", "isTransformable"}, at = @At("HEAD"), cancellable = true)
    private void wildFruit$noPropagation(CallbackInfoReturnable<Boolean> cir) {
        if (wildFruit$protected()) cir.setReturnValue(false);
    }
    @Inject(method = "plantSapling", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noPlanting(LevelAccessor level, BlockPos pos, boolean override, CallbackInfoReturnable<Boolean> cir) {
        if (wildFruit$protected()) cir.setReturnValue(false);
    }
    @Inject(method = "transitionToTree(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noExistingSaplingGrowth(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (wildFruit$protected()) cir.setReturnValue(false);
    }
    @Inject(method = "getVoluntaryDrops", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noVoluntarySeeds(Level level, BlockPos pos, int fertility, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (wildFruit$protected()) cir.setReturnValue(List.of());
    }
    @Inject(method = "getPrimitiveSaplingRecipes", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noConversions(CallbackInfoReturnable<Set<SeedSaplingRecipe>> cir) {
        if (wildFruit$protected()) cir.setReturnValue(Set.of());
    }
}
