package com.bettercontent.betterwildfruittrees.gametest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

/** Nonshipping observation only. Never accepts/rejects calibration or retains Minecraft objects. */
public final class OriginalLightTrace {
    public static final int SKY = 0, CAN_SEE = 1, BRIGHTNESS = 2, ADEQUATE = 3, WRITE = 4;
    private static final int PULSES = 4096, STORAGES = 16, TAIL = 64, EVENT_LIMIT = 1_000_000;
    private static volatile Session active;
    private static volatile boolean stateCodesReady;
    public static void stateCodesReady(boolean complete) { stateCodesReady = complete; }
    private OriginalLightTrace() {}

    public static boolean ownerActive() {
        Session s = active;
        return s != null && s.owner == Thread.currentThread().getId();
    }
    public static boolean publicationActive() { return active != null; }

    public static void begin(String fruit, int sample) {
        // Only the original eight orange samples. No world/registry/codec references.
        if (fruit.equals("orange") && sample >= 0 && sample < 8) active = new Session(sample);
    }
    public static void pulse(int pulse) {
        Session s = active;
        if (s == null || s.owner != Thread.currentThread().getId()) return;
        if (pulse < 1 || pulse > PULSES) { s.overflow.set(true); return; }
        s.pulse = pulse;
    }
    public static void light(int kind, long pos, int value) {
        Session s = active;
        if (s == null || s.owner != Thread.currentThread().getId()) return;
        s.record(kind, pos, value, 0, 0, 0, 0);
    }
    public static void write(long pos, int requestedStateIdentity, int returnedStateIdentity, boolean changed) {
        Session s = active;
        if (s == null || s.owner != Thread.currentThread().getId()) return;
        s.record(WRITE, pos, changed ? 1 : 0, requestedStateIdentity, returnedStateIdentity, 0, 0);
    }
    public static void canonicalWrite(long pos, long requestedCode, long returnedCode) {
        Session s = active;
        if (s == null || s.owner != Thread.currentThread().getId()) return;
        int p = s.pulse;
        if (p < 1 || p > PULSES || s.canonicalCounts[p] >= EVENT_LIMIT) { s.overflow.set(true); return; }
        int index = ++s.canonicalCounts[p];
        if (requestedCode == Long.MIN_VALUE || returnedCode == Long.MIN_VALUE) {
            s.canonicalIncomplete = true; s.canonicalUnknown[p]++;
        }
        s.canonicalHashes[p] = mix(mix(mix(s.canonicalHashes[p], pos), requestedCode), returnedCode);
        long[] entry = s.canonicalTail[(int)(s.canonicalTailCount++ % TAIL)];
        entry[0] = p; entry[1] = index; entry[2] = pos; entry[3] = requestedCode; entry[4] = returnedCode;
    }
    public static void skyEnter(int storageIdentity) {
        Session s = active;
        if (s == null || s.owner != Thread.currentThread().getId()) return;
        if (s.skyDepth++ != 0) { s.overflow.set(true); return; }
        s.skyId = storageIdentity;
        s.skySlot = s.storage(storageIdentity);
        s.skyBefore = s.skySlot < 0 ? -1 : s.versions.get(s.skySlot);
    }
    public static void skyReturn(int storageIdentity, long pos, int value) {
        Session s = active;
        if (s == null || s.owner != Thread.currentThread().getId()) return;
        if (--s.skyDepth != 0 || storageIdentity != s.skyId) { s.overflow.set(true); return; }
        long after = s.skySlot < 0 ? -1 : s.versions.get(s.skySlot);
        s.record(SKY, pos, value, storageIdentity, s.skySlot, s.skyBefore, after);
    }
    public static void published(int storageIdentity) {
        Session s = active;
        if (s == null) return;
        int slot = s.storage(storageIdentity);
        if (slot < 0) return;
        long version = s.versions.get(slot);
        if (version >= EVENT_LIMIT || !s.versions.compareAndSet(slot, version, version + 1)) {
            s.overflow.set(true); return; // Bounded single attempt; never wait for another observer.
        }
        version++;
        int pulse = s.pulse;
        if (pulse >= 1 && pulse <= PULSES) s.publicationCounts.incrementAndGet(pulse);
        long index = s.publications.getAndIncrement();
        long sequence = s.sequence.incrementAndGet();
        // Immutable scalar record; no monitor, barrier, field replacement or engine access.
        s.publicationTail.set((int)(index % TAIL), new Publication(sequence, pulse, storageIdentity, slot, version));
    }
    public static void finish() {
        Session s = active;
        if (s == null || s.owner != Thread.currentThread().getId()) return;
        active = null; // No wait for the light worker: this is a detached diagnostic snapshot, NOT a light barrier.
        if (s.skyDepth != 0) s.overflow.set(true);
        s.print();
    }

    private record Publication(long sequence, int pulse, int identity, int slot, long version) {}
    private static final class Session {
        final long owner = Thread.currentThread().getId();
        final int sample;
        volatile int pulse;
        final AtomicBoolean overflow = new AtomicBoolean();
        final AtomicLong sequence = new AtomicLong(), publications = new AtomicLong();
        final AtomicLongArray storageIds = new AtomicLongArray(STORAGES), versions = new AtomicLongArray(STORAGES);
        final AtomicLongArray publicationCounts = new AtomicLongArray(PULSES + 1);
        final AtomicReferenceArray<Publication> publicationTail = new AtomicReferenceArray<>(TAIL);
        final int[][] counts = new int[PULSES + 1][5];
        final long[][] hashes = new long[PULSES + 1][5];
        final long[][] tail = new long[TAIL][9];
        final int[] minima = new int[PULSES + 1];
        final long[] firstLowPos = new long[PULSES + 1];
        final long[] skyPositionHashes = new long[PULSES + 1], canonicalHashes = new long[PULSES + 1];
        final int[][] skyHistograms = new int[PULSES + 1][16];
        final int[] canonicalCounts = new int[PULSES + 1], canonicalUnknown = new int[PULSES + 1];
        final long[][] canonicalTail = new long[TAIL][5];
        long canonicalTailCount;
        boolean canonicalIncomplete = !stateCodesReady;
        int skyDepth, skyId, skySlot;
        long tailCount, skyBefore;
        Session(int sample) { this.sample = sample; java.util.Arrays.fill(minima, 16); }
        int storage(int identity) {
            long scalar = Integer.toUnsignedLong(identity) + 1;
            for (int i = 0; i < STORAGES; i++) {
                long found = storageIds.get(i);
                if (found == scalar) return i;
                if (found == 0 && (storageIds.compareAndSet(i, 0, scalar) || storageIds.get(i) == scalar)) return i;
            }
            overflow.set(true);
            return -1;
        }
        void record(int kind, long pos, int value, int identity, int detail, long before, long after) {
            int p = pulse;
            if (p < 1 || p > PULSES || kind < 0 || kind >= 5) { overflow.set(true); return; }
            if (counts[p][kind] >= EVENT_LIMIT) { overflow.set(true); return; }
            counts[p][kind]++;
            // Stable across runs: excludes identity hashes and worker publication ordering.
            hashes[p][kind] = kind == WRITE ? mix(hashes[p][kind], pos)
                : mix(mix(hashes[p][kind], pos), value);
            if (kind == SKY) {
                skyPositionHashes[p] = mix(skyPositionHashes[p], pos);
                if (value >= 0 && value < 16) skyHistograms[p][value]++;
                else overflow.set(true);
            }
            if (kind == SKY && value < minima[p]) { minima[p] = value; firstLowPos[p] = pos; }
            long[] entry = tail[(int)(tailCount++ % TAIL)];
            entry[0] = sequence.incrementAndGet(); entry[1] = p; entry[2] = kind;
            entry[3] = pos; entry[4] = value; entry[5] = identity; entry[6] = detail;
            entry[7] = before; entry[8] = after;
        }
        void print() {
            System.out.println("ORIGINAL_LIGHT_TRACE sample=" + sample + " pulses=" + pulse
                + " diagnosticOnly=true overflow=" + overflow.get() + " canonicalIncomplete=" + canonicalIncomplete
                + " detachedNoBarrier=true identityHashesMayCollide=true publicationVersion=postPutfieldObservationNotMapGeneration");
            for (int p = 1; p <= pulse; p++) {
                System.out.println("ORIGINAL_LIGHT_PULSE sample=" + sample + " pulse=" + p
                    + " countsSkyCanSeeBrightnessAdequateWrite=" + java.util.Arrays.toString(counts[p])
                    + " stableHashes=" + java.util.Arrays.toString(hashes[p])
                    + " observedPublications=" + publicationCounts.get(p) + " minSky=" + (minima[p] == 16 ? "unobserved" : minima[p]) + " minSkyPos=" + firstLowPos[p]);
            }
            for (long i = Math.max(0, tailCount - TAIL); i < tailCount; i++)
                System.out.println("ORIGINAL_LIGHT_TAIL sample=" + sample + " seqPulseKindPosValueIdentityDetailVersionBeforeAfter="
                    + java.util.Arrays.toString(tail[(int)(i % TAIL)]));
            for (int p = 1; p <= pulse; p++)
                System.out.println("ORIGINAL_LIGHT_REFINED sample=" + sample + " pulse=" + p
                    + " skyPositionHash=" + skyPositionHashes[p] + " skyIntensityHistogram=" + java.util.Arrays.toString(skyHistograms[p])
                    + " canonicalWriteCount=" + canonicalCounts[p] + " canonicalWriteHash=" + canonicalHashes[p]
                    + " canonicalUnknown=" + canonicalUnknown[p] + " canonicalIncomplete=" + canonicalIncomplete);
            for (long i = Math.max(0, canonicalTailCount - TAIL); i < canonicalTailCount; i++)
                System.out.println("ORIGINAL_CANONICAL_WRITE sample=" + sample + " pulseIndexPosRequestedReturned="
                    + java.util.Arrays.toString(canonicalTail[(int)(i % TAIL)]));
            for (int i = 0; i < STORAGES; i++) if (storageIds.get(i) != 0)
                System.out.println("ORIGINAL_LIGHT_STORAGE sample=" + sample + " slot=" + i
                    + " identity=" + (int)(storageIds.get(i) - 1) + " observedPublications=" + versions.get(i));
            for (int i = 0; i < TAIL; i++) {
                Publication publication = publicationTail.get(i);
                if (publication != null) System.out.println("ORIGINAL_LIGHT_PUBLICATION sample=" + sample + " " + publication);
            }
            System.out.println("ORIGINAL_LIGHT_END sample=" + sample + " diagnosticOnly=true overflow=" + overflow.get() + " canonicalIncomplete=" + canonicalIncomplete);
        }
    }
    private static long mix(long hash, long value) { return (hash ^ value) * 0x100000001b3L; }
}
