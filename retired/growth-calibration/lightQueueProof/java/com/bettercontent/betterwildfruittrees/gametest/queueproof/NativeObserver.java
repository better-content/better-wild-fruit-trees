package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin.MessageAccess;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin.TicketAccess;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.SectionPos;
/** Temporary native metadata only. Every derivation is diagnostic; no reference is retained.
 * NO original pipeline operation, allocation, lambda, I/O or gameplay query belongs here. */
public final class NativeObserver {
 private NativeObserver(){}
 public static void issue(Object engine,Object mailbox,int x,int z,Object type,Object task){try{if(QueueProofLedger.active())QueueProofLedger.issue(System.identityHashCode(engine),System.identityHashCode(mailbox),x,z,((Enum<?>)type).ordinal(),System.identityHashCode(task));}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void staged(Object engine,Object task,Object type,boolean result){try{if(QueueProofLedger.active()){if(!result)QueueProofLedger.invalid();QueueProofLedger.staged(System.identityHashCode(engine),System.identityHashCode(task),((Enum<?>)type).ordinal());}}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void factory(Object message,Object wrapper,long pos){try{if(QueueProofLedger.active())QueueProofLedger.sent(System.identityHashCode(((MessageAccess)message).proof$task()),System.identityHashCode(wrapper),pos);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void ticket(Object ticket,long pos,boolean removed){try{if(QueueProofLedger.active())QueueProofLedger.ticket(pos,removed,((TicketAccess)ticket).proof$type()==TicketType.LIGHT);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void ticketSent(Object task,ChunkPos pos){try{if(QueueProofLedger.active())QueueProofLedger.callbackSent(System.identityHashCode(task),pos.toLong(),0);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void notificationSent(Object task,SectionPos pos){try{if(QueueProofLedger.active())QueueProofLedger.callbackSent(System.identityHashCode(task),pos.asLong(),1);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void thrown(int point,Throwable original){try{QueueProofLedger.originalThrowable(point,System.identityHashCode(original),System.identityHashCode(original.getClass()));}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void send(Object task,int outcome){try{QueueProofLedger.originalSend(System.identityHashCode(task),outcome);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void admission(Object task){try{if(QueueProofLedger.active())QueueProofLedger.admissionBegin(System.identityHashCode(task));}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void batch(Object engine,int n,int total){try{if(QueueProofLedger.active())QueueProofLedger.run(System.identityHashCode(engine),n,total);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static long taskBegin(Object task){try{return QueueProofLedger.taskBegin(System.identityHashCode(task));}catch(Throwable diagnostic){QueueProofLedger.observerFault();return 0;}}
 public static void lifecycle(Object chunk,int kind,boolean value){try{QueueProofLedger.lifecycle(kind,System.identityHashCode(chunk),value?1:0);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void callback(Object task,boolean end){try{QueueProofLedger.callback(System.identityHashCode(task),end);}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
 public static void published(Object storage){try{QueueProofLedger.published(System.identityHashCode(storage));}catch(Throwable diagnostic){QueueProofLedger.observerFault();}}
}
