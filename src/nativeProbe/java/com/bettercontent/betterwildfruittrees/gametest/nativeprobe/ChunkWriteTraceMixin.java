package com.bettercontent.betterwildfruittrees.gametest.nativeprobe;

import com.bettercontent.betterwildfruittrees.gametest.OriginalLightTrace;
import com.bettercontent.betterwildfruittrees.gametest.OrderedEvents;
import com.bettercontent.betterwildfruittrees.gametest.NativeStateCodes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class ChunkWriteTraceMixin {
    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private void trace$writeHead(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> cir) {
        if (!OrderedEvents.ownerActive()) return;
        OrderedEvents.writeHead(pos.asLong(), NativeStateCodes.code(state));
    }
    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), require = 4, expect = 4, allow = 4)
    private void trace$write(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> cir) {
        if (!OriginalLightTrace.ownerActive()) return;
        BlockState old = cir.getReturnValue();
        long packed = pos.asLong();
        OriginalLightTrace.write(packed, System.identityHashCode(state), System.identityHashCode(old), old != null);
        OriginalLightTrace.canonicalWrite(packed, NativeStateCodes.code(state), NativeStateCodes.code(old));
    }
}
