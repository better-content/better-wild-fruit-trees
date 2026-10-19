package com.bettercontent.betterwildfruittrees;

import com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Root-owned maturation; vanilla chunk inhabited time advances with native active/forced ticking. */
public final class TreeMaturation {
    public static final String KEY = "better_wild_fruit_trees:maturation";
    private static final String CLOCK = "lastInhabitedTick";
    private TreeMaturation() {}

    private static SpeciesBlockEntity root(LevelAccessor level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        return level.getBlockEntity(pos) instanceof SpeciesBlockEntity entity ? entity : null;
    }

    /** Called only after successful protected-species root placement; never rewrites tree blocks. */
    public static void start(LevelAccessor level, BlockPos pos) {
        if (level.isClientSide()) return;
        var entity = root(level, pos);
        if (entity == null || entity.getPersistentData().contains(KEY)) return;
        var data = new CompoundTag();
        data.putLong("age", 0);
        entity.getPersistentData().put(KEY, data);
        entity.setChanged();
    }

    /** Native JoCodes finish with exhausted soil. Supply new protected trees once, after all
     * generation/initial fruit placement, so their additional growth can actually occur. */
    public static void finishGeneration(LevelAccessor level, BlockPos pos) {
        if (level.isClientSide()) return;
        var entity = root(level, pos);
        if (entity == null || !entity.getPersistentData().contains(KEY)) return;
        var data = entity.getPersistentData().getCompound(KEY);
        if (data.getBoolean("worldgenEstablished")) return;
        data.putBoolean("worldgenEstablished", true);
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof com.ferreusveritas.dynamictrees.block.rooty.RootyBlock
                && state.getValue(com.ferreusveritas.dynamictrees.block.rooty.RootyBlock.FERTILITY) == 0)
            level.setBlock(pos, state.setValue(com.ferreusveritas.dynamictrees.block.rooty.RootyBlock.FERTILITY, 15), 2);
        entity.setChanged();
    }

    public static long observe(LevelAccessor level, BlockPos pos) {
        if (level.isClientSide()) return peek(level, pos);
        var entity = root(level, pos);
        if (entity == null) return 0; // No untracked early enlargement if a protected root is missing.
        var persistent = entity.getPersistentData();
        if (!persistent.contains(KEY)) return TreeGrowthSchedule.DURATION; // Legacy roots keep full potential.
        var data = persistent.getCompound(KEY);
        long age = TreeGrowthSchedule.bounded(data.getLong("age"));
        if (age == TreeGrowthSchedule.DURATION) return age;
        // Loaded-only lookup: inhabited time is persisted by vanilla and does not advance in
        // unloaded/non-active chunks. Calendar skips, dayTime commands and repeated bonemeal
        // calls in the same chunk tick cannot award extra maturation.
        long now = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getInhabitedTime();
        long updated = data.contains(CLOCK) ? TreeGrowthSchedule.advance(age, data.getLong(CLOCK), now) : age;
        boolean changed = !data.contains(CLOCK) || data.getLong(CLOCK) != now || data.getLong("age") != updated;
        if (changed) {
            data.putLong("age", updated);
            data.putLong(CLOCK, now);
            entity.setChanged();
        }
        return updated;
    }

    /** Branch traversal reads one already-observed age, without dirtying the root per branch. */
    public static long peek(LevelAccessor level, BlockPos pos) {
        var entity = root(level, pos);
        if (entity == null) return 0;
        var persistent = entity.getPersistentData();
        return persistent.contains(KEY) ? TreeGrowthSchedule.bounded(persistent.getCompound(KEY).getLong("age"))
                : TreeGrowthSchedule.DURATION;
    }

    public static void resume(BlockEntity entity) {
        if (!(entity instanceof SpeciesBlockEntity)) return;
        var persistent = entity.getPersistentData();
        if (persistent.contains(KEY) && persistent.getCompound(KEY).contains(CLOCK)) {
            // Rebase on the first native update after reattachment, including restart/force-load
            // startup. Retain earned age, but never interpret a native clock jump as offline growth.
            persistent.getCompound(KEY).remove(CLOCK);
            entity.setChanged();
        }
    }
}
