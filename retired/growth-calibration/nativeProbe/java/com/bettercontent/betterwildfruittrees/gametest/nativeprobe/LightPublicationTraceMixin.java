package com.bettercontent.betterwildfruittrees.gametest.nativeprobe;

import com.bettercontent.betterwildfruittrees.gametest.OriginalLightTrace;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.objectweb.asm.Opcodes;

/** AFTER the original volatile write, not method RETURN (which can execute without publication). */
@Mixin(LayerLightSectionStorage.class)
public abstract class LightPublicationTraceMixin {
    @Inject(method = "swapSectionMap()V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/lighting/LayerLightSectionStorage;visibleSectionData:Lnet/minecraft/world/level/lighting/DataLayerStorageMap;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), require = 1, expect = 1, allow = 1)
    private void trace$published(CallbackInfo ci) {
        if (!OriginalLightTrace.publicationActive()) return;
        OriginalLightTrace.published(System.identityHashCode(this));
    }
}
