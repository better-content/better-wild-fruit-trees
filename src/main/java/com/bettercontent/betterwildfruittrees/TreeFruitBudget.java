package com.bettercontent.betterwildfruittrees;

import com.bettercontent.betterwildfruittrees.compat.RootLedger;
import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Success-only placement accounting; legacy root entities and optional dimension records share the same cadence. */
public final class TreeFruitBudget {
    public static final String KEY = "better_wild_fruit_trees:production";
    private static final ThreadLocal<Account> ATTEMPT = new ThreadLocal<>();
    private record Account(BlockEntity root, CompoundTag data, RootLedger ledger, RootLedger.Record record) {
        long credit() { return ledger == null ? data.getLong("credit") : record.credit; }
        void debit() {
            if (ledger == null) data.putLong("credit", credit() - ProductionRates.FRUIT_INTERVAL);
            else ledger.debit(record);
        }
        void refund() {
            if (ledger == null) data.putLong("credit", Math.min(ProductionRates.MAX_CREDIT, credit() + ProductionRates.FRUIT_INTERVAL));
            else ledger.refund(record);
        }
        void dirty() { if (root != null) root.setChanged(); }
    }
    private TreeFruitBudget() {}

    public static void attempt(ServerLevel level, BlockPos root, Runnable nativePlacement) {
        if (!level.hasChunkAt(root)) return;
        Account account;
        String species = RootLedger.speciesAt(level, root);
        if (WildFruitPolicy.savedRoot(species)) {
            var ledger = RootLedger.get(level);
            if (!ledger.reserve(level, root)) return;
            account = new Account(null, null, ledger, ledger.record(level, root));
        } else {
            var entity = level.getBlockEntity(root);
            if (!(entity instanceof com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity)) return;
            var persistent = entity.getPersistentData();
            var data = persistent.getCompound(KEY);
            long now = level.getGameTime();
            long credit = data.contains("lastTick")
                ? ProductionRates.accrue(data.getLong("credit"), data.getLong("lastTick"), now)
                : Math.max(0, Math.min(ProductionRates.MAX_CREDIT, data.getLong("credit")));
            data.putLong("credit", credit); data.putLong("lastTick", now);
            persistent.put(KEY, data); entity.setChanged();
            if (credit < ProductionRates.FRUIT_INTERVAL) return;
            account = new Account(entity, data, null, null);
        }
        var previous = ATTEMPT.get();
        ATTEMPT.set(account);
        try { nativePlacement.run(); }
        finally { if (previous == null) ATTEMPT.remove(); else ATTEMPT.set(previous); }
    }
    public static void place(Fruit fruit, LevelAccessor level, BlockPos pos, Float season) {
        placeBlock(fruit.getBlock(), level, pos, () -> fruit.place(level, pos, season));
    }
    /** Called at the native candidate placement site, after native geometry/season checks. */
    public static void placeBlock(Block product, LevelAccessor level, BlockPos pos, Runnable nativePlacement) {
        var account = ATTEMPT.get();
        boolean reserved = account != null && !level.getBlockState(pos).is(product);
        if (reserved) {
            if (account.credit() < ProductionRates.FRUIT_INTERVAL) return;
            account.debit();
        }
        try { nativePlacement.run(); }
        finally {
            if (reserved) {
                if (!level.getBlockState(pos).is(product)) account.refund();
                account.dirty();
            }
        }
    }
    public static void resume(BlockEntity entity) {
        if (!(entity instanceof com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity)) return;
        var persistent = entity.getPersistentData();
        if (persistent.contains(KEY)) { persistent.getCompound(KEY).remove("lastTick"); entity.setChanged(); }
    }
}
