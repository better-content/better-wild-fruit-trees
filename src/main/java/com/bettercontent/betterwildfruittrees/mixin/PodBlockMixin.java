package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.Harvests;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.block.PodBlock;
import com.ferreusveritas.dynamictrees.systems.pod.Pod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** FallingPodBlock.getDropOnFallItems calls Block.getDrops: amplify at that shared loot path only. */
@Mixin(PodBlock.class)
public abstract class PodBlockMixin {
    @Shadow(remap = false) @Final protected Pod pod;
    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void wildFruit$bounty(BlockState state, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (WildFruitPolicy.COCONUT.equals(pod.getRegistryName().toString()) && state.hasProperty(pod.getAgeProperty())
                && state.getValue(pod.getAgeProperty()) == pod.getMaxAge())
            cir.setReturnValue(Harvests.amplify(pod.getRegistryName().toString(), pod.getItemStack(), cir.getReturnValue()));
    }
}
