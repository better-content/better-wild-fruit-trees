package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.Harvests;
import com.ferreusveritas.dynamictrees.block.FruitBlock;
import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(FruitBlock.class)
public abstract class FruitBlockMixin {
    @Shadow(remap = false) @Final protected Fruit fruit;
    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void wildFruit$bounty(BlockState state, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (state.hasProperty(fruit.getAgeProperty()) && state.getValue(fruit.getAgeProperty()) == fruit.getMaxAge())
            cir.setReturnValue(Harvests.amplify(fruit, cir.getReturnValue()));
    }
}
