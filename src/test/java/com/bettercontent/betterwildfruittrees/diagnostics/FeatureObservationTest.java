package com.bettercontent.betterwildfruittrees.diagnostics;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class FeatureObservationTest {
    public static void main(String[] args) { new FeatureObservationTest().outerBatchConservesReservationsAndPreservesAbortAndPrivacy(); }
    @Test void outerBatchConservesReservationsAndPreservesAbortAndPrivacy() {
        var records = new ArrayList<String>(); boolean[] enabled = {false};
        var log = new PlaytestLog("better_wild_fruit_trees", new PlaytestLog.Sink() {
            public boolean enabled(String c, String l) { return enabled[0]; }
            public void write(String c, String l, String text) { records.add(text); }
        }, () -> 0L, new Properties());
        FeatureObservation.decision(log, "production", "fruit.attempt", "deferred", "unloaded_root");
        var root = new PlaytestLog.Context("server", "minecraft:overworld", 120, "", "4,64,-7");
        var quiet = new FruitBatchObservation(root, 5000, true);
        quiet.reserved(); quiet.settled(true); quiet.finish(log, true, 2500);
        assertTrue(records.isEmpty());
        enabled[0] = true;
        for (int i = 0; i < 600; i++) FeatureObservation.tick(log);
        assertTrue(records.isEmpty(), "disabled callbacks did not accrue deferral totals");
        var batch = new FruitBatchObservation(root, 5000, true);
        for (int i = 0; i < 8; i++) { batch.reserved(); batch.settled(i < 2); }
        assertEquals(8, batch.reservedCount()); assertEquals(2, batch.placedCount());
        assertEquals(6, batch.refundedCount()); assertEquals(0, batch.unsettledCount());
        assertTrue(records.isEmpty(), "no per-cell details");
        batch.finish(log, true, 0);
        assertEquals(1, records.size());
        assertTrue(records.get(0).contains("event=fruit.batch"));
        assertTrue(records.get(0).contains("result=completed reason=native_callback_returned"));
        assertTrue(records.get(0).contains("reserved=8 placed=2 refunded=6 unsettled=0 credit_before=5000 credit_after=0"));
        assertTrue(records.get(0).contains("dimension=minecraft:overworld tick=120 pos=4,64,-7"));
        // Same coordinates in another dimension are different existing root operations, not a new root ID.
        var otherRoot = new PlaytestLog.Context("server", "minecraft:the_nether", 121, "", "4,64,-7");
        var aborted = new FruitBatchObservation(otherRoot, 5000, false);
        aborted.reserved(); aborted.settled(true); aborted.reserved();
        aborted.finish(log, false, 0);
        assertEquals(2, records.size());
        assertTrue(records.get(1).contains("result=aborted reason=native_callback_threw"));
        assertTrue(records.get(1).contains("reserved=2 placed=1 refunded=0 unsettled=1"));
        assertTrue(records.get(1).contains("dimension=minecraft:the_nether tick=121 pos=4,64,-7"));
        var deferred = new FeatureObservation.Deferrals();
        deferred.record("unloaded_root"); deferred.record("insufficient_credit"); deferred.record("ledger_allowance_or_schema");
        for (int i = 0; i < 599; i++) deferred.tick(log);
        assertEquals(2, records.size()); deferred.tick(log); assertEquals(3, records.size());
        assertTrue(records.get(2).contains("unloaded_roots=1 insufficient_credit=1 ledger_rejected=1"));
        deferred.flush(log); assertEquals(3, records.size());
        assertFalse(records.get(2).contains("dimension="), "global deferrals are intentionally multi-root/multi-world");
        assertFalse(records.get(2).contains("pos="));
        assertTrue(records.stream().noneMatch(s -> s.contains("seed=") || s.contains("nbt=") || s.contains("subject=") || s.contains("cell=") || s.contains("harvested=")));
        FeatureObservation.stopped(log);
    }
}
