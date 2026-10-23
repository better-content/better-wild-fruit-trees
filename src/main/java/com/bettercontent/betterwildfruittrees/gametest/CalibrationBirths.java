package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import com.ferreusveritas.dynamictrees.entity.FallingTreeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Nonshipping auto-subscriber installed during mod construction, before GameTest levels/entities exist. */
@Mod.EventBusSubscriber(modid = WildFruitTrees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public final class CalibrationBirths {
    private static final int ROOT_X = scopeRoot();
    private static final CalibrationBirthLedger LEDGER = new CalibrationBirthLedger();
    // Loaded block data is insufficient: the native VOID entity must get its real server tick.
    // Nonpersistent region tickets expire even if an assertion/timeout prevents normal release.
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TICKETS =
        net.minecraft.server.level.TicketType.create("wild_fruit_calibration",
            java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 2000);
    private CalibrationBirths() {}
    private static int scopeRoot() {
        if (!Boolean.getBoolean("forge.gameTestServer")
                || !Boolean.getBoolean("better_wild_fruit_trees.fixture.wild_addons")) return Integer.MIN_VALUE;
        String namespace = System.getProperty("forge.enabledGameTestNamespaces", "");
        if (namespace.equals("better_wild_fruit_calibration")) return 48;
        if (namespace.equals("better_wild_fruit_trees")) return -160;
        return Integer.MIN_VALUE;
    }
    private static boolean relevant(ServerLevel level) {
        return ROOT_X != Integer.MIN_VALUE && level.dimension().location().toString().equals("minecraft:overworld");
    }
    private static boolean inside(BlockPos cut) {
        return cut.getX() >= ROOT_X - 44 && cut.getX() <= ROOT_X + 44
            && cut.getY() >= 160 && cut.getY() <= 220 && cut.getZ() >= 4 && cut.getZ() <= 92;
    }
    private static void checkThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) LEDGER.fault("Off-server-thread fixture entity lifecycle");
    }
    @SubscribeEvent
    public static void loaded(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && relevant(level)) {
            checkThread(level); LEDGER.loaded();
        }
    }
    @SubscribeEvent
    public static void unloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && relevant(level)) LEDGER.unloaded();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !relevant(level)
                || !(event.getEntity() instanceof FallingTreeEntity entity)) return;
        checkThread(level);
        var data = entity.getDestroyData();
        if (data == null || data.cutPos == null || data.getNumBranches() <= 0) {
            LEDGER.fault("Unready fixture falling-tree destruction data"); return;
        }
        if (!inside(data.cutPos)) return;
        LEDGER.join(entity.getUUID(), data.cutPos.getX(), data.cutPos.getY(), data.cutPos.getZ(), event.isCanceled());
        // LOWEST is NOT claimed to be the final cancellation decision or successful insertion.
    }
    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !relevant(level)
                || !(event.getEntity() instanceof FallingTreeEntity entity)) return;
        checkThread(level);
        if (!LEDGER.knows(entity.getUUID())) {
            var data = entity.getDestroyData();
            if (data != null && data.cutPos != null && inside(data.cutPos))
                LEDGER.fault("Fixture removal without captured birth");
            return;
        }
        var data = entity.getDestroyData();
        if (data == null || data.cutPos == null
                || !LEDGER.cutMatches(entity.getUUID(), data.cutPos.getX(), data.cutPos.getY(), data.cutPos.getZ())) {
            LEDGER.fault("Fixture removal destruction identity drift"); return;
        }
        var reason = entity.getRemovalReason();
        LEDGER.leave(entity.getUUID(), reason != null && reason.shouldDestroy());
    }
    static void assertScope(GameTestHelper h, BlockPos root) {
        h.assertTrue(ROOT_X == root.getX() && root.getY() == 160 && root.getZ() == 48,
            "Fixture birth scope not armed for original plot");
        h.assertTrue(LEDGER.levelReady(), "Fixture early observer/level lifecycle missing or failed: " + LEDGER.failure());
    }
    private static void plotChunks(BlockPos root, java.util.function.Consumer<net.minecraft.world.level.ChunkPos> action) {
        for (int x = (root.getX() - 44) >> 4; x <= (root.getX() + 44) >> 4; x++)
            for (int z = (root.getZ() - 44) >> 4; z <= (root.getZ() + 44) >> 4; z++)
                action.accept(new net.minecraft.world.level.ChunkPos(x, z));
    }
    static void armTicks(GameTestHelper h, BlockPos root) {
        assertScope(h, root);
        plotChunks(root, chunk -> h.getLevel().getChunkSource().addRegionTicket(TICKETS, chunk, 2, chunk));
    }
    static void releaseTicks(GameTestHelper h, BlockPos root) {
        plotChunks(root, chunk -> h.getLevel().getChunkSource().removeRegionTicket(TICKETS, chunk, 2, chunk));
    }
    static boolean ticking(GameTestHelper h, BlockPos root) {
        boolean[] ready = {true};
        plotChunks(root, chunk -> ready[0] &= h.getLevel().isPositionEntityTicking(
            new BlockPos(chunk.getMiddleBlockX(), root.getY(), chunk.getMiddleBlockZ())));
        return ready[0];
    }
    static boolean quiescent(GameTestHelper h, BlockPos root) {
        assertScope(h, root);
        // Bounded scalar IDs only: getEntity(UUID) is NOT a global/spatial collection scan or chunk load.
        for (var id : LEDGER.identities()) {
            var loaded = h.getLevel().getEntity(id);
            if (loaded == null) { LEDGER.lookupAbsent(id); continue; }
            if (!(loaded instanceof FallingTreeEntity entity)) {
                LEDGER.fault("Fixture UUID resolved to wrong native type"); continue;
            }
            var data = entity.getDestroyData();
            if (data == null || data.cutPos == null
                    || !LEDGER.cutMatches(id, data.cutPos.getX(), data.cutPos.getY(), data.cutPos.getZ())) {
                LEDGER.fault("Fixture UUID destruction identity drift"); continue;
            }
            LEDGER.lookupPresent(id);
        }
        h.assertTrue(LEDGER.failure() == null, "Fixture identity capture failed: " + LEDGER.failure());
        return LEDGER.quiescent();
    }
    static void releaseRetired() { LEDGER.releaseRetired(); }
}
