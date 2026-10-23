package com.bettercontent.betterwildfruittrees.gametest.nativeprobe;

import com.bettercontent.betterwildfruittrees.gametest.OriginalLightTrace;
import com.bettercontent.betterwildfruittrees.gametest.OrderedEvents;
import com.ferreusveritas.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native-only forwarding: each original invocation occurs exactly once, including when inactive/throwing. */
@Mixin(DynamicLeavesBlock.class)
public abstract class LeavesLightTraceMixin {
    @Redirect(method = "hasAdequateLight", remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/LevelAccessor;canSeeSkyFromBelowWater(Lnet/minecraft/core/BlockPos;)Z"), require = 1, expect = 1, allow = 1)
    private boolean trace$canSee(LevelAccessor level, BlockPos pos) {
        if (!OriginalLightTrace.ownerActive()) return level.canSeeSkyFromBelowWater(pos);
        if (OrderedEvents.ownerActive()) OrderedEvents.queryBegin(false, pos.asLong());
        boolean result = level.canSeeSkyFromBelowWater(pos);
        if (OrderedEvents.ownerActive()) OrderedEvents.queryEnd(false, pos.asLong(), result ? 1 : 0);
        OriginalLightTrace.light(OriginalLightTrace.CAN_SEE, pos.asLong(), result ? 1 : 0);
        return result;
    }
    @Redirect(method = "hasAdequateLight", remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/LevelAccessor;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"), require = 1, expect = 1, allow = 1)
    private int trace$brightness(LevelAccessor level, LightLayer layer, BlockPos pos) {
        if (!OriginalLightTrace.ownerActive()) return level.getBrightness(layer, pos);
        if (OrderedEvents.ownerActive()) OrderedEvents.queryBegin(true, pos.asLong());
        int result = level.getBrightness(layer, pos);
        if (OrderedEvents.ownerActive()) OrderedEvents.queryEnd(true, pos.asLong(), result);
        OriginalLightTrace.light(OriginalLightTrace.BRIGHTNESS, pos.asLong(), result);
        return result;
    }
    @Inject(method = "hasAdequateLight", remap = false, at = @At("RETURN"), require = 3, expect = 3, allow = 3)
    private void trace$adequate(BlockState state, LevelAccessor level, LeavesProperties properties, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!OriginalLightTrace.ownerActive()) return;
        OriginalLightTrace.light(OriginalLightTrace.ADEQUATE, pos.asLong(), cir.getReturnValue() ? 1 : 0);
    }
}
