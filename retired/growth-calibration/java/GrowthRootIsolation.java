package com.bettercontent.betterwildfruittrees.gametest;

/** Exact v2 manual-pulse root lease. No Minecraft API, no waits, no leaf/global suppression. */
public final class GrowthRootIsolation {
    private final Object world;
    private final long root;
    private final Thread thread;
    private Object block;
    private boolean admitted;
    private long blocked, forwarded, returned;
    public GrowthRootIsolation(Object world, long root) { this.world = world; this.root = root; thread = Thread.currentThread(); }
    public void admit(Object expectedRootBlock) {
        if (admitted || expectedRootBlock == null || thread != Thread.currentThread()) throw new IllegalStateException("Invalid root admission");
        block = expectedRootBlock; admitted = true;
    }
    public void disarm() { admitted = false; block = null; }
    public boolean block(Object actualWorld, long actualPosition, Object actualBlock) {
        boolean match = admitted && actualWorld == world && actualPosition == root && actualBlock == block && Thread.currentThread() == thread;
        if (match) blocked++; else forwarded++;
        return match;
    }
    public void returned() { returned++; }
    public long blocked() { return blocked; }
    public long forwarded() { return forwarded; }
    public long returns() { return returned; }
    public boolean admitted() { return admitted; }
}
