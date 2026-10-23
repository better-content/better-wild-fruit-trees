package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ServerChunkCache.class)
public abstract class NotificationMixin {
 // execute is the inherited java.util.concurrent.Executor contract, not an obfuscated game method.
 @Redirect(method="onLightUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerChunkCache$MainThreadExecutor;execute(Ljava/lang/Runnable;)V",remap=false),require=1,expect=1,allow=1)
 private void proof$sent(@Coerce Object executor,Runnable original,LightLayer layer,SectionPos pos) throws Throwable {QueueProofLedger.callbackSent(System.identityHashCode(original),pos.asLong(),1);try{((BlockableEventLoop<?>)executor).execute(original);}catch(Throwable failure){QueueProofLedger.originalThrowable(7,System.identityHashCode(failure),System.identityHashCode(failure.getClass()));throw failure;}}
}
