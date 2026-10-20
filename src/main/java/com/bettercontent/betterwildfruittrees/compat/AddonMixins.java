package com.bettercontent.betterwildfruittrees.compat;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Never link optional addon classes while that addon is absent. Version bounds live in mods.toml. */
public final class AddonMixins implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        String addon = mixin.endsWith("CactusFruitGenFeatureMixin") ? "dynamictreesplus"
            : mixin.endsWith("PalmFruitGenFeatureMixin") || mixin.endsWith("CoconutFoodBlockMixin") ? "dtnatures_spirit" : null;
        return addon == null || FMLLoader.getLoadingModList().getModFileById(addon) != null;
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
