package com.bettercontent.betterwildfruittrees.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.TreeMap;

/** Native-only startup adapter. Only globally interned immutable registered states may be retained. */
@Mod.EventBusSubscriber(modid = "better_wild_fruit_trees", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NativeStateCodes {
    private static volatile StateCodebook<BlockState> book;
    private NativeStateCodes() {}

    @SubscribeEvent
    public static void starting(ServerStartingEvent ignored) {
        // Forge GameTestServer.initServer posts this before tickServer starts tests. No server/world is read or retained.
        try {
            var builder = new StateCodebook.Builder<BlockState>(131072);
            int states = 0;
            outer: for (var block : BuiltInRegistries.BLOCK) {
                String name = BuiltInRegistries.BLOCK.getKey(block).toString();
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    if (++states > 131072) { builder.exceeded(); break outer; }
                    var properties = new TreeMap<String, String>();
                    state.getValues().forEach((property, value) -> properties.put(property.getName(), value(property, value)));
                    builder.add(state, name, properties);
                }
            }
            StateCodebook<BlockState> frozen = builder.freeze();
            book = frozen;
            OriginalLightTrace.stateCodesReady(frozen.complete);
            System.out.println("ORIGINAL_STATE_CODEBOOK schema=" + StateCodebook.SCHEMA + " entries=" + frozen.entries
                + " complete=" + frozen.complete + " collisionsOrInvalid=" + frozen.collisions + " excess=" + frozen.excess
                + " digest=" + frozen.digest + " nullCode=0 unknownCode=" + StateCodebook.UNKNOWN);
        } catch (RuntimeException incomplete) {
            book = null;
            OriginalLightTrace.stateCodesReady(false);
            System.out.println("ORIGINAL_STATE_CODEBOOK schema=" + StateCodebook.SCHEMA + " complete=false reason=" + incomplete.getClass().getName());
        }
    }
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String value(Property<T> property, Comparable<?> value) {
        return property.getName((T)value);
    }
    public static long code(BlockState state) {
        // No lazy bootstrap, property/registry/world calls, locks, I/O, allocation or readiness wait.
        if (state == null) return StateCodebook.NULL;
        StateCodebook<BlockState> frozen = book;
        return frozen == null ? StateCodebook.UNKNOWN : frozen.lookup(state);
    }
}
