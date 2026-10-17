package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.item.Seed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Seed.class, remap = false)
public abstract class SeedMixin {
    private boolean wildFruit$protected() {
        return WildFruitPolicy.protectsSpecies(((Seed)(Object)this).getSpecies().getRegistryName().toString());
    }
    @Inject(method = "shouldPlant", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noSelfPlant(Level level, BlockPos pos, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (wildFruit$protected()) cir.setReturnValue(false);
    }
    @Inject(method = "onItemUseFlowerPot", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noPot(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (wildFruit$protected()) cir.setReturnValue(InteractionResult.FAIL);
    }
    @Inject(method = "getPlant", at = @At("HEAD"), cancellable = true)
    private void wildFruit$noMachinePlant(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (wildFruit$protected()) cir.setReturnValue(Blocks.AIR.defaultBlockState());
    }
}
