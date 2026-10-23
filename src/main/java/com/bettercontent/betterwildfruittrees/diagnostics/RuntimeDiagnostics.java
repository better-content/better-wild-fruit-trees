package com.bettercontent.betterwildfruittrees.diagnostics;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** One process-local budget; never retains world/player/menu references. */
@Mod.EventBusSubscriber(modid = "better_wild_fruit_trees")
public final class RuntimeDiagnostics {
    public static final PlaytestLog LOG = new PlaytestLog("better_wild_fruit_trees");
    private RuntimeDiagnostics() {}
    public static void initialize() {
        var mods = net.minecraftforge.fml.ModList.get();
        var own = mods.getModFileById("better_wild_fruit_trees");
        String source = LOG.sourceIdentity(own == null ? null : own.getFile().findResource("META-INF", "better-content-source.properties"));
        String version = mods.getModContainerById("better_wild_fruit_trees").map(c -> c.getModInfo().getVersion().toString()).orElse("UNKNOWN");
        LOG.status("common", version, source, () -> PlaytestLog.fields("enabled", true, "dynamictrees", mods.isLoaded("dynamictrees"), "dtplus", mods.isLoaded("dynamictreesplus"), "dtquark", mods.isLoaded("dtquark"), "dtnatures_spirit", mods.isLoaded("dtnatures_spirit"), "unmeasured_references", com.bettercontent.betterwildfruittrees.TreeGrowthRates.missingOptionalReferences().size()));
    }
    public static void event(String category, String event, String result, String reason) {
        if (!LOG.enabled(category)) return;
        FeatureObservation.decision(LOG, category, event, result, reason);
    }
    public static PlaytestLog.Context context(net.minecraft.world.level.Level level) {
        return new PlaytestLog.Context(level.isClientSide ? "client" : "server", level.dimension().location().toString(), level.getGameTime(), "", "");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) { FeatureObservation.tick(LOG); LOG.summary("server"); }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { FeatureObservation.stopped(LOG); LOG.reset("server"); }
}
