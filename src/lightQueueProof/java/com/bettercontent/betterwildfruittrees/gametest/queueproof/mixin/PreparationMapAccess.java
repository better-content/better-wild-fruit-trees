package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import net.minecraft.server.level.*;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(ChunkMap.class)
public interface PreparationMapAccess {
 @Accessor("mainThreadExecutor") BlockableEventLoop<Runnable> proof$executor();
 @Invoker("getVisibleChunkIfPresent") ChunkHolder proof$visible(long key);
}
