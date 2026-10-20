package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.TreeFruitBudget;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.ferreusveritas.dynamictrees.systems.pod.Pod;
import com.ferreusveritas.dynamictrees.util.LevelContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "xueluoanping.dtnatures_spirit.systems.featuregen.PalmFruitGenFeature", remap = false)
public abstract class PalmFruitGenFeatureMixin {
    @Shadow protected abstract void addFruit(GenFeatureConfiguration config, LevelContext level, BlockPos root, BlockPos end, boolean worldgen);
    /** Root credit replaces the native extra 1/16 throttle, not seasonal/size/geometry eligibility.
     * Keep the native random draw itself, and all unselected palms' behavior, unchanged. */
    @ModifyConstant(method = "postGrow", constant = @Constant(intValue = 16))
    private int wildFruit$rootCadence(int divisor, GenFeatureConfiguration config, PostGrowContext context) {
        return WildFruitPolicy.COCONUT.equals(context.species().getRegistryName().toString()) ? 1 : divisor;
    }
    @Redirect(method = "postGrow", at = @At(value = "INVOKE", target = "Lxueluoanping/dtnatures_spirit/systems/featuregen/PalmFruitGenFeature;addFruit(Lcom/ferreusveritas/dynamictrees/systems/genfeature/GenFeatureConfiguration;Lcom/ferreusveritas/dynamictrees/util/LevelContext;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Z)V"))
    private void wildFruit$budget(@Coerce Object feature, GenFeatureConfiguration config, LevelContext level, BlockPos root,
            BlockPos end, boolean worldgen, GenFeatureConfiguration original, PostGrowContext context) {
        if (!worldgen && level.accessor() instanceof ServerLevel server && WildFruitPolicy.COCONUT.equals(context.species().getRegistryName().toString())) {
            TreeFruitBudget.attempt(server, root, () -> {
                for (int i = 0; i < WildFruitPolicy.FRUIT_PLACEMENT_ATTEMPTS; i++) addFruit(config, level, root, end, false);
            });
        } else addFruit(config, level, root, end, worldgen);
    }
    @Redirect(method = "addFruit", at = @At(value = "INVOKE", target = "Lcom/ferreusveritas/dynamictrees/systems/pod/Pod;place(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Ljava/lang/Float;Lnet/minecraft/core/Direction;I)V"))
    private void wildFruit$success(Pod pod, LevelAccessor level, BlockPos pos, Float season, Direction direction, int radius) {
        TreeFruitBudget.placeBlock(pod.getBlock(), level, pos, () -> pod.place(level, pos, season, direction, radius));
    }
}
