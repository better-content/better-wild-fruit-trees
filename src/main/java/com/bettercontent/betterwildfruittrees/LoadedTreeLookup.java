package com.bettercontent.betterwildfruittrees;

import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Bounded, loaded-only root lookup. Never substitutes the family's common species. */
public final class LoadedTreeLookup {
    private static final int MAX_VISITED = 4096;
    private static final int MAX_DISTANCE = 128;
    private static final Map<Level, Map<BlockPos, CachedRoot>> CACHE = new WeakHashMap<>();
    private record CachedRoot(BlockPos root, long expires) {}
    private LoadedTreeLookup() {}

    public static boolean isProtected(BlockGetter getter, BlockPos start) {
        if (!(getter instanceof Level level) || !level.hasChunkAt(start)) return false;
        BlockState state = level.getBlockState(start);
        // Thick trunks expose shell blocks around their center. Follow only loaded local
        // shell directions; never invoke a native helper that could request another chunk.
        for (int step = 0; step < 3 && state.getBlock() instanceof com.ferreusveritas.dynamictrees.block.branch.TrunkShellBlock shell; step++) {
            var direction = shell.getMuseDir(state, start);
            if (direction == null) return false;
            start = start.offset(direction.getOffset());
            if (!level.hasChunkAt(start)) return false;
            state = level.getBlockState(start);
        }
        if (state.getBlock() instanceof RootyBlock rooty)
            return protectedRoot(level, start, rooty, state);
        if (!(state.getBlock() instanceof BranchBlock branch)) return false;
        Family family = branch.getFamily();
        synchronized (CACHE) {
            Map<BlockPos, CachedRoot> cache = CACHE.computeIfAbsent(level, key -> new LinkedHashMap<>());
            CachedRoot cached = cache.get(start);
            if (cached != null && cached.expires >= level.getGameTime() && level.hasChunkAt(cached.root)) {
                BlockState rootState = level.getBlockState(cached.root);
                if (rootState.getBlock() instanceof RootyBlock rooty) {
                    Species species = rooty.getSpecies(rootState, level, cached.root);
                    if (species.getFamily() == family)
                        return protectedIdentity(level, cached.root, species);
                }
            }
            ArrayDeque<BlockPos> pending = new ArrayDeque<>();
            HashSet<BlockPos> seen = new HashSet<>();
            pending.add(start.immutable());
            while (!pending.isEmpty() && seen.size() < MAX_VISITED) {
                BlockPos pos = pending.removeFirst();
                if (!seen.add(pos) || start.distManhattan(pos) > MAX_DISTANCE || !level.hasChunkAt(pos)) continue;
                BlockState current = level.getBlockState(pos);
                if (!(current.getBlock() instanceof BranchBlock currentBranch) || currentBranch.getFamily() != family) continue;
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.relative(direction);
                    if (!level.hasChunkAt(next)) continue;
                    BlockState neighbor = level.getBlockState(next);
                    if (neighbor.getBlock() instanceof RootyBlock rooty
                        && next.relative(rooty.getTrunkDirection(level, next)).equals(pos)) {
                        Species species = rooty.getSpecies(neighbor, level, next);
                        if (species.getFamily() != family) continue;
                        if (cache.size() >= 256) cache.clear();
                        cache.put(start.immutable(), new CachedRoot(next.immutable(), level.getGameTime() + 5));
                        return protectedIdentity(level, next, species);
                    }
                    if (!seen.contains(next) && neighbor.getBlock() instanceof BranchBlock adjacent && adjacent.getFamily() == family)
                        pending.addLast(next);
                }
            }
        }
        return false;
    }
    private static boolean protectedRoot(Level level, BlockPos pos, RootyBlock rooty, BlockState state) {
        return protectedIdentity(level, pos, rooty.getSpecies(state, level, pos));
    }
    private static boolean protectedIdentity(Level level, BlockPos root, Species species) {
        String id = species.getRegistryName().toString();
        return WildFruitPolicy.protectsSpecies(id)
            || com.bettercontent.betterwildfruittrees.compat.RootLedger.inherited(level, root, id);
    }
}
