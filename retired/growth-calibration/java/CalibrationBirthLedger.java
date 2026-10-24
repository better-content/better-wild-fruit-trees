package com.bettercontent.betterwildfruittrees.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Nonshipping fixture-only scalar state, not native objects, gameplay persistence or a world scan. */
final class CalibrationBirthLedger {
    static final int LIMIT = 4096;
    enum State { JOIN_PENDING, VISIBLE, UNTRACKED, DESTROYED }
    private static final class Birth {
        final int x, y, z;
        final boolean cancellationObserved;
        State state = State.JOIN_PENDING;
        Birth(int x, int y, int z, boolean cancelled) {
            this.x = x; this.y = y; this.z = z; this.cancellationObserved = cancelled;
        }
    }
    private final LinkedHashMap<UUID, Birth> births = new LinkedHashMap<>();
    private String failure;
    private boolean levelLoaded;
    private boolean levelUnloaded;
    synchronized void fault(String reason) { if (failure == null) failure = reason; }
    synchronized String failure() { return failure; }
    synchronized void loaded() {
        if (levelLoaded || levelUnloaded) fault("Repeated fixture level lifecycle");
        levelLoaded = true;
    }
    synchronized void unloaded() { levelUnloaded = true; fault("Fixture level unloaded"); }
    synchronized boolean levelReady() { return levelLoaded && !levelUnloaded && failure == null; }
    synchronized void join(UUID id, int x, int y, int z, boolean cancelled) {
        if (failure != null) return;
        if (id == null) { fault("Missing fixture UUID"); return; }
        if (births.containsKey(id)) { fault("Duplicate/reinserted fixture UUID"); return; }
        if (births.size() >= LIMIT) { fault("Fixture birth identity capacity exhausted"); return; }
        births.put(id, new Birth(x, y, z, cancelled));
    }
    synchronized List<UUID> identities() { return List.copyOf(births.keySet()); }
    synchronized boolean knows(UUID id) { return births.containsKey(id); }
    synchronized boolean cutMatches(UUID id, int x, int y, int z) {
        var b = births.get(id);
        return b != null && b.x == x && b.y == y && b.z == z;
    }
    synchronized void lookupPresent(UUID id) {
        var b = births.get(id);
        if (b == null) { fault("Lookup for unknown fixture UUID"); return; }
        if (b.state == State.DESTROYED) { fault("Destroyed fixture UUID visible again"); return; }
        b.state = State.VISIBLE;
    }
    synchronized void lookupAbsent(UUID id) {
        if (!births.containsKey(id)) fault("Absent lookup for unknown fixture UUID");
        // Delayed insertion/cancellation/unload cannot be distinguished by null. NEVER clear it.
    }
    synchronized void leave(UUID id, boolean permanentDestroy) {
        var b = births.get(id);
        if (b == null) { fault("Removal for unobserved fixture birth"); return; }
        if (b.state == State.DESTROYED) { fault("Duplicate terminal fixture leave"); return; }
        b.state = permanentDestroy ? State.DESTROYED : State.UNTRACKED;
    }
    synchronized boolean quiescent() {
        return levelReady() && births.values().stream().allMatch(b -> b.state == State.DESTROYED);
    }
    synchronized void releaseRetired() {
        if (!quiescent()) throw new IllegalStateException("Unresolved fixture birth cohort");
        births.clear();
    }
}
