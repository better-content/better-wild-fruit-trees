package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.util.thread.ProcessorHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.function.*;
@Mixin(ChunkTaskPriorityQueueSorter.class)
public abstract class SorterAdmissionMixin {
 @Inject(method="message(Ljava/lang/Runnable;JLjava/util/function/IntSupplier;)Lnet/minecraft/server/level/ChunkTaskPriorityQueueSorter$Message;",at=@At("RETURN"),require=1,expect=1,allow=1)
 private static void proof$sent(Runnable wrapper,long pos,IntSupplier level,CallbackInfoReturnable<ChunkTaskPriorityQueueSorter.Message<Runnable>> cir){if(QueueProofLedger.active())QueueProofLedger.sent(System.identityHashCode(((MessageAccess)(Object)cir.getReturnValue()).proof$task()),System.identityHashCode(wrapper),pos);}
 @Inject(method="lambda$submit$12(Lnet/minecraft/util/thread/ProcessorHandle;Ljava/util/function/IntSupplier;JLjava/util/function/Function;Z)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void proof$begin(ProcessorHandle<?> processor,IntSupplier level,long pos,Function<?,?> task,boolean blocker,CallbackInfo ci){if(QueueProofLedger.active())QueueProofLedger.admissionBegin(System.identityHashCode(task));}
 @Redirect(method="lambda$submit$12(Lnet/minecraft/util/thread/ProcessorHandle;Ljava/util/function/IntSupplier;JLjava/util/function/Function;Z)V",at=@At(value="INVOKE",target="Ljava/util/function/IntSupplier;getAsInt()I",remap=false),require=1,expect=1,allow=1)
 private int proof$priority(IntSupplier original,ProcessorHandle<?> processor,IntSupplier level,long pos,Function<?,?> task,boolean blocker) throws Throwable {int actual;try{actual=original.getAsInt();}catch(Throwable failure){QueueProofLedger.originalThrowable(4,System.identityHashCode(failure),System.identityHashCode(failure.getClass()));throw failure;}if(QueueProofLedger.active())QueueProofLedger.admitted(pos,actual);return actual;}
 @Inject(method="lambda$submit$12(Lnet/minecraft/util/thread/ProcessorHandle;Ljava/util/function/IntSupplier;JLjava/util/function/Function;Z)V",at=@At("RETURN"),require=1,expect=1,allow=1)
 private void proof$end(ProcessorHandle<?> processor,IntSupplier level,long pos,Function<?,?> task,boolean blocker,CallbackInfo ci){if(QueueProofLedger.active())QueueProofLedger.admissionEnd();}
}
