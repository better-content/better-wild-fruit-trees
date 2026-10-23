package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=StructureUtils.class,remap=false)
public abstract class PreparationSpawnMixin {
 @WrapMethod(method="spawnStructure(Ljava/lang/String;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Rotation;ILnet/minecraft/server/level/ServerLevel;Z)Lnet/minecraft/world/level/block/entity/StructureBlockEntity;",require=1,expect=1,allow=1,remap=false)
 private static StructureBlockEntity proof$scope(String name,BlockPos pos,Rotation rotation,int floor,ServerLevel world,boolean flag,Operation<StructureBlockEntity> original){try(var scope=PreparationScope.spawn()){return original.call(name,pos,rotation,floor,world,flag);}}
 @Redirect(method="spawnStructure(Ljava/lang/String;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Rotation;ILnet/minecraft/server/level/ServerLevel;Z)Lnet/minecraft/world/level/block/entity/StructureBlockEntity;",at=@At(value="INVOKE",target="Lnet/minecraft/gametest/framework/StructureUtils;clearSpaceForStructure(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;ILnet/minecraft/server/level/ServerLevel;)V",remap=false),require=1,expect=1,allow=1,remap=false)
 private static void proof$clear(BoundingBox bounds,int floor,ServerLevel world){if(PreparationScope.takeSpawnClear()){try(var lease=QueueClearPreparation.prepare(bounds,world)){StructureUtils.clearSpaceForStructure(bounds,floor,world);}}else StructureUtils.clearSpaceForStructure(bounds,floor,world);}
}
