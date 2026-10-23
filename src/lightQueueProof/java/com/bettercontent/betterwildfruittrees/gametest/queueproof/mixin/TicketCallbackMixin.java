package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ChunkMap.class)
public abstract class TicketCallbackMixin {
 @Redirect(method="releaseLightTicket",at=@At(value="INVOKE",target="Lnet/minecraft/util/thread/BlockableEventLoop;tell(Ljava/lang/Runnable;)V"),require=1,expect=1,allow=1)
 private void proof$sent(BlockableEventLoop<Runnable> executor,Runnable original,ChunkPos pos) throws Throwable {QueueProofLedger.callbackSent(System.identityHashCode(original),pos.toLong(),0);try{executor.tell(original);}catch(Throwable failure){QueueProofLedger.originalThrowable(6,System.identityHashCode(failure),System.identityHashCode(failure.getClass()));throw failure;}}
}
