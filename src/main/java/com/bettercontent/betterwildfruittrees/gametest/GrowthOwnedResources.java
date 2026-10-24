package com.bettercontent.betterwildfruittrees.gametest;

import java.util.List;
import java.util.function.Consumer;

/** Attempt every owned release independently. Failed leases remain dirty; never touch foreign ones. */
public final class GrowthOwnedResources {
    private GrowthOwnedResources() {}
    public static <T> Throwable releaseAll(List<T> owned, Consumer<T> release) {
        Throwable first = null;
        for (T item : List.copyOf(owned)) {
            try { release.accept(item); owned.remove(item); }
            catch (Throwable failure) { if (first == null) first = failure; else if (first != failure) first.addSuppressed(failure); }
        }
        return first;
    }
}
