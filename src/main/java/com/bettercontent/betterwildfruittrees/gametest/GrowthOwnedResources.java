package com.bettercontent.betterwildfruittrees.gametest;

import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

/** Attempt every owned release independently. Failed leases remain dirty; never touch foreign ones. */
public final class GrowthOwnedResources {
    private GrowthOwnedResources() {}
    public static <T> Throwable releaseAll(Collection<T> owned, Consumer<T> release) {
        Throwable first = null;
        for (T item : List.copyOf(owned)) {
            try { release.accept(item); owned.remove(item); }
            catch (Throwable failure) { if (first == null) first = failure; else if (first != failure) first.addSuppressed(failure); }
        }
        return first;
    }
    /** Release new reentrant leases too, but never retry a failed lease in this cleanup call. */
    public static <T> Throwable drain(Collection<T> owned, Consumer<T> release) {
        Throwable first = null;
        var attempted = new HashSet<T>();
        while (true) {
            var pending = List.copyOf(owned).stream().filter(x -> !attempted.contains(x)).toList();
            if (pending.isEmpty()) return first;
            for (T item : pending) {
                if (attempted.size() >= 4096) {
                    var flood = new IllegalStateException("Owned entity release cascade exceeded 4096 distinct leases");
                    if (first == null) return flood;
                    first.addSuppressed(flood); return first;
                }
                attempted.add(item);
                try { release.accept(item); owned.remove(item); }
                catch (Throwable failure) { if (first == null) first = failure; else if (first != failure) first.addSuppressed(failure); }
            }
        }
    }
}
