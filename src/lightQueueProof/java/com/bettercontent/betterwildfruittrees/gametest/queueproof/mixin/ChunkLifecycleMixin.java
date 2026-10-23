package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.NativeObserver;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChunkAccess.class)
public abstract class ChunkLifecycleMixin {
 @Inject(method="setLightCorrect(Z)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void proof$correct(boolean value,CallbackInfo ci){NativeObserver.lifecycle(this,0,value);}
 @Inject(method="setUnsaved(Z)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void proof$unsaved(boolean value,CallbackInfo ci){NativeObserver.lifecycle(this,1,value);}
}
