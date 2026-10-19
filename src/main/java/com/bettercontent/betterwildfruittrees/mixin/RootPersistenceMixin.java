package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.TreeFruitBudget;
import com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The pinned native species entity overrides save/load without calling Forge's super methods. */
@Mixin(SpeciesBlockEntity.class)
public abstract class RootPersistenceMixin {
    @Unique private static final java.util.List<String> wildFruit$keys = java.util.List.of(TreeFruitBudget.KEY);

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void wildFruit$saveOwnedData(CompoundTag output, CallbackInfo ci) {
        var persistent = ((BlockEntity)(Object)this).getPersistentData();
        for (String key : wildFruit$keys)
            if (persistent.contains(key, 10)) output.put(key, persistent.getCompound(key).copy());
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void wildFruit$loadOwnedData(CompoundTag input, CallbackInfo ci) {
        var entity = (BlockEntity)(Object)this;
        var persistent = entity.getPersistentData();
        for (String key : wildFruit$keys) {
            // Only restore our own records, not unrelated Forge data/capabilities or native species.
            persistent.remove(key);
            if (input.contains(key, 10)) persistent.put(key, input.getCompound(key).copy());
            else if (input.getCompound("ForgeData").contains(key, 10))
                persistent.put(key, input.getCompound("ForgeData").getCompound(key).copy());
        }
        TreeFruitBudget.resume(entity);
    }
}
