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
    private record Account(BlockEntity root, CompoundTag data, RootLedger ledger, RootLedger.Record record,
        com.bettercontent.betterwildfruittrees.diagnostics.FruitBatchObservation batch) {
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
        if (!level.hasChunkAt(root)) {
            com.bettercontent.betterwildfruittrees.diagnostics.RuntimeDiagnostics.event("production", "fruit.attempt", "deferred", "unloaded_root"); return;
        }
        Account account;
        String species = RootLedger.speciesAt(level, root);
        if (WildFruitPolicy.savedRoot(species)) {
            var ledger = RootLedger.get(level);
            if (!ledger.reserve(level, root)) {
                com.bettercontent.betterwildfruittrees.diagnostics.RuntimeDiagnostics.event("production", "fruit.attempt", "deferred", "ledger_allowance_or_schema"); return;
            }
            account = new Account(null, null, ledger, ledger.record(level, root), null);
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
            if (credit < ProductionRates.FRUIT_INTERVAL) {
                com.bettercontent.betterwildfruittrees.diagnostics.RuntimeDiagnostics.event("production", "fruit.attempt", "deferred", "insufficient_credit"); return;
            }
            account = new Account(entity, data, null, null, null);
        }
        if (com.bettercontent.betterwildfruittrees.diagnostics.RuntimeDiagnostics.LOG.enabled("production")) {
            // Existing native budget root, not a player/cell location or a new persistent tree ID.
            // Snapshot only scalar correlation after admission; retain no level/root object in diagnostics.
            var context = new com.bettercontent.betterwildfruittrees.diagnostics.PlaytestLog.Context(
                "server", level.dimension().location().toString(), level.getGameTime(), "",
                root.getX() + "," + root.getY() + "," + root.getZ());
            account = new Account(account.root(), account.data(), account.ledger(), account.record(),
                new com.bettercontent.betterwildfruittrees.diagnostics.FruitBatchObservation(context, account.credit(), account.ledger() != null));
        }
        var previous = ATTEMPT.get();
        ATTEMPT.set(account);
        boolean completed = false;
        try { nativePlacement.run(); completed = true; }
        finally {
            if (previous == null) ATTEMPT.remove(); else ATTEMPT.set(previous);
            if (account.batch() != null) account.batch().finish(
                com.bettercontent.betterwildfruittrees.diagnostics.RuntimeDiagnostics.LOG, completed, account.credit());
        }
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
            if (account.batch() != null) account.batch().reserved();
        }
        try { nativePlacement.run(); }
        finally {
            if (reserved) {
                boolean placed = level.getBlockState(pos).is(product);
                if (!placed) account.refund();
                account.dirty();
                if (account.batch() != null) account.batch().settled(placed);
            }
        }
    }
    public static void resume(BlockEntity entity) {
        if (!(entity instanceof com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity)) return;
        var persistent = entity.getPersistentData();
        if (persistent.contains(KEY)) { persistent.getCompound(KEY).remove("lastTick"); entity.setChanged(); }
    }
}
