package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.entity.SpeciesBlockEntity;
import com.ferreusveritas.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.entity.FallingTreeEntity;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Read-only nonshipping witnesses, not a new freshness oracle, retirement barrier or measurement.
 * No RNG calls, entity ticking/removal, chunk loading, configuration writes or gameplay hooks.
 * Lifecycle markers report native events, NOT proof that cleanupRootyDirt was actually invoked. */
@Mod.EventBusSubscriber(modid = WildFruitTrees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public final class LegacyGrowthWitness {
    private static boolean enabled() {
        return Boolean.getBoolean("better_wild_fruit_trees.growthDiagnostics")
            && Boolean.getBoolean("forge.gameTestServer")
            && "better_wild_fruit_trees".equals(System.getProperty("forge.enabledGameTestNamespaces"));
    }
    private static boolean inside(BlockPos pos) {
        return pos.getX() >= 4 && pos.getX() <= 92 && pos.getY() >= 160 && pos.getY() <= 220
            && pos.getZ() >= 4 && pos.getZ() <= 92;
    }
    static void sample(GameTestHelper h, BlockPos root, Species tree, int sample, String phase) {
        if (!enabled()) return;
        var level = h.getLevel();
        var rootState = level.getBlockState(root);
        var entity = level.getBlockEntity(root);
        String resolved = rootState.getBlock() instanceof RootyBlock soil
            ? soil.getSpecies(rootState, level, root).getRegistryName().toString() : "NOT_ROOTY";
        var branch = tree.getFamily().getBranch().orElseThrow();
        var leaves = tree.getLeavesProperties().getDynamicLeavesBlock().orElseThrow();
        var baseState = level.getBlockState(root.above());
        int branches = 0, foreignBranches = 0, foliage = 0, foreignLeaves = 0, other = 0, unloaded = 0;
        long hash = 0xcbf29ce484222325L;
        // Exactly the pre-existing cleared plot, loaded-only. A missing chunk is recorded, not loaded.
        for (var pos : BlockPos.betweenClosed(root.offset(-44, 0, -44), root.offset(44, 60, 44))) {
            if (!level.hasChunkAt(pos)) { unloaded++; continue; }
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.is(branch)) branches++;
            else if (state.getBlock() instanceof BranchBlock) foreignBranches++;
            else if (state.is(leaves)) foliage++;
            else if (state.getBlock() instanceof DynamicLeavesBlock) foreignLeaves++;
            else other++;
            hash = (hash ^ pos.asLong()) * 0x100000001b3L;
            hash = (hash ^ Block.getId(state)) * 0x100000001b3L;
        }
        System.out.println("LEGACY_GROWTH_WITNESS phase=" + phase + " species=" + tree.getRegistryName()
            + " sample=" + sample + " testTick=" + h.getTick() + " gameTick=" + level.getGameTime()
            + " day=" + level.getDayTime() + " root=" + root + " rootState=" + rootState
            + " resolved=" + resolved + " entity=" + (entity == null ? "NONE" : entity.getClass().getName())
            + " entityIdentity=" + (entity == null ? 0 : System.identityHashCode(entity))
            + " entitySpecies=" + (entity instanceof SpeciesBlockEntity speciesEntity ? speciesEntity.getSpecies().getRegistryName() : "NONE")
            + " base=" + level.getBlockState(root.above()) + " crown=" + level.getBlockState(root.above(2))
            + " radius=" + (baseState.is(branch) ? branch.getRadius(baseState) : -1)
            + " branches=" + branches + " foreignBranches=" + foreignBranches + " leaves=" + foliage
            + " foreignLeaves=" + foreignLeaves + " other=" + other + " unloadedCells=" + unloaded
            + " geometryHash=" + Long.toUnsignedString(hash, 16)
            + " biome=" + level.getBiome(root).unwrapKey().map(k -> k.location().toString()).orElse("UNKEYED")
            + " light=" + level.getMaxLocalRawBrightness(root.above(3)) + " rain=" + level.isRaining()
            + " energy=" + tree.getEnergy(level, root) + " kit=" + tree.getGrowthLogicKit().getConfigurable().getRegistryName()
            + " taper=" + tree.getTapering() + " maxRadius=" + tree.getMaxBranchRadius());
    }
    private static void birth(ServerLevel level, FallingTreeEntity entity, String phase, boolean cancelled) {
        if (!enabled() || !level.dimension().location().toString().equals("minecraft:overworld")) return;
        var data = entity.getDestroyData();
        if (data == null || data.cutPos == null || !inside(data.cutPos)) return;
        System.out.println("LEGACY_GROWTH_BIRTH phase=" + phase + " uuid=" + entity.getUUID()
            + " gameTick=" + level.getGameTime() + " cut=" + data.cutPos + " species=" + data.species.getRegistryName()
            + " entityTick=" + entity.tickCount + " branches=" + data.getNumBranches()
            + " cancelledObserved=" + cancelled + " removal=" + entity.getRemovalReason()
            + " currentRoot=" + (level.hasChunkAt(data.cutPos.below()) ? level.getBlockState(data.cutPos.below()) : "UNLOADED"));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void joined(EntityJoinLevelEvent event) {
        if (!enabled()) return;
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof FallingTreeEntity tree)
            birth(level, tree, "join", event.isCanceled());
    }
    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (!enabled()) return;
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof FallingTreeEntity tree)
            birth(level, tree, "leave", false);
    }
}
