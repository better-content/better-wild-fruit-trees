package com.bettercontent.betterwildfruittrees.gametest;

import com.ferreusveritas.dynamictrees.api.cell.Cell;
import com.ferreusveritas.dynamictrees.api.network.MapSignal;
import com.ferreusveritas.dynamictrees.api.treedata.TreePart;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** Typed native pulse observer. Every unexpected query remains an assertion failure. */
final class OptionalCountingGrowthPart implements TreePart {
    int pulses;
    float energy = Float.NaN;
    private static AssertionError unexpected() { return new AssertionError("Unexpected optional native growth-part query"); }
    @Override public GrowSignal growSignal(Level level, BlockPos pos, GrowSignal signal) {
        pulses++; energy = signal.energy; signal.success = false; return signal;
    }
    @Override public Cell getHydrationCell(BlockGetter level, BlockPos pos, BlockState state, Direction direction, LeavesProperties leaves) { throw unexpected(); }
    @Override public int probabilityForBlock(BlockState state, BlockGetter level, BlockPos pos, BranchBlock branch) { throw unexpected(); }
    @Override public int getRadiusForConnection(BlockState state, BlockGetter level, BlockPos pos, BranchBlock branch, Direction direction, int radius) { throw unexpected(); }
    @Override public int getRadius(BlockState state) { throw unexpected(); }
    @Override public boolean shouldAnalyse(BlockState state, BlockGetter level, BlockPos pos) { throw unexpected(); }
    @Override public MapSignal analyse(BlockState state, LevelAccessor level, BlockPos pos, Direction direction, MapSignal signal) { throw unexpected(); }
    @Override public Family getFamily(BlockState state, BlockGetter level, BlockPos pos) { throw unexpected(); }
    @Override public int branchSupport(BlockState state, BlockGetter level, BranchBlock branch, BlockPos pos, Direction direction, int radius) { throw unexpected(); }
    @Override public TreePartType getTreePartType() { throw unexpected(); }
}
