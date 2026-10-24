package com.bettercontent.betterwildfruittrees.gametest.growthv2.mixin;

import com.bettercontent.betterwildfruittrees.gametest.GrowthFixtureV2;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** V2 measurement isolation only. All nonowned roots retain their original native invocation. */
@Mixin(RootyBlock.class)
public abstract class RootRandomTickIsolationMixin {
    @Inject(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At("HEAD"), cancellable = true)
    private void growthV2$ownedRoot(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (GrowthFixtureV2.blockRootRandomTick(level, pos, this)) ci.cancel();
    }
    @Inject(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At("RETURN"))
    private void growthV2$returned(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        GrowthFixtureV2.rootRandomTickReturned(level, pos, this);
    }
}
