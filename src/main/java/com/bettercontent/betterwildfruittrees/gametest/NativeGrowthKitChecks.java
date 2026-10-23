package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.compat.OptionalGrowthKitRepair;
import com.ferreusveritas.dynamictrees.api.registry.SimpleRegistry;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKit;
import net.minecraft.gametest.framework.GameTestHelper;

/** Assertions attached to the existing optional stock contract, not another test or a fixture-only alias. */
final class NativeGrowthKitChecks {
    private NativeGrowthKitChecks() {}
    static void verify(GameTestHelper h, boolean present) {
        var scratch = new SimpleRegistry<>(GrowthLogicKit.class, GrowthLogicKit.DEFAULT);
        h.assertTrue(!OptionalGrowthKitRepair.registerIfMissing(false, scratch, () -> {
            throw new AssertionError("Absent addon invoked optional factory");
        }), "Absent native registration unexpectedly changed registry");
        h.assertTrue(!scratch.has(OptionalGrowthKitRepair.REDWOOD), "Absent registration minted native ID");
        GrowthLogicKit existing = new GrowthLogicKit(OptionalGrowthKitRepair.REDWOOD) {};
        GrowthLogicKit hemlock = new GrowthLogicKit(OptionalGrowthKitRepair.HEMLOCK) {};
        scratch.register(existing); scratch.register(hemlock);
        h.assertTrue(!OptionalGrowthKitRepair.registerIfMissing(true, scratch, () -> {
            throw new AssertionError("Existing upstream alias was overridden");
        }), "Already-present native registration not idempotent");
        h.assertTrue(scratch.get(OptionalGrowthKitRepair.REDWOOD) == existing
            && scratch.get(OptionalGrowthKitRepair.HEMLOCK) == hemlock, "Existing alias/hemlock identity changed");
        var nativeRegistry = GrowthLogicKit.REGISTRY;
        h.assertTrue(nativeRegistry.has(OptionalGrowthKitRepair.REDWOOD) == present, "Wrong native redwood optional presence");
        if (present) {
            String nativeClass = "xueluoanping.dtnatures_spirit.systems.growthlogic.RedwoodLogic";
            h.assertTrue(nativeRegistry.get(OptionalGrowthKitRepair.REDWOOD).getClass().getName().equals(nativeClass),
                "Restored alias is not actual pinned native RedwoodLogic");
            h.assertTrue(nativeRegistry.get(OptionalGrowthKitRepair.HEMLOCK).getClass().getName().equals(nativeClass),
                "Existing native hemlock binding lost");
            NativeCalibrationMarkers.emit("OPTIONAL_NATIVE_REDWOOD_BINDING redwood=native_RedwoodLogic hemlock=unchanged_native_RedwoodLogic");
        }
    }
}
