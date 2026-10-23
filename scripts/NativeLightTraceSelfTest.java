import com.bettercontent.betterwildfruittrees.gametest.OriginalLightTrace;
import com.bettercontent.betterwildfruittrees.gametest.nativeprobe.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.*;

/** Pure authored stubs, no Minecraft/Forge startup. Not native acceptance. */
public final class NativeLightTraceSelfTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Throwable {
        Method method = target.getClass().getSuperclass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        try { return method.invoke(target, args); }
        catch (InvocationTargetException error) { throw error.getCause(); }
    }
    static long[] hashes(String text) {
        var matcher = java.util.regex.Pattern.compile("stableHashes=\\[([^]]+)\\]").matcher(text);
        check(matcher.find(), "missing comparable channels");
        return java.util.Arrays.stream(matcher.group(1).split(", ")).mapToLong(Long::parseLong).toArray();
    }
    static long scalar(String text, String key) {
        var matcher = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(key)+"=(-?[0-9]+)").matcher(text);
        check(matcher.find(), "missing refined channel " + key); return Long.parseLong(matcher.group(1));
    }
    static long[] histogram(String text) {
        var matcher = java.util.regex.Pattern.compile("skyIntensityHistogram=\\[([^]]+)\\]").matcher(text);
        check(matcher.find(), "missing histogram");
        return java.util.Arrays.stream(matcher.group(1).split(", ")).mapToLong(Long::parseLong).toArray();
    }
    public static void main(String[] args) throws Throwable {
        var output = new ByteArrayOutputStream();
        var prior = System.out;
        System.setOut(new PrintStream(output));
        try {
            OriginalLightTrace.stateCodesReady(true);
            var leaves = new LeavesLightTraceMixin() {};
            var pos = new BlockPos();
            var failure = new IllegalStateException("same original exception");
            int[] calls = {0};
            boolean[] sky = {true};
            int[] brightness = {7};
            boolean[] throwing = {false};
            LevelAccessor level = new LevelAccessor() {
                public boolean canSeeSkyFromBelowWater(BlockPos p) {
                    check(p == pos, "position identity changed"); calls[0]++;
                    if (throwing[0]) throw failure;
                    return sky[0];
                }
                public int getBrightness(LightLayer layer, BlockPos p) {
                    check(p == pos && layer == LightLayer.SKY, "original args changed"); calls[0]++;
                    if (throwing[0]) throw failure;
                    return brightness[0];
                }
            };
            Class<?>[] seeTypes = {LevelAccessor.class, BlockPos.class};
            Class<?>[] brightTypes = {LevelAccessor.class, LightLayer.class, BlockPos.class};
            OriginalLightTrace.begin("apple", 0);
            check(!OriginalLightTrace.ownerActive(), "nonorange active");
            for (boolean active : new boolean[] {false, true}) {
                if (active) { OriginalLightTrace.begin("orange", 0); OriginalLightTrace.pulse(1); }
                for (boolean flag : new boolean[] {false, true}) {
                    sky[0] = flag;
                    int before = calls[0], encodes = pos.encodes;
                    check(invoke(leaves, "trace$canSee", seeTypes, level, pos).equals(flag), "changed boolean result");
                    check(calls[0] == before + 1, "boolean original not exactly once");
                    check(pos.encodes == encodes + (active ? 1 : 0), "inactive observational lookup");
                }
                for (int value : new int[] {0, 7, 15}) {
                    brightness[0] = value; int before = calls[0];
                    check(invoke(leaves, "trace$brightness", brightTypes, level, LightLayer.SKY, pos).equals(value), "changed int result");
                    check(calls[0] == before + 1, "brightness original not exactly once");
                }
                throwing[0] = true;
                for (boolean see : new boolean[] {false, true}) {
                    int before = calls[0], encodes = pos.encodes;
                    try {
                        if (see) invoke(leaves, "trace$canSee", seeTypes, level, pos);
                        else invoke(leaves, "trace$brightness", brightTypes, level, LightLayer.SKY, pos);
                        throw new AssertionError("original exception swallowed");
                    } catch (IllegalStateException error) { check(error == failure, "exception identity changed"); }
                    check(calls[0] == before + 1 && pos.encodes == encodes, "exception path queried or called again");
                }
                throwing[0] = false;
            }
            check(output.size() == 0, "IO inside original loop");
            OriginalLightTrace.skyEnter(41);
            Thread worker = new Thread(() -> { for (int i = 0; i < 32; i++) OriginalLightTrace.published(41); });
            worker.start(); worker.join(); // Test harness only; production observer has no wait/barrier.
            OriginalLightTrace.skyReturn(41, 123L, 9);
            OriginalLightTrace.write(123L, 12, 13, true);
            OriginalLightTrace.canonicalWrite(123L, 101L, 202L);
            try { try { throw failure; } finally { OriginalLightTrace.finish(); } }
            catch (IllegalStateException error) { check(error == failure, "scope changed original exception"); }
            check(!OriginalLightTrace.ownerActive() && !OriginalLightTrace.publicationActive(), "scope leaked");
            String record = output.toString();
            check(record.contains("diagnosticOnly=true overflow=false"), "bad normal diagnostic");
            check(record.contains("observedPublications=32") && record.contains("minSky=9"), "missing actual read/publications");
            output.reset();
            OriginalLightTrace.begin("orange", 1); OriginalLightTrace.pulse(1);
            OriginalLightTrace.skyEnter(99); OriginalLightTrace.skyReturn(99, 123L, 9);
            OriginalLightTrace.write(123L, 999, 0, false);
            OriginalLightTrace.canonicalWrite(123L, 101L, 202L);
            OriginalLightTrace.finish();
            long[] original = hashes(record), changedOpaque = hashes(output.toString());
            check(original[0] == changedOpaque[0], "storage identity/version contaminated gameplay-light hash");
            check(original[4] == changedOpaque[4], "state identity/change result contaminated position-only hash");
            check(scalar(record, "skyPositionHash") == scalar(output.toString(), "skyPositionHash"), "sky position hash depends on opaque identity");
            check(scalar(record, "canonicalWriteHash") == scalar(output.toString(), "canonicalWriteHash"), "canonical hash depends on opaque identity");
            check(histogram(record).length == 16 && histogram(record)[9] == 1 && java.util.Arrays.stream(histogram(record)).sum() == 1, "bad actual light histogram");
            output.reset();
            OriginalLightTrace.begin("orange", 3); OriginalLightTrace.pulse(1);
            OriginalLightTrace.skyEnter(101); OriginalLightTrace.skyReturn(101, 123L, 14);
            OriginalLightTrace.write(123L, 999, 0, false); OriginalLightTrace.canonicalWrite(123L, 103L, 202L);
            OriginalLightTrace.finish();
            check(scalar(record, "skyPositionHash") == scalar(output.toString(), "skyPositionHash"), "light-result change contaminated location-only hash");
            check(hashes(record)[0] != hashes(output.toString())[0] && histogram(output.toString())[14] == 1, "actual light change concealed");
            check(hashes(record)[4] == hashes(output.toString())[4] && scalar(record, "canonicalWriteHash") != scalar(output.toString(), "canonicalWriteHash"), "canonical state change concealed/position-only channel altered");
            output.reset();
            OriginalLightTrace.begin("orange", 4); OriginalLightTrace.pulse(1);
            OriginalLightTrace.skyEnter(303); OriginalLightTrace.skyReturn(303, 124L, 9); OriginalLightTrace.finish();
            check(scalar(record, "skyPositionHash") != scalar(output.toString(), "skyPositionHash") && java.util.Arrays.equals(histogram(record), histogram(output.toString())), "query-location change not separable from actual intensities");
            output.reset();
            OriginalLightTrace.begin("orange", 6); OriginalLightTrace.pulse(4097); OriginalLightTrace.finish();
            check(output.toString().contains("overflow=true"), "pulse overflow concealed");
            output.reset();
            OriginalLightTrace.begin("orange", 7); OriginalLightTrace.pulse(4096);
            for (int i = 0; i < 17; i++) OriginalLightTrace.published(i);
            OriginalLightTrace.finish();
            check(output.toString().contains("overflow=true"), "storage overflow concealed");
            check(output.toString().lines().filter(line -> line.startsWith("ORIGINAL_LIGHT_STORAGE ")).count() == 16, "unbounded storage IDs");
            OriginalLightTrace.begin("orange", 8);
            check(!OriginalLightTrace.ownerActive(), "extra seed admitted");
            // Opt-in actual redirect bodies: exact-once forwarding, paired BEGIN/RETURN,
            // and original throwing invocation leaves a diagnostic-incomplete scope only.
            var attempt = java.nio.file.Path.of(args[0]).resolve(java.util.UUID.randomUUID().toString());
            java.nio.file.Files.createDirectory(attempt);
            var bindings = new java.util.Properties();
            bindings.setProperty("nonce", attempt.getFileName().toString());
            bindings.setProperty("attemptDirectory", attempt.toAbsolutePath().toString());
            bindings.setProperty("profile", "pure-authored-stubs-not-native");
            bindings.setProperty("sourceCommit", "a".repeat(40)); bindings.setProperty("sourceDirty", "false");
            for (String key : new String[]{"classHashSetSha256", "nativeInputReceiptSha256", "dictDigest"}) bindings.setProperty(key,"b".repeat(64));
            bindings.setProperty("dictSchema", "unit"); bindings.setProperty("dictScope", "unit"); bindings.setProperty("dictComplete", "true");
            var events = attempt.resolve("original-events");
            com.bettercontent.betterwildfruittrees.gametest.OrderedEvents.configure(true, events, bindings);
            OriginalLightTrace.begin("orange", 0); OriginalLightTrace.pulse(1);
            throwing[0] = false; int countBefore = calls[0];
            invoke(leaves, "trace$canSee", seeTypes, level, pos);
            check(calls[0] == countBefore + 1, "ordered mode forwarded twice");
            OriginalLightTrace.finish();
            check(com.bettercontent.betterwildfruittrees.gametest.OrderedEvents.read(events.resolve("sample-0.wftord")).count() == 3, "paired query boundaries/STOP missing");
            OriginalLightTrace.begin("orange", 1); OriginalLightTrace.pulse(1); throwing[0] = true;
            try { invoke(leaves, "trace$brightness", brightTypes, level, LightLayer.SKY, pos); throw new AssertionError("missing original exception"); }
            catch (IllegalStateException caught) { check(caught == failure, "ordered redirect replaced original exception"); }
            finally { OriginalLightTrace.finish(); }
            try { com.bettercontent.betterwildfruittrees.gametest.OrderedEvents.read(events.resolve("sample-1.wftord")); throw new AssertionError("throwing query declared complete"); }
            catch (java.io.IOException incomplete) { /* dangling BEGIN is not measurement completion */ }
            com.bettercontent.betterwildfruittrees.gametest.OrderedEvents.configure(false, events, bindings);
        } finally { OriginalLightTrace.finish(); System.setOut(prior); }
        System.out.println("Pure native light forwarding/result/exception/scope/bounds controls passed");
    }
}
