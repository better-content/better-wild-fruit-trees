package com.bettercontent.betterwildfruittrees.compat;

import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import com.bettercontent.betterwildfruittrees.diagnostics.RuntimeDiagnostics;
import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.api.registry.RegistryEvent;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKit;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.util.function.Supplier;

/** Repairs only the missing ID requested by pinned DTNS 1.3 redwood species resources. */
@Mod.EventBusSubscriber(modid = WildFruitTrees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OptionalGrowthKitRepair {
    public static final ResourceLocation REDWOOD = new ResourceLocation("dtnatures_spirit", "redwood");
    public static final ResourceLocation HEMLOCK = new ResourceLocation("dtnatures_spirit", "hemlock");
    private OptionalGrowthKitRepair() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(RegistryEvent<GrowthLogicKit> event) {
        var registry = event.getRegistry();
        var hemlockBefore = registry.getOptional(HEMLOCK);
        // The supplier body (and optional class) is reached only after the actual presence/ID guards.
        boolean repaired = registerIfMissing(ModList.get().isLoaded("dtnatures_spirit"), registry,
            () -> NativeLink.create());
        if (!hemlockBefore.equals(registry.getOptional(HEMLOCK)))
            throw new IllegalStateException("Native hemlock binding changed during redwood ID repair");
        if (repaired) RuntimeDiagnostics.event("registry", "growth_kit.restore", "registered", "pinned_missing_native_redwood_id");
    }

    /** Native controls exercise the exact guard without invoking an optional factory when absent/existing. */
    public static boolean registerIfMissing(boolean present, Registry<GrowthLogicKit> registry, Supplier<GrowthLogicKit> nativeFactory) {
        if (!present || registry.has(REDWOOD)) return false;
        var nativeKit = nativeFactory.get();
        if (!REDWOOD.equals(nativeKit.getRegistryName())) throw new IllegalArgumentException("Wrong native redwood repair ID");
        registry.register(nativeKit);
        return true;
    }

    /** No optional type appears in outer class signatures, fields, annotations or method references. */
    private static final class NativeLink {
        private static GrowthLogicKit create() {
            return new xueluoanping.dtnatures_spirit.systems.growthlogic.RedwoodLogic(REDWOOD);
        }
    }
}
