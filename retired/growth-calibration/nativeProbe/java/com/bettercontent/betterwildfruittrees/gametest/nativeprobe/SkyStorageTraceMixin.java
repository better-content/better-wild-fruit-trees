package com.bettercontent.betterwildfruittrees.gametest.nativeprobe;

import com.bettercontent.betterwildfruittrees.gametest.OriginalLightTrace;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkyLightSectionStorage.class)
public abstract class SkyStorageTraceMixin {
    @Inject(method = "getLightValue(J)I", at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private void trace$enter(long pos, CallbackInfoReturnable<Integer> cir) {
        if (!OriginalLightTrace.ownerActive()) return;
        OriginalLightTrace.skyEnter(System.identityHashCode(this));
    }
    @Inject(method = "getLightValue(J)I", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void trace$return(long pos, CallbackInfoReturnable<Integer> cir) {
        if (!OriginalLightTrace.ownerActive()) return;
        OriginalLightTrace.skyReturn(System.identityHashCode(this), pos, cir.getReturnValue());
    }
}
