package com.bettercontent.betterwildfruittrees.gametest;

/** Ordered, synchronous fixture-owned cleanup. No ticks, waits, foreign disposal or failure swallowing. */
public final class GrowthPlotCleanup {
    private GrowthPlotCleanup() {}
    public interface Port {
        boolean plotOwned();
        void checkForeignEntities();
        void beforeClear();
        void clearBlocks();
        void clearScheduledTicks();
        void drainOwnedEntities();
        void verifyPlotAndEntitiesEmpty();
        void markPlotReleased();
        void releaseTickets();
        void verifyResourcesReleased();
    }
    public static Throwable run(Port port, Throwable original) {
        Throwable failure = attempt(original, port::checkForeignEntities);
        if (port.plotOwned()) {
            failure = attempt(failure, port::beforeClear);
            failure = attempt(failure, port::clearBlocks);
            failure = attempt(failure, port::clearScheduledTicks);
        }
        // Native block clears can join new owned entities. Drain AFTER clears, and also when
        // an earlier partial cleanup released the plot but left owned entity leases behind.
        failure = attempt(failure, port::drainOwnedEntities);
        try { port.verifyPlotAndEntitiesEmpty(); port.markPlotReleased(); }
        catch (Throwable dirty) { failure = merge(failure, dirty); }
        failure = attempt(failure, port::releaseTickets);
        return attempt(failure, port::verifyResourcesReleased);
    }
    private static Throwable attempt(Throwable failure, Runnable action) {
        try { action.run(); return failure; }
        catch (Throwable next) { return merge(failure, next); }
    }
    private static Throwable merge(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }
}
