package com.bettercontent.betterwildfruittrees.diagnostics;

/** Deferred production callbacks are primitive totals, never random-tick detail records. */
public final class FeatureObservation {
    private static final Deferrals DEFERRALS = new Deferrals();
    private FeatureObservation() {}
    public static void decision(PlaytestLog log, String category, String event, String result, String reason) {
        if (!log.enabled(category)) return;
        if (category.equals("production") && event.equals("fruit.attempt")) { DEFERRALS.record(reason); return; }
        log.event(category, event, "server", result, reason, () -> "");
    }
    public static void tick(PlaytestLog log) { if (log.enabled("production")) DEFERRALS.tick(log); }
    public static void stopped(PlaytestLog log) { DEFERRALS.flush(log); DEFERRALS.reset(); }
    public static final class Deferrals {
        private long unloaded, credit, ledger, other;
        private int ticks;
        public void record(String reason) {
            switch (reason) {
                case "unloaded_root" -> unloaded++;
                case "insufficient_credit" -> credit++;
                case "ledger_allowance_or_schema" -> ledger++;
                default -> other++;
            }
        }
        public void tick(PlaytestLog log) { if (++ticks >= 600) { ticks = 0; flush(log); } }
        public void flush(PlaytestLog log) {
            if (unloaded + credit + ledger + other == 0) return;
            if (log.enabled("production")) {
                long u = unloaded, c = credit, l = ledger, o = other;
                log.event("production", "fruit.deferred_batch", "server", "deferred", "native_tick_window",
                    () -> PlaytestLog.fields("unloaded_roots", u, "insufficient_credit", c, "ledger_rejected", l, "other", o));
            }
            unloaded = credit = ledger = other = 0;
        }
        public void reset() { unloaded = credit = ledger = other = 0; ticks = 0; }
    }
}
