package com.bettercontent.betterwildfruittrees.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Set;

/** Native-only startup adapter. Only globally interned immutable registered states may be retained. */
@Mod.EventBusSubscriber(modid = "better_wild_fruit_trees", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NativeStateCodes {
    public static final String SCOPE = "original-orange-blocks-v1";
    private static final Set<String> NATIVE_KEYS = Set.of("dynamictrees:rooty_dirt", "dynamictrees:oak_branch", "dtfruitsdelight:orange_leaves");
    private static volatile StateCodebook<BlockState> book;
    private NativeStateCodes() {}

    @SubscribeEvent
    public static void starting(ServerStartingEvent ignored) {
        // Forge GameTestServer.initServer posts this before tickServer starts tests. No server/world is read or retained.
        try {
            var builder = new StateCodebook.Builder<BlockState>(131072);
            var selectedKeys = new TreeSet<String>();
            int states = 0;
            outer: for (var block : BuiltInRegistries.BLOCK) {
                String name = BuiltInRegistries.BLOCK.getKey(block).toString();
                if (!selected(name)) continue; // Select keys BEFORE asking for any possible states.
                selectedKeys.add(name);
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    if (++states > 131072) { builder.exceeded(); break outer; }
                    var properties = new TreeMap<String, String>();
                    state.getValues().forEach((property, value) -> properties.put(property.getName(), value(property, value)));
                    builder.add(state, name, properties);
                }
            }
            StateCodebook<BlockState> frozen = builder.freeze();
            boolean scopeComplete = frozen.excess == 0 && selectedKeys.containsAll(NATIVE_KEYS);
            boolean complete = frozen.complete && scopeComplete;
            book = complete ? frozen : null;
            OriginalLightTrace.stateCodesReady(complete);
            System.out.println("ORIGINAL_STATE_CODEBOOK schema=" + StateCodebook.SCHEMA + " entries=" + frozen.entries
                + " complete=" + complete + " scope=" + SCOPE + " scopeComplete=" + scopeComplete
                + " selectedBlockKeys=" + selectedKeys + " collisionsOrInvalid=" + frozen.collisions + " excess=" + frozen.excess
                + " digest=" + frozen.digest + " nullCode=0 unknownCode=" + StateCodebook.UNKNOWN);
        } catch (RuntimeException incomplete) {
            book = null;
            OriginalLightTrace.stateCodesReady(false);
            System.out.println("ORIGINAL_STATE_CODEBOOK schema=" + StateCodebook.SCHEMA + " scope=" + SCOPE + " selectedBlockKeys=unavailable digest=unavailable complete=false reason=" + incomplete.getClass().getName());
        }
    }
    static boolean selected(String key) {
        if (key == null) return false;
        int colon = key.indexOf(':');
        if (colon <= 0 || colon != key.lastIndexOf(':') || colon == key.length() - 1) return false;
        return key.substring(0, colon).equals("minecraft") || NATIVE_KEYS.contains(key);
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
