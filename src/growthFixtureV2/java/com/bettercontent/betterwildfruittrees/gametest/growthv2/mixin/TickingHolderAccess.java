package com.bettercontent.betterwildfruittrees.gametest.growthv2.mixin;

import com.bettercontent.betterwildfruittrees.gametest.GrowthFixtureV2;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface TickingHolderAccess extends GrowthFixtureV2.TickingAccess {
    @Override @Invoker("getVisibleChunkIfPresent") ChunkHolder growthV2$visible(long key);
}
