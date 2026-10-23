package com.bettercontent.betterwildfruittrees.mixin;

import net.hibiscus.naturespirit.blocks.CoconutBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Native whole coconuts remain placeable food/processing blocks, but cannot germinate into portable trees. */
// A real compile-time target is required: @Pseudo suppresses AP descriptor discovery and
// silently leaves these inherited Minecraft interface methods unmapped in production.
// AddonMixins gates application before optional target loading; compileOnly supplies the AP ABI.
@Mixin(CoconutBlock.class)
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
