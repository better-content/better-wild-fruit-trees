package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.bettercontent.betterwildfruittrees.TreeFruitBudget;
import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Shadow;
import com.ferreusveritas.dynamictrees.systems.genfeature.FruitGenFeature;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.ferreusveritas.dynamictrees.api.configuration.ConfigurationProperty;

@Mixin(value = FruitGenFeature.class, remap = false)
public abstract class FruitGenFeatureMixin {
    @Shadow protected abstract void place(GenFeatureConfiguration configuration, Species species,
        LevelAccessor level, BlockPos rootAbove, BlockPos end, Float season);

    @Redirect(method = "postGrow", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/systems/genfeature/FruitGenFeature;place(Lcom/ferreusveritas/dynamictrees/systems/genfeature/GenFeatureConfiguration;Lcom/ferreusveritas/dynamictrees/tree/species/Species;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Ljava/lang/Float;)V"))
    private void wildFruit$replacementBudget(FruitGenFeature feature, GenFeatureConfiguration configuration,
            Species species, LevelAccessor level, BlockPos rootAbove, BlockPos end, Float season,
            GenFeatureConfiguration original, PostGrowContext context) {
        Runnable nativePlacement = () -> place(configuration, species, level, rootAbove, end, season);
        if (level instanceof ServerLevel server && WildFruitPolicy.protectsSpecies(species.getRegistryName().toString()))
            TreeFruitBudget.attempt(server, context.pos(), nativePlacement);
        else nativePlacement.run();
    }

    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/systems/fruit/Fruit;place(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Ljava/lang/Float;)V"))
    private void wildFruit$chargeSuccessfulPlacement(Fruit fruit, LevelAccessor level, BlockPos pos, Float season) {
        TreeFruitBudget.place(fruit, level, pos, season);
    }

    /** Keep bounded retries for native geometry; only successful replacements spend rate credit. */
    @Redirect(method = "postGrow", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/systems/genfeature/GenFeatureConfiguration;get(Lcom/ferreusveritas/dynamictrees/api/configuration/ConfigurationProperty;)Ljava/lang/Object;"))
    private Object wildFruit$quantity(GenFeatureConfiguration configuration, ConfigurationProperty<?> property,
                                      GenFeatureConfiguration original, PostGrowContext context) {
        Object value = configuration.get(property);
        return property == GenFeature.QUANTITY && WildFruitPolicy.protectsSpecies(context.species().getRegistryName().toString())
            ? Math.max((Integer)value, WildFruitPolicy.FRUIT_PLACEMENT_ATTEMPTS) : value;
    }
}
