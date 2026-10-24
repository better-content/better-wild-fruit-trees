package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ServerChunkCache.class)
public interface PreparationCacheAccess { @Invoker("runDistanceManagerUpdates") boolean proof$admit(); }
