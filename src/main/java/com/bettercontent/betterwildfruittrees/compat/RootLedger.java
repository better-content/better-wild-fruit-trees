package com.bettercontent.betterwildfruittrees.compat;

import com.bettercontent.betterwildfruittrees.ProductionRates;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;

/** Dimension-owned records for optional resources. Runtime clocks are deliberately not serialized. */
public final class RootLedger extends SavedData {
    private static final String NAME = "better_wild_fruit_trees_roots";
    public static final int SCHEMA = 1;
    public static final class Record {
        public final String source;
        public final String species;
        public long credit;
        long lastTick = Long.MIN_VALUE;
        Record(String source, String species, long credit) {
            this.source = source; this.species = species;
            this.credit = Math.max(0, Math.min(ProductionRates.MAX_CREDIT, credit));
        }
    }
    private final Map<Long, Record> roots = new HashMap<>();
    private CompoundTag unsupported;
    public void requireSupported() {
        if (unsupported != null) throw new IllegalStateException("Unsupported wild fruit root schema; preserve the saved data and resolve its version before continuing");
    }
    public static RootLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RootLedger::load, RootLedger::new, NAME);
    }
    public static RootLedger load(CompoundTag tag) {
        var result = new RootLedger();
        // DimensionDataStorage catches deserializer exceptions and can replace failed loads with empty
        // data. Preserve unknown bytes here and reject use outside that catch instead of losing origin.
        if (tag.getInt("schema") != SCHEMA) { result.unsupported = tag.copy(); return result; }
        for (var entry : tag.getList("roots", 10)) {
            var row = (CompoundTag) entry;
            String source = row.getString("source"), species = row.getString("species");
            if (WildFruitPolicy.savedRoot(source) && (source.equals(species) ||
                    source.equals(WildFruitPolicy.SAGUARO) && species.equals(WildFruitPolicy.MEGA_CACTUS)))
                result.roots.put(row.getLong("pos"), new Record(source, species, row.getLong("credit")));
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        if (unsupported != null) return unsupported.copy();
        tag.putInt("schema", SCHEMA);
        var list = new ListTag();
        roots.forEach((pos, record) -> {
            var row = new CompoundTag();
            row.putLong("pos", pos); row.putString("source", record.source); row.putString("species", record.species);
            row.putLong("credit", record.credit); list.add(row);
        });
        tag.put("roots", list);
        return tag;
    }
    public static String speciesAt(Level level, BlockPos root) {
        if (!level.hasChunkAt(root)) return "";
        var state = level.getBlockState(root);
        return state.getBlock() instanceof RootyBlock rooty
            ? rooty.getSpecies(state, level, root).getRegistryName().toString() : "";
    }
    public Record record(ServerLevel level, BlockPos root) {
        requireSupported();
        if (!level.hasChunkAt(root)) return null;
        String species = speciesAt(level, root);
        var record = roots.get(root.asLong());
        if (record != null && !record.species.equals(species)) {
            remove(level, root); record = null;
        }
        if (record == null && WildFruitPolicy.savedRoot(species)) {
            record = new Record(species, species, 0);
            roots.put(root.asLong(), record); setDirty();
        }
        return record;
    }
    public static boolean inherited(Level level, BlockPos root, String species) {
        if (!WildFruitPolicy.MEGA_CACTUS.equals(species)) return false;
        if (level instanceof ServerLevel server) {
            var record = get(server).record(server, root);
            return record != null && record.source.equals(WildFruitPolicy.SAGUARO);
        }
        return LedgerNetwork.inherited(level, root);
    }
    public void converted(ServerLevel level, BlockPos root) {
        requireSupported();
        if (!speciesAt(level, root).equals(WildFruitPolicy.MEGA_CACTUS)) return;
        roots.put(root.asLong(), new Record(WildFruitPolicy.SAGUARO, WildFruitPolicy.MEGA_CACTUS, 0));
        setDirty(); LedgerNetwork.changed(level, root, true);
    }
    public void remove(ServerLevel level, BlockPos root) {
        requireSupported();
        var old = roots.remove(root.asLong());
        if (old != null) {
            setDirty();
            if (old.species.equals(WildFruitPolicy.MEGA_CACTUS)) LedgerNetwork.changed(level, root, false);
        }
    }
    public void suspendChunk(int x, int z) {
        roots.forEach((pos, record) -> {
            var root = BlockPos.of(pos);
            if ((root.getX() >> 4) == x && (root.getZ() >> 4) == z) record.lastTick = Long.MIN_VALUE;
        });
    }
    public java.util.List<BlockPos> inheritedInChunk(int x, int z) {
        return roots.entrySet().stream().filter(e -> e.getValue().species.equals(WildFruitPolicy.MEGA_CACTUS))
            .map(e -> BlockPos.of(e.getKey())).filter(p -> (p.getX() >> 4) == x && (p.getZ() >> 4) == z).toList();
    }
    /** Credit is earned only while this chunk remains attached, and only native successful placement spends it. */
    public boolean reserve(ServerLevel level, BlockPos root) {
        var record = record(level, root);
        if (record == null || record.species.equals(WildFruitPolicy.MEGA_CACTUS)) return false;
        long now = level.getGameTime();
        if (record.lastTick != Long.MIN_VALUE) record.credit = ProductionRates.accrue(record.credit, record.lastTick, now);
        record.lastTick = now; setDirty();
        return record.credit >= ProductionRates.FRUIT_INTERVAL;
    }
    public void debit(Record record) { record.credit -= ProductionRates.FRUIT_INTERVAL; setDirty(); }
    public void refund(Record record) { record.credit = Math.min(ProductionRates.MAX_CREDIT, record.credit + ProductionRates.FRUIT_INTERVAL); setDirty(); }
}
