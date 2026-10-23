package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.gametest.nativeprobe.ChunkWriteTraceMixin;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.server.ServerStartingEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Authored stub registry/state fixture only; not a Minecraft startup or native acceptance. */
public final class StateCodebookSelfTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Object a = new String("same"), b = new String("same"), c = new Object();
        var builder = new StateCodebook.Builder<Object>(3);
        builder.add(a, "fixture:tree", Map.of("z", "2", "a", "1"));
        StateCodebook<Object> first = builder.freeze();
        var reorder = new StateCodebook.Builder<Object>(3);
        reorder.add(b, "fixture:tree", Map.of("a", "1", "z", "2"));
        StateCodebook<Object> second = reorder.freeze();
        check(first.lookup(a) == -5483860927355687533L, "SHA256 canonical schema/code drift");
        check(first.complete && first.lookup(a) == second.lookup(b) && first.digest.equals(second.digest), "canonical code depends on identity/property order");
        check(first.lookup(b) == StateCodebook.UNKNOWN && first.lookup(null) == 0, "lookup is not identity-only / null sentinel");
        builder.add(c, "fixture:other", Map.of());
        check(first.lookup(c) == StateCodebook.UNKNOWN && first.entries == 1, "frozen book mutated");
        var swapped = new StateCodebook.Builder<Object>(3);
        swapped.add(c, "fixture:other", Map.of()); swapped.add(b, "fixture:tree", Map.of("z", "2", "a", "1"));
        check(builder.freeze().digest.equals(swapped.freeze().digest), "digest depends on registry iteration order");
        var collision = new StateCodebook.Builder<Object>(3, value -> 42);
        collision.add(a, "fixture:a", Map.of()); collision.add(b, "fixture:b", Map.of());
        check(!collision.freeze().complete && collision.freeze().lookup(a) == StateCodebook.UNKNOWN, "SHA-code collision not fail-closed");
        for (long reserved : new long[] {0, Long.MIN_VALUE}) {
            var invalid = new StateCodebook.Builder<Object>(3, value -> reserved);
            invalid.add(a, "fixture:a", Map.of());
            check(!invalid.freeze().complete, "reserved code admitted");
        }
        var cap = new StateCodebook.Builder<Object>(1);
        cap.add(a, "fixture:a", Map.of()); cap.add(b, "fixture:b", Map.of());
        check(!cap.freeze().complete && cap.freeze().entries == 1 && cap.freeze().lookup(a) == StateCodebook.UNKNOWN, "cap concealed / unsafe partial lookup");
        check(!new StateCodebook.Builder<Object>(1).freeze().complete, "empty book complete");

        var output = new ByteArrayOutputStream(); PrintStream prior = System.out;
        System.setOut(new PrintStream(output));
        try {
            BlockState requested = new BlockState(), old = new BlockState();
            var chunk = new ChunkWriteTraceMixin() {};
            var hook = ChunkWriteTraceMixin.class.getDeclaredMethod("trace$write", BlockPos.class, BlockState.class, boolean.class, CallbackInfoReturnable.class);
            hook.setAccessible(true);
            check(NativeStateCodes.code(requested) == StateCodebook.UNKNOWN, "missing bootstrap not unknown");
            check(BuiltInRegistries.BLOCK.lookups == 0, "lazy registry access");
            OriginalLightTrace.begin("orange", 0); OriginalLightTrace.pulse(1);
            hook.invoke(chunk, new BlockPos(), requested, false, new CallbackInfoReturnable<BlockState>("test", false, old));
            check(output.size() == 0, "in-loop diagnostic IO"); OriginalLightTrace.finish();
            check(output.toString().contains("canonicalIncomplete=true"), "missing bootstrap not incomplete");
            output.reset();
            BuiltInRegistries.BLOCK.add(new Block("fixture:requested", requested));
            BuiltInRegistries.BLOCK.add(new Block("fixture:old", old));
            NativeStateCodes.starting(new ServerStartingEvent());
            check(output.toString().contains("ORIGINAL_STATE_CODEBOOK schema=original-state-v1 entries=2 complete=true"), "startup marker absent/incomplete");
            output.reset();
            int registryReads = BuiltInRegistries.BLOCK.lookups, propertyReads = BlockState.propertyReads;
            OriginalLightTrace.begin("orange", 1); OriginalLightTrace.pulse(1);
            hook.invoke(chunk, new BlockPos(), requested, false, new CallbackInfoReturnable<BlockState>("test", false, old));
            check(output.size() == 0, "write callback IO"); OriginalLightTrace.finish();
            check(output.toString().contains("canonicalUnknown=0 canonicalIncomplete=false"), "known states incomplete");
            check(BuiltInRegistries.BLOCK.lookups == registryReads && BlockState.propertyReads == propertyReads, "hot-path registry/property query");
            check(NativeStateCodes.code(null) == 0, "null returned state treated as missing");
            output.reset();
            OriginalLightTrace.begin("orange", 2); OriginalLightTrace.pulse(1);
            hook.invoke(chunk, new BlockPos(), new BlockState(), false, new CallbackInfoReturnable<BlockState>("test", false, null));
            OriginalLightTrace.finish();
            check(output.toString().contains("canonicalUnknown=1 canonicalIncomplete=true"), "unregistered identity not incomplete");
            check(BuiltInRegistries.BLOCK.lookups == registryReads && BlockState.propertyReads == propertyReads, "unknown lookup became lazy");
        } finally { OriginalLightTrace.finish(); System.setOut(prior); }
        System.out.println("Pure canonical codebook/order/collision/cap/identity/startup/no-lazy/hot-path controls passed");
    }
}
