package com.bettercontent.betterwildfruittrees.gametest;

/** Pure identity oracle for an already-observed native TICKING future; never waits or loads. */
public final class GrowthTickingReadiness {
    private GrowthTickingReadiness() {}
    public static boolean ready(boolean done, boolean exceptional, boolean cancelled,
                                Object leftChunk, Object currentChunk, Object chunkWorld, Object world,
                                long chunkKey, long expectedKey, Object capturedHolder, Object freshHolder) {
        if (exceptional || cancelled) throw new IllegalStateException("Native TICKING future failed");
        return done && leftChunk != null && leftChunk == currentChunk && chunkWorld == world &&
            chunkKey == expectedKey && capturedHolder != null && capturedHolder == freshHolder;
    }
}
