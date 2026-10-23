package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChunkAccess.class)
public abstract class ChunkLifecycleMixin {
 @Inject(method="setLightCorrect(Z)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void proof$correct(boolean value,CallbackInfo ci){QueueProofLedger.lifecycle(0,System.identityHashCode(this),value?1:0);}
 @Inject(method="setUnsaved(Z)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void proof$unsaved(boolean value,CallbackInfo ci){QueueProofLedger.lifecycle(1,System.identityHashCode(this),value?1:0);}
}
