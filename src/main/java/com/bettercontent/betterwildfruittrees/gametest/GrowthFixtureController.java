package com.bettercontent.betterwildfruittrees.gametest;

import java.util.ArrayList;
import java.util.List;

/** V2 scheduling/aggregation only. Native growth remains in the adapter; no world or static sums. */
public final class GrowthFixtureController {
    public static final int SAMPLES = 8, SETUP_LIMIT = 200, PULSE_LIMIT = 4096;
    // Includes one prepare, two admission/transition callbacks and one cleanup callback per sample,
    // plus a finalizer. It is a native-tick safety bound, never a wallclock expectation.
    public static final int MAX_TICKS = 13 * SAMPLES * (SETUP_LIMIT + PULSE_LIMIT + 8) + 32;
    public enum Status { PASS, ASSERTION_FAILURE, INVALID_SETUP, INCOMPLETE }
    public record Sample(String species, int seed) {
        public long growthSeed() { return 12345 + 104729L * seed; }
        public long worldSeed() { return 54321 + 104729L * seed; }
    }
    public record Outcome(Sample sample, Status status, int pulses, int radius, int endpoint,
                          long startTick, long admissionTick, long endTick, String reason) {}
    public record Mean(String species, int validSamples, Double measured, double reference,
                       Double deviation, Long deviationNumerator, long deviationDenominator, boolean pass) {}
    public interface Backend {
        long tick();
        void schedule(Runnable callback);
        void prepare(Sample sample);
        boolean chunksReady();
        void construct(Sample sample);
        boolean favorable();
        void admit(Sample sample);
        int pulse(int pulse);
        int endpoint();
        double reference(String species);
        void cleanup(Sample sample);
        void outcome(Outcome outcome);
        void mean(Mean mean);
        void finish(boolean pass, boolean complete, Throwable original);
    }
    private enum Phase { PREPARE, CHUNKS, FAVORABLE, GROW, CLEANUP }
    private final Backend backend;
    private final List<String> species;
    private final List<Outcome> outcomes = new ArrayList<>();
    private Phase phase = Phase.PREPARE;
    private int index, pulses, radius, endpoint;
    private long start, admission = -1, previousTick = Long.MIN_VALUE;
    private boolean terminal, failed, prepared, started;
    private Outcome pending;
    private Throwable original;

    public GrowthFixtureController(Backend backend, List<String> species) {
        if (species.isEmpty() || species.size() > 13 || species.stream().distinct().count() != species.size())
            throw new IllegalArgumentException("Missing/duplicate species coverage");
        this.backend = backend;
        this.species = List.copyOf(species);
    }
    public void start() {
        if (started || terminal) throw new IllegalStateException("Controller already started/terminal");
        started = true;
        try { backend.schedule(this::callback); }
        catch (Throwable failure) { abort(Status.INCOMPLETE, failure); }
    }
    public void cancel(Throwable cause) { if (!terminal) abort(Status.INCOMPLETE, cause); }
    public List<Outcome> outcomes() { return List.copyOf(outcomes); }
    private Sample sample() { return new Sample(species.get(index / SAMPLES), index % SAMPLES); }
    private void remember(Throwable failure) {
        if (original == null) original = failure;
        else if (original != failure) original.addSuppressed(failure);
    }
    private void callback() {
        if (terminal) return; // A cancelled queued callback must never mutate its old world.
        try {
            long now = backend.tick();
            if ((phase == Phase.CHUNKS || phase == Phase.FAVORABLE) && now - start > SETUP_LIMIT) {
                abort(Status.INVALID_SETUP, new IllegalStateException("Setup exceeded 200 native ticks"));
                return;
            }
            if (now <= previousTick) throw new IllegalStateException("More than one callback per native tick");
            previousTick = now;
            switch (phase) {
                case PREPARE -> {
                    start = now; admission = -1; pulses = radius = endpoint = 0;
                    prepared = true; // Cleanup is attempted even if prepare partially throws.
                    backend.prepare(sample());
                    phase = Phase.CHUNKS;
                }
                case CHUNKS -> {
                    if (backend.chunksReady()) {
                        backend.construct(sample());
                        phase = Phase.FAVORABLE;
                    } else if (now - start >= SETUP_LIMIT) {
                        abort(Status.INVALID_SETUP, new IllegalStateException("Chunk readiness exceeded 200 native ticks"));
                        return;
                    }
                }
                case FAVORABLE -> {
                    if (backend.favorable()) {
                        endpoint = backend.endpoint();
                        if (endpoint <= 1) throw new IllegalStateException("Invalid native endpoint");
                        backend.admit(sample());
                        admission = now;
                        phase = Phase.GROW;
                    } else if (now - start >= SETUP_LIMIT) {
                        abort(Status.INVALID_SETUP, new IllegalStateException("Favorable setup exceeded 200 native ticks"));
                        return;
                    }
                }
                case GROW -> {
                    if (now != admission + pulses + 1) throw new IllegalStateException("Missed native pulse callback; no catch-up or hidden gaps");
                    radius = backend.pulse(++pulses); // Exactly one WHOLE native operation per callback.
                    if (radius == endpoint || pulses == PULSE_LIMIT) {
                        Status status = radius == endpoint ? Status.PASS : Status.ASSERTION_FAILURE;
                        pending = new Outcome(sample(), status, pulses, radius, endpoint, start, admission, now,
                            status == Status.PASS ? "NATIVE_MAX_TRUNK_RADIUS" : "PULSE_LIMIT_NATIVE_ENDPOINT_NOT_REACHED");
                        if (status != Status.PASS) { failed = true; remember(new AssertionError(pending.reason())); }
                        phase = Phase.CLEANUP;
                    }
                }
                case CLEANUP -> {
                    prepared = false;
                    backend.cleanup(sample());
                    emit(pending);
                    index++;
                    if (index % SAMPLES == 0) aggregate(species.get(index / SAMPLES - 1));
                    if (index == species.size() * SAMPLES) { finish(true); return; }
                    phase = Phase.PREPARE;
                }
            }
            backend.schedule(this::callback); // No synchronous successor or catch-up loop.
        } catch (Throwable failure) {
            if (terminal) {
                if (failure instanceof RuntimeException runtime) throw runtime;
                if (failure instanceof Error error) throw error;
                throw new IllegalStateException(failure);
            }
            abort(Status.INCOMPLETE, failure);
        }
    }
    private void emit(Outcome outcome) { backend.outcome(outcome); outcomes.add(outcome); }
    private void aggregate(String fruit) {
        var samples = outcomes.stream().filter(x -> x.sample().species().equals(fruit)).toList();
        int valid = (int) samples.stream().filter(x -> x.status() == Status.PASS).count();
        double reference = backend.reference(fruit);
        if (!(reference > 0) || !Double.isFinite(reference)) throw new IllegalStateException("Invalid reference");
        long referenceEighths = Math.round(reference * 8);
        if (referenceEighths / 8.0 != reference) throw new IllegalStateException("Reference precision drift");
        long sum = samples.stream().mapToInt(Outcome::pulses).sum();
        Double mean = valid == SAMPLES ? sum / 8.0 : null;
        Long numerator = mean == null ? null : Math.abs(sum - referenceEighths);
        Double deviation = numerator == null ? null : (double)numerator / referenceEighths;
        boolean pass = numerator != null && numerator * 5 < referenceEighths;
        backend.mean(new Mean(fruit, valid, mean, reference, deviation, numerator, referenceEighths, pass));
        if (!pass) { failed = true; remember(new AssertionError("V2 native calibration drifted " + fruit)); }
    }
    private void abort(Status status, Throwable failure) {
        if (terminal) return;
        failed = true; remember(failure);
        if (prepared) {
            try { backend.cleanup(sample()); }
            catch (Throwable cleanup) { remember(cleanup); }
            prepared = false;
        }
        // No contaminated continuation. Remaining rows are explicit NOT_MEASURED, never placeholders
        // in a mean. Preserve the first failure and attach cleanup/report failures as suppressed.
        int first = index;
        for (; index < species.size() * SAMPLES; index++) {
            try {
                emit(new Outcome(sample(), index == first ? status : Status.INCOMPLETE,
                    pulses, radius, endpoint, start, admission, backend.tick(),
                    index == first ? failure.toString() : "NOT_MEASURED_AFTER_CONTAMINATION"));
            } catch (Throwable reporting) { remember(reporting); break; }
            status = Status.INCOMPLETE; pulses = radius = endpoint = 0; admission = -1;
        }
        finish(false);
    }
    private void finish(boolean complete) {
        if (terminal) return;
        terminal = true; // Set BEFORE external code can throw/re-enter.
        backend.finish(!failed && complete, complete, original);
    }
}
