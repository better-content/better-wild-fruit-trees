package com.bettercontent.betterwildfruittrees.compat;

import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.List;
import java.util.Optional;

/** Only provenance is synchronized; production clocks and allowance are server-owned. */
@Mod.EventBusSubscriber(modid = WildFruitTrees.MOD_ID)
public final class LedgerNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(WildFruitTrees.MOD_ID, "root_provenance"), () -> "1", "1"::equals, "1"::equals);
    private record Snapshot(ResourceLocation dimension, int x, int z, boolean reset, List<BlockPos> roots) {
        void encode(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(dimension); buffer.writeInt(x); buffer.writeInt(z); buffer.writeBoolean(reset);
            buffer.writeCollection(roots, FriendlyByteBuf::writeBlockPos);
        }
        static Snapshot decode(FriendlyByteBuf buffer) {
            var dimension = buffer.readResourceLocation(); int x = buffer.readInt(), z = buffer.readInt();
            boolean reset = buffer.readBoolean();
            int count = buffer.readVarInt();
            if (count < 0 || count > 1024) throw new IllegalArgumentException("Invalid fruit root snapshot");
            var roots = new java.util.ArrayList<BlockPos>(count);
            for (int i = 0; i < count; i++) {
                var root = buffer.readBlockPos();
                if ((root.getX() >> 4) != x || (root.getZ() >> 4) != z) throw new IllegalArgumentException("Root outside tracked chunk");
                roots.add(root);
            }
            return new Snapshot(dimension, x, z, reset, roots);
        }
    }
    static {
        CHANNEL.registerMessage(0, Snapshot.class, Snapshot::encode, Snapshot::decode, (packet, context) -> {
            context.get().enqueueWork(() -> ClientState.accept(packet));
            context.get().setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void initialize() {}
    private static List<Snapshot> snapshots(ServerLevel level, int x, int z) {
        var ledger = RootLedger.get(level);
        // Validate loaded roots before advertising them; historical unmarked mega roots are never inferred.
        var roots = ledger.inheritedInChunk(x, z).stream()
            .filter(root -> RootLedger.inherited(level, root, RootLedger.speciesAt(level, root))).toList();
        var packets = new java.util.ArrayList<Snapshot>();
        // Empty snapshots still clear stale client provenance. Page dense chunks rather than sending
        // an encoder payload that our bounded decoder would reject, or dropping legitimate roots.
        for (int offset = 0; offset < roots.size() || offset == 0; offset += 1024)
            packets.add(new Snapshot(level.dimension().location(), x, z, offset == 0,
                roots.subList(offset, Math.min(offset + 1024, roots.size()))));
        return packets;
    }
    @SubscribeEvent public static void watch(ChunkWatchEvent.Watch event) {
        for (var packet : snapshots(event.getLevel(), event.getPos().x, event.getPos().z))
            CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), packet);
    }
    @SubscribeEvent public static void chunkDetached(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var pos = event.getChunk().getPos(); RootLedger.get(level).suspendChunk(pos.x, pos.z);
        }
    }
    @SubscribeEvent public static void chunkAttached(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var pos = event.getChunk().getPos(); RootLedger.get(level).suspendChunk(pos.x, pos.z);
        }
    }
    public static void changed(ServerLevel level, BlockPos root, boolean inherited) {
        if (level.hasChunkAt(root)) for (var packet : snapshots(level, root.getX() >> 4, root.getZ() >> 4))
            CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(root)), packet);
    }
    public static boolean inherited(Level level, BlockPos root) { return ClientState.contains(level, root); }

    @Mod.EventBusSubscriber(modid = WildFruitTrees.MOD_ID, value = Dist.CLIENT)
    public static final class ClientState {
        private static final java.util.Map<ResourceLocation, java.util.Set<Long>> ROOTS = new java.util.HashMap<>();
        static void accept(Snapshot packet) {
            var roots = ROOTS.computeIfAbsent(packet.dimension, key -> new java.util.HashSet<>());
            if (packet.reset) roots.removeIf(pos -> { var p = BlockPos.of(pos); return (p.getX() >> 4) == packet.x && (p.getZ() >> 4) == packet.z; });
            packet.roots.forEach(pos -> roots.add(pos.asLong()));
        }
        static boolean contains(Level level, BlockPos root) {
            return ROOTS.getOrDefault(level.dimension().location(), java.util.Set.of()).contains(root.asLong());
        }
        @SubscribeEvent public static void unload(ChunkEvent.Unload event) {
            if (event.getLevel() instanceof Level level && level.isClientSide) {
                var pos = event.getChunk().getPos();
                var roots = ROOTS.get(level.dimension().location());
                if (roots != null) roots.removeIf(key -> { var p = BlockPos.of(key); return (p.getX() >> 4) == pos.x && (p.getZ() >> 4) == pos.z; });
            }
        }
        @SubscribeEvent public static void disconnect(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { ROOTS.clear(); }
        @SubscribeEvent public static void levelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) ROOTS.clear();
        }
    }
}
