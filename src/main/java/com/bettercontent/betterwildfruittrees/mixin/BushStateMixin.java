package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildBushes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BushStateMixin {
    @Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void wildFruit$fixedShape(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = (BlockState)(Object)this;
        if (WildBushes.isBush(state))
            cir.setReturnValue(state.getBlock().getShape(WildBushes.mature(state), level, pos, context));
    }
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void wildFruit$bushHardness(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (WildBushes.protectedResource((BlockState)(Object)this, level, pos)) cir.setReturnValue(Math.max(80.0F, cir.getReturnValue()));
    }
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void wildFruit$toolGate(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (WildBushes.restrictPlayer(player) && WildBushes.protectedResource((BlockState)(Object)this, level, pos) && !WildBushes.appropriateAxe(player.getMainHandItem()))
            cir.setReturnValue(0.0F);
    }
}
