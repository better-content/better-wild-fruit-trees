package com.bettercontent.betterwildfruittrees.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Native whole coconuts remain placeable food/processing blocks, but cannot germinate into portable trees. */
@Pseudo
@Mixin(targets = "net.hibiscus.naturespirit.blocks.CoconutBlock")
public abstract class CoconutFoodBlockMixin {
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noGermination(LevelReader level, BlockPos pos, BlockState state, boolean client, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noDirectGermination(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        ci.cancel();
    }
}
