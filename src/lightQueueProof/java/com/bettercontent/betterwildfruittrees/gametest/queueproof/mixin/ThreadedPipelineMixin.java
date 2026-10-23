package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;

import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.util.thread.ProcessorMailbox;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.IntSupplier;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedPipelineMixin {
 @Shadow @Final private ProcessorMailbox<Runnable> taskMailbox;
 @Inject(method="addTask(IILjava/util/function/IntSupplier;Lnet/minecraft/server/level/ThreadedLevelLightEngine$TaskType;Ljava/lang/Runnable;)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void proof$issue(int x,int z,IntSupplier priority,@Coerce Object type,Runnable task,CallbackInfo ci){if(QueueProofLedger.active())QueueProofLedger.issue(System.identityHashCode(this),System.identityHashCode(taskMailbox),x,z,((Enum<?>)type).ordinal(),System.identityHashCode(task));}
 @Inject(method="addTask(IILjava/util/function/IntSupplier;Lnet/minecraft/server/level/ThreadedLevelLightEngine$TaskType;Ljava/lang/Runnable;)V",at=@At("RETURN"),require=1,expect=1,allow=1)
 private void proof$issued(int x,int z,IntSupplier priority,@Coerce Object type,Runnable task,CallbackInfo ci){if(QueueProofLedger.active())QueueProofLedger.issueEnd();}
 @Redirect(method="lambda$addTask$15(Lnet/minecraft/server/level/ThreadedLevelLightEngine$TaskType;Ljava/lang/Runnable;)V",at=@At(value="INVOKE",target="Lit/unimi/dsi/fastutil/objects/ObjectList;add(Ljava/lang/Object;)Z",remap=false),require=1,expect=1,allow=1)
 private boolean proof$staged(ObjectList<Object> list,Object pair,@Coerce Object type,Runnable task) throws Throwable {boolean result;try{result=list.add(pair);}catch(Throwable original){QueueProofLedger.originalThrowable(2,System.identityHashCode(original),System.identityHashCode(original.getClass()));throw original;}if(QueueProofLedger.active()){if(!result)QueueProofLedger.invalid();QueueProofLedger.staged(System.identityHashCode(this),System.identityHashCode(task),((Enum<?>)type).ordinal());}return result;}
 @Redirect(method="runUpdate",at=@At(value="INVOKE",target="Ljava/lang/Math;min(II)I",remap=false),require=1,expect=1,allow=1)
 private int proof$batch(int total,int maximum){int result=Math.min(total,maximum);if(QueueProofLedger.active())QueueProofLedger.run(System.identityHashCode(this),result,total);return result;}
 @Inject(method="runUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I",shift=At.Shift.BEFORE),require=1,expect=1,allow=1)
 private void proof$solverBefore(CallbackInfo ci){QueueProofLedger.solver(false);}
 @Inject(method="runUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I",shift=At.Shift.AFTER),require=1,expect=1,allow=1)
 private void proof$solverAfter(CallbackInfo ci){QueueProofLedger.solver(true);}
 @Redirect(method="runUpdate",at=@At(value="INVOKE",target="Ljava/lang/Runnable;run()V",remap=false),require=2,expect=2,allow=2)
 private void proof$task(Runnable task) throws Throwable {long id=QueueProofLedger.taskBegin(System.identityHashCode(task));boolean returned=false;try{task.run();returned=true;}catch(Throwable original){QueueProofLedger.originalThrowable(1,System.identityHashCode(original),System.identityHashCode(original.getClass()));throw original;}finally{QueueProofLedger.taskEnd(id,returned);}}
 @Redirect(method="addTask(IILjava/util/function/IntSupplier;Lnet/minecraft/server/level/ThreadedLevelLightEngine$TaskType;Ljava/lang/Runnable;)V",at=@At(value="INVOKE",target="Lnet/minecraft/util/thread/ProcessorHandle;tell(Ljava/lang/Object;)V"),require=1,expect=1,allow=1)
 private void proof$send(net.minecraft.util.thread.ProcessorHandle<Object> handle,Object original) throws Throwable {int identity=System.identityHashCode(original);QueueProofLedger.originalSend(identity,0);try{handle.tell(original);QueueProofLedger.originalSend(identity,1);}catch(Throwable failure){QueueProofLedger.originalThrowable(3,System.identityHashCode(failure),System.identityHashCode(failure.getClass()));throw failure;}}
 @Inject(method="runUpdate",at=@At(value="INVOKE",target="Lit/unimi/dsi/fastutil/objects/ObjectListIterator;remove()V",remap=false,shift=At.Shift.AFTER),require=1,expect=1,allow=1)
 private void proof$retire(CallbackInfo ci){QueueProofLedger.retired();}
 @Inject(method="runUpdate",at=@At("RETURN"),require=1,expect=1,allow=1)
 private void proof$runEnd(CallbackInfo ci){QueueProofLedger.runEnd();}
}
