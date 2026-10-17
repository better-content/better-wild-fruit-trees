package com.bettercontent.betterwildfruittrees.mixin;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    /** Filter only explicit JSON item outputs; do not evaluate unrelated recipes with an empty registry. */
    @ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"), argsOnly = true)
    private Map<ResourceLocation, JsonElement> wildFruit$removeConversions(Map<ResourceLocation, JsonElement> original) {
        Map<ResourceLocation, JsonElement> filtered = new HashMap<>(original);
        filtered.entrySet().removeIf(entry -> {
            JsonElement json = entry.getValue();
            if (!json.isJsonObject()) return false;
            JsonElement result = json.getAsJsonObject().get("result");
            if (result == null) return false;
            if (result.isJsonObject()) result = result.getAsJsonObject().get("item");
            return result != null && result.isJsonPrimitive() && result.getAsJsonPrimitive().isString()
                && WildFruitPolicy.blockedPlantItem(result.getAsString());
        });
        return filtered;
    }
}
