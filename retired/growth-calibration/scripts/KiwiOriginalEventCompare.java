package com.bettercontent.betterwildfruittrees.gametest;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** DISTINCT kiwi-v1 entry point; never admits cross-profile comparison or native acceptance. */
public final class KiwiOriginalEventCompare {
    static void scope(OrderedEvents.Capture capture, Path trace) throws Exception {
        Properties p = capture.metadata();
        if (!"original-state-v1".equals(p.getProperty("dictSchema"))
            || !"true".equals(p.getProperty("dictComplete"))
            || !p.getProperty("dictDigest", "").matches("[a-f0-9]{64}")
            || !"original-kiwi-blocks-v1".equals(p.getProperty("dictScope"))
            || !"kiwi".equals(p.getProperty("observedSpecies"))
            || !"kiwi-v1".equals(p.getProperty("traceScope"))
            || !p.getProperty("profile", "").endsWith(":traceScope=kiwi-v1"))
            throw new IOException("not the explicit kiwi-v1 observation scope");
        Properties binding = new Properties();
        Path attempt = trace.toAbsolutePath().normalize().getParent().getParent();
        try (var input = Files.newInputStream(attempt.resolve("original-events-bindings.properties"))) { binding.load(input); }
        for (String key : List.of("traceScope", "observedSpecies"))
            if (!Objects.equals(p.getProperty(key), binding.getProperty(key)))
                throw new IOException("kiwi scope binding mismatch: " + key);
    }
    static void completion(OrderedEvents.Capture capture, Path trace, Path log, String expected) throws Exception {
        scope(capture, trace);
        OriginalEventCompare.completion(capture, trace, log, expected, "kiwi");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 6) throw new IllegalArgumentException("TRACE1 LOG1 LOG1_SHA256 TRACE2 LOG2 LOG2_SHA256");
        try {
            var a = OrderedEvents.read(Path.of(args[0])); var b = OrderedEvents.read(Path.of(args[3]));
            completion(a, Path.of(args[0]), Path.of(args[1]), args[2]);
            completion(b, Path.of(args[3]), Path.of(args[4]), args[5]);
            // Keep the ORIGINAL comparator's exact profile/source/class/dictionary equality.
            // In particular, present versus absent native profiles remain INCOMPARABLE.
            System.out.println("KIWI_V1_LOG_BINDINGS sha256=" + args[2] + "," + args[5]
                + " outcomes=receiptThenPostAssertCorrelatedNotAuthenticatedNotNativeGateWaiver");
            System.out.println(OriginalEventCompare.compare(a, b));
        } catch (IOException invalid) {
            System.out.println("INCONCLUSIVE_INVALID_OR_INCOMPLETE " + invalid.getMessage()); System.exit(2);
        }
    }
}
