package com.bettercontent.betterwildfruittrees.diagnostics;

/** One outer native callback: primitive accounting plus immutable root correlation, never a world/position ledger. */
public final class FruitBatchObservation {
    private final PlaytestLog.Context context;
    private final long creditBefore;
    private final boolean dimensionLedger;
    private long reserved, placed, refunded;
    public FruitBatchObservation(PlaytestLog.Context context, long creditBefore, boolean dimensionLedger) {
        this.context = context; this.creditBefore = creditBefore; this.dimensionLedger = dimensionLedger;
    }
    public void reserved() { reserved++; }
    public void settled(boolean nativeProductObserved) { if (nativeProductObserved) placed++; else refunded++; }
    public long reservedCount() { return reserved; }
    public long placedCount() { return placed; }
    public long refundedCount() { return refunded; }
    public long unsettledCount() { return reserved - placed - refunded; }
    public void finish(PlaytestLog log, boolean completed, long creditAfter) {
        if (!log.enabled("production") || completed && reserved == 0) return;
        log.debug("production", "fruit.batch", context, completed ? "completed" : "aborted",
            completed ? "native_callback_returned" : "native_callback_threw", () -> PlaytestLog.fields(
                "reserved", reserved, "placed", placed, "refunded", refunded, "unsettled", unsettledCount(),
                "credit_before", creditBefore, "credit_after", creditAfter,
                "storage", dimensionLedger ? "dimension_ledger" : "species_root"));
    }
}
