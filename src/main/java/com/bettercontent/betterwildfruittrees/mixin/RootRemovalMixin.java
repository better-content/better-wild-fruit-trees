package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.compat.RootLedger;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** RootyBlock inherits Block.onRemove in pinned DT; intercept the actual declaring method. */
@Mixin(Block.class)
public abstract class RootRemovalMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void wildFruit$forget(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving, CallbackInfo ci) {
        if (state.getBlock() instanceof RootyBlock && !state.is(replacement.getBlock()) && level instanceof ServerLevel server)
            RootLedger.get(server).remove(server, pos);
    }
}
