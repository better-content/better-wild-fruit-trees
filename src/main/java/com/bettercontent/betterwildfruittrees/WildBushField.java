package com.bettercontent.betterwildfruittrees;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Only invoked by normal configured-feature decoration; never scans or backfills chunks. */
public final class WildBushField extends Feature<WildBushField.Config> {
    public record Config(String block) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = Codec.STRING.fieldOf("block").xmap(Config::new, Config::block).codec();
    }
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, WildFruitTrees.MOD_ID);
    static { FEATURES.register("bush_field", WildBushField::new); }
    public WildBushField() { super(Config.CODEC); }
    @Override public boolean place(FeaturePlaceContext<Config> context) {
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(context.config().block()));
        // Optional mod absent: no invalid registry-backed state in the datapack codec.
        if (block == null || block == Blocks.AIR || !WildBushes.isBush(block.defaultBlockState())) return false;
        var level = context.level();
        var random = context.random();
        int target = 30 + random.nextInt(13), placed = 0;
        // Centre the larger field on its decoration chunk so writes stay in its 3x3 chunk neighbourhood.
        var chunk = level instanceof WorldGenRegion region ? region.getCenter() : new ChunkPos(context.origin());
        int centerX = chunk.getMiddleBlockX(), centerZ = chunk.getMiddleBlockZ();
        var state = WildBushes.mature(block.defaultBlockState());
        for (int attempt = 0; attempt < 288 && placed < target; attempt++) {
            int x = centerX + random.nextInt(24) - random.nextInt(24);
            int z = centerZ + random.nextInt(24) - random.nextInt(24);
            BlockPos pos = null;
            // Follow local ground rather than trusting a WG heightmap (which may include canopy).
            for (int y = context.origin().getY() + 5; y >= context.origin().getY() - 5; y--) {
                var candidate = new BlockPos(x, y, z);
                if (level.isEmptyBlock(candidate) && state.canSurvive(level, candidate)) { pos = candidate; break; }
            }
            if (pos == null) continue;
            // Sparse uneven spacing, not a filled disk or agricultural grid.
            boolean crowded = false;
            for (var neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1)))
                if (level.hasChunk(neighbor.getX() >> 4, neighbor.getZ() >> 4)
                        && WildBushes.isBush(level.getBlockState(neighbor))) { crowded = true; break; }
            if (!crowded) { level.setBlock(pos, state, 2); placed++; }
        }
        return placed > 0;
    }
}
