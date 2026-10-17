package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
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
    /** Only replenish more fruit during natural updates. Initial worldgen is untouched. */
    @Redirect(method = "postGrow", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/systems/genfeature/GenFeatureConfiguration;get(Lcom/ferreusveritas/dynamictrees/api/configuration/ConfigurationProperty;)Ljava/lang/Object;"))
    private Object wildFruit$quantity(GenFeatureConfiguration configuration, ConfigurationProperty<?> property,
                                      GenFeatureConfiguration original, PostGrowContext context) {
        Object value = configuration.get(property);
        return property == GenFeature.QUANTITY && WildFruitPolicy.protectsSpecies(context.species().getRegistryName().toString())
            ? Math.max((Integer)value, WildFruitPolicy.FRUIT_PLACEMENT_ATTEMPTS) : value;
    }
}
