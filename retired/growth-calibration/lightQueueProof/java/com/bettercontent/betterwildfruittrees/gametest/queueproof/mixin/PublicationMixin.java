package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.NativeObserver;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LayerLightSectionStorage.class)
public abstract class PublicationMixin {
 @Inject(method="swapSectionMap()V",at=@At(value="FIELD",target="Lnet/minecraft/world/level/lighting/LayerLightSectionStorage;visibleSectionData:Lnet/minecraft/world/level/lighting/DataLayerStorageMap;",opcode=181,shift=At.Shift.AFTER),require=1,expect=1,allow=1)
 private void proof$publication(CallbackInfo ci){NativeObserver.published(this);}
}
