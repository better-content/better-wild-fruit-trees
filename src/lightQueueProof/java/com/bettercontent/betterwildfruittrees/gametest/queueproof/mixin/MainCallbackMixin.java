package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.minecraft.server.level.ServerChunkCache$MainThreadExecutor")
public abstract class MainCallbackMixin {
 @Inject(method="doRunTask(Ljava/lang/Runnable;)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void proof$begin(Runnable original,CallbackInfo ci){QueueProofLedger.callback(System.identityHashCode(original),false);}
 @Inject(method="doRunTask(Ljava/lang/Runnable;)V",at=@At("RETURN"),require=1,expect=1,allow=1)
 private void proof$end(Runnable original,CallbackInfo ci){QueueProofLedger.callback(System.identityHashCode(original),true);}
}
