package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.TreeFruitBudget;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.api.configuration.ConfigurationProperty;
import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** DT Plus overrides both native methods, so ordinary FruitGenFeature redirects cannot cover it. */
@Pseudo
@Mixin(targets = "com.ferreusveritas.dynamictreesplus.systems.featuregen.CactusFruitGenFeature", remap = false)
public abstract class CactusFruitGenFeatureMixin {
    @Shadow protected abstract void place(GenFeatureConfiguration configuration, Species species,
        LevelAccessor level, BlockPos rootAbove, BlockPos end, Float season);
    @Redirect(method = "postGrow", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictreesplus/systems/featuregen/CactusFruitGenFeature;place(Lcom/ferreusveritas/dynamictrees/systems/genfeature/GenFeatureConfiguration;Lcom/ferreusveritas/dynamictrees/tree/species/Species;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Ljava/lang/Float;)V"))
    private void wildFruit$budget(@Coerce Object feature, GenFeatureConfiguration config, Species species,
            LevelAccessor level, BlockPos rootAbove, BlockPos end, Float season, GenFeatureConfiguration original, PostGrowContext context) {
        Runnable nativePlacement = () -> place(config, species, level, rootAbove, end, season);
        if (level instanceof ServerLevel server && WildFruitPolicy.SAGUARO.equals(species.getRegistryName().toString()))
            TreeFruitBudget.attempt(server, context.pos(), nativePlacement);
        else nativePlacement.run();
    }
    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/systems/fruit/Fruit;place(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Ljava/lang/Float;)V"))
    private void wildFruit$success(Fruit fruit, LevelAccessor level, BlockPos pos, Float season) {
        TreeFruitBudget.place(fruit, level, pos, season);
    }
    @Redirect(method = "postGrow", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/systems/genfeature/GenFeatureConfiguration;get(Lcom/ferreusveritas/dynamictrees/api/configuration/ConfigurationProperty;)Ljava/lang/Object;"))
    private Object wildFruit$attempts(GenFeatureConfiguration config, ConfigurationProperty<?> property,
            GenFeatureConfiguration original, PostGrowContext context) {
        Object value = config.get(property);
        return property == GenFeature.QUANTITY && WildFruitPolicy.SAGUARO.equals(context.species().getRegistryName().toString())
            ? Math.max((Integer)value, WildFruitPolicy.FRUIT_PLACEMENT_ATTEMPTS) : value;
    }
}
