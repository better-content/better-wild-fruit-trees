package com.bettercontent.betterwildfruittrees;

import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Per-root replacement cadence, owned/persisted by the native root block entity. */
public final class TreeFruitBudget {
    public static final String KEY = "better_wild_fruit_trees:production";
    private static final ThreadLocal<Account> ATTEMPT = new ThreadLocal<>();
    private record Account(BlockEntity root, CompoundTag data) {}
    private TreeFruitBudget() {}

    public static void attempt(ServerLevel level, BlockPos root, Runnable nativePlacement) {
        // Never load a missing chunk or permit an unaccounted replacement if its root is missing.
        if (!level.hasChunkAt(root)) return;
        var entity = level.getBlockEntity(root);
        // Protected species in the pinned integration have native species-bearing root entities.
        if (!(entity instanceof com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity)) return;
        var persistent = entity.getPersistentData();
        var data = persistent.getCompound(KEY);
        long now = level.getGameTime();
        long credit = data.contains("lastTick")
                ? ProductionRates.accrue(data.getLong("credit"), data.getLong("lastTick"), now)
                : Math.max(0, Math.min(ProductionRates.MAX_CREDIT, data.getLong("credit")));
        data.putLong("credit", credit);
        data.putLong("lastTick", now);
        persistent.put(KEY, data);
        entity.setChanged();
        if (credit < ProductionRates.FRUIT_INTERVAL) return;
        var previous = ATTEMPT.get();
        ATTEMPT.set(new Account(entity, data));
        try {
            nativePlacement.run();
        } finally {
            if (previous == null) ATTEMPT.remove(); else ATTEMPT.set(previous);
        }
    }

    /** Called only at the native successful-candidate placement site, after native geometry checks. */
    public static void place(Fruit fruit, LevelAccessor level, BlockPos pos, Float season) {
        var account = ATTEMPT.get();
        boolean existed = level.getBlockState(pos).is(fruit.getBlock());
        boolean reserved = account != null && !existed;
        if (reserved) {
            long credit = account.data.getLong("credit");
            if (credit < ProductionRates.FRUIT_INTERVAL) return;
            account.data.putLong("credit", credit - ProductionRates.FRUIT_INTERVAL);
        }
        try {
            fruit.place(level, pos, season);
        } finally {
            if (reserved) {
                if (!level.getBlockState(pos).is(fruit.getBlock()))
                    account.data.putLong("credit", Math.min(ProductionRates.MAX_CREDIT,
                            account.data.getLong("credit") + ProductionRates.FRUIT_INTERVAL));
                account.root.setChanged();
            }
        }
    }

    /** Native data/level reattachment must not accrue unloaded/restart time. */
    public static void resume(BlockEntity entity) {
        if (!(entity instanceof com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity)) return;
        var persistent = entity.getPersistentData();
        if (persistent.contains(KEY)) {
            persistent.getCompound(KEY).remove("lastTick");
            entity.setChanged();
        }
    }
}
