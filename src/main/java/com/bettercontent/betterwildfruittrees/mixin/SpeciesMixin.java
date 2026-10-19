package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.bettercontent.betterwildfruittrees.TreeMaturation;
import com.bettercontent.betterwildfruittrees.TreeGrowthSchedule;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.systems.SeedSaplingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.Set;

@Mixin(value = Species.class, remap = false)
public abstract class SpeciesMixin {
    @Shadow protected float signalEnergy;
    @Unique private String wildFruit$id() { return ((Species)(Object)this).getRegistryName().toString(); }
    @Unique private boolean wildFruit$protected() { return WildFruitPolicy.protectsSpecies(wildFruit$id()); }

    @Inject(method = "getSignalEnergy", at = @At("RETURN"), cancellable = true)
    private void wildFruit$energy(CallbackInfoReturnable<Float> cir) {
        if (wildFruit$protected()) cir.setReturnValue(WildFruitPolicy.energy(wildFruit$id(), cir.getReturnValue()));
    }
    @Inject(method = "placeRootyDirtBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)Z", at = @At("RETURN"))
    private void wildFruit$birth(LevelAccessor level, BlockPos root, int fertility, CallbackInfoReturnable<Boolean> cir) {
        if (wildFruit$protected() && cir.getReturnValue()) TreeMaturation.start(level, root);
    }
    @Inject(method = "grow", at = @At("HEAD"))
    private void wildFruit$age(Level level, com.ferreusveritas.dynamictrees.block.rooty.RootyBlock soil,
            BlockPos root, int fertility, com.ferreusveritas.dynamictrees.api.treedata.TreePart part,
            BlockPos trunk, net.minecraft.util.RandomSource random, boolean natural, CallbackInfoReturnable<Boolean> cir) {
        if (wildFruit$protected()) TreeMaturation.observe(level, root);
    }
    @Redirect(method = "sendGrowthSignal", at = @At(value = "INVOKE",
        target = "Lcom/ferreusveritas/dynamictrees/tree/species/Species;getEnergy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"))
    private float wildFruit$maturingEnergy(Species species, Level level, BlockPos root) {
        float energy = species.getEnergy(level, root);
        return wildFruit$protected() ? TreeGrowthSchedule.energy(energy, signalEnergy, species.getSignalEnergy(),
                TreeMaturation.observe(level, root)) : energy;
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
