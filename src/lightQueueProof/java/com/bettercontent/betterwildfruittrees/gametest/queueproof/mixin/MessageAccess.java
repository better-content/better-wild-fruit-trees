package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.function.Function;
@Mixin(ChunkTaskPriorityQueueSorter.Message.class)
public interface MessageAccess { @Accessor("task") Function<?,?> proof$task(); }
