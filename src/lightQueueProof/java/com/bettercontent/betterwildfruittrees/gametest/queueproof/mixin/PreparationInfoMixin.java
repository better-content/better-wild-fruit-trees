package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Development-only unmapped GameTest preparation; never an ordinary/shipping classpath. */
@Mixin(value=GameTestInfo.class,remap=false)
public abstract class PreparationInfoMixin {
 @WrapMethod(method="spawnStructure(Lnet/minecraft/core/BlockPos;I)V",require=1,expect=1,allow=1,remap=false)
 private void proof$scopeSpawn(BlockPos pos,int floor,Operation<Void> original){GameTestInfo info=(GameTestInfo)(Object)this;try(var scope=PreparationScope.info(info.getTestName(),info.getStructureName())){if(scope.qualified())QueueProofNativeRegistry.validate();original.call(pos,floor);}}
 @WrapMethod(method="clearStructure()V",require=1,expect=1,allow=1,remap=false)
 private void proof$scopeClear(Operation<Void> original){GameTestInfo info=(GameTestInfo)(Object)this;try(var scope=PreparationScope.info(info.getTestName(),info.getStructureName())){if(scope.qualified())QueueProofNativeRegistry.validate();original.call();}}
 @Redirect(method="clearStructure()V",at=@At(value="INVOKE",target="Lnet/minecraft/gametest/framework/StructureUtils;clearSpaceForStructure(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;ILnet/minecraft/server/level/ServerLevel;)V",remap=false),require=1,expect=1,allow=1,remap=false)
 private void proof$clear(BoundingBox bounds,int floor,ServerLevel world){if(PreparationScope.takeInfoClear()){try(var lease=QueueClearPreparation.prepare(bounds,world)){StructureUtils.clearSpaceForStructure(bounds,floor,world);}}else StructureUtils.clearSpaceForStructure(bounds,floor,world);}
}
