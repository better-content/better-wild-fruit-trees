package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Inert JDK realm: original suppliers/runnables are outside observer catch boundaries. */
public final class QueueObserverSafetySelfTest {
 public static void main(String[] args)throws Exception{
  var s=new QueueProofLedgerEngine.Session(new Properties());QueueProofLedgerEngine.active=s;
  AtomicInteger originals=new AtomicInteger();RuntimeException original=new RuntimeException("original");
  for(int slot:new int[]{-2,-1,-3,Integer.MIN_VALUE,6,Integer.MAX_VALUE}){
   try(var intent=QueueProofLedger.intentScope(slot)){
    QueueProofLedger.issue(11,22,0,0,0,100+originals.get());
    int result;try{result=((java.util.function.IntSupplier)()->{originals.incrementAndGet();return 47;}).getAsInt();}finally{QueueProofLedger.issueEnd();}if(result!=47)throw new AssertionError();
    QueueProofLedger.lifecycle(0,44,1);QueueProofLedger.submitted(slot);QueueProofLedger.ack(slot,0,0,0,0,0);QueueProofLedger.outcome(slot);
   }
   if(s.intent!=-1)throw new AssertionError("stale intent");
  }
  if(originals.get()!=6)throw new AssertionError("original suppressed/repeated");
  try{try(var intent=QueueProofLedger.intentScope(2)){QueueProofLedger.issue(11,22,0,0,0,999);try{originals.incrementAndGet();throw original;}finally{QueueProofLedger.issueEnd();}}}catch(RuntimeException caught){if(caught!=original)throw new AssertionError("throwable replaced",caught);}if(s.intent!=-1)throw new AssertionError();
  Thread worker=new Thread(()->{QueueProofLedger.intent(4);QueueProofLedger.issue(11,22,0,0,0,991);QueueProofLedger.issueEnd();QueueProofLedger.lifecycle(0,44,1);});worker.start();worker.join();
  for(long id:new long[]{-1,Long.MIN_VALUE,Long.MAX_VALUE,QueueProofLedger.TASKS})QueueProofLedger.taskEnd(id,false);
  s.next.set(Integer.MAX_VALUE);QueueProofLedger.issue(11,22,0,0,0,992);QueueProofLedger.issueEnd();
  s.nextCallback.set(Integer.MAX_VALUE);QueueProofLedger.callbackSent(992,0,0);
  for(int i=0;i<4;i++)s.engines.set(i,100+i);QueueProofLedger.run(999,1,1);QueueProofLedger.solver(false);QueueProofLedger.retired();QueueProofLedger.runEnd();
  for(int i=0;i<QueueProofLedger.THREADS;i++)s.threads.set(i,1_000_000+i);QueueProofLedger.issueEnd();QueueProofLedger.lifecycle(0,44,1);QueueProofLedger.callback(99,false);QueueProofLedger.callback(99,true);
  var stack=new QueueProofLedgerEngine.Stack();for(int i=0;i<QueueProofLedger.DEPTH+2;i++)stack.push(0,i+1,s.bad);if(stack.top(0)!=0)throw new AssertionError("overflow leaked ancestor");stack.pop(0,s.bad);stack.pop(0,s.bad);if(stack.top(0)!=QueueProofLedger.DEPTH)throw new AssertionError("overflow popped ancestor");
  s.sequence.set(Integer.MAX_VALUE);QueueProofLedger.lifecycle(0,44,1);QueueProofLedger.notification(0,false);QueueProofLedger.originalThrowable(1,2,3);
  for(int i=0;i<QueueProofLedger.THREADS;i++)s.threads.set(i,0);int thread=s.thread();s.taskStack.push(thread,1,s.bad);long unknown=QueueProofLedger.taskBegin(123456);if(unknown!=0||s.taskStack.top(thread)!=0)throw new AssertionError("unknown/racy entry leaked ancestor");QueueProofLedger.lifecycle(0,44,1);QueueProofLedger.taskEnd(unknown,true);if(s.taskStack.top(thread)!=1)throw new AssertionError("unknown exit popped ancestor");s.taskStack.pop(thread,s.bad);
  for(var counter:new java.util.concurrent.atomic.AtomicInteger[]{s.submitted,s.retired,s.runs,s.completed,s.publications,s.ticketRemovals,s.notifications,s.callbacks,s.callbackReturns}){counter.set(Integer.MAX_VALUE);s.inc(counter);if(counter.get()!=Integer.MAX_VALUE)throw new AssertionError("counter wrapped");counter.set(0);s.dec(counter);if(counter.get()!=0)throw new AssertionError("counter underflow");}
  for(int at=0;at<48;at++){s.counts.set(at,Integer.MAX_VALUE);s.count(at);if(s.counts.get(at)!=Integer.MAX_VALUE)throw new AssertionError("slot counter wrapped");}s.count(-16);s.count(48);s.count(Integer.MAX_VALUE);
  if(s.observerFaults.get()!=0)throw new AssertionError("known invalid input reached fallback "+s.observerFaults.get());if(!s.bad.get()||!s.unknown.get())throw new AssertionError("invalid accepted");
  s.taskStack.depth[thread]=Integer.MAX_VALUE;try{QueueProofLedger.lifecycle(0,44,1);originals.incrementAndGet();throw original;}catch(RuntimeException caught){if(caught!=original)throw new AssertionError("fault replaced original",caught);}finally{s.taskStack.depth[thread]=0;}if(s.observerFaults.get()!=1)throw new AssertionError("fault not explicit");
  QueueProofLedgerEngine.active=null;QueueProofLedger.issue(0,0,0,0,0,0);QueueProofLedger.ack(-2,0,0,0,0,0);QueueProofLedger.taskEnd(Long.MAX_VALUE,false);
  System.out.println("PASS nonthrowing observer sentinel/invalidslot/token/thread/ID/record/engine/frame overflow, intent finally, original return/throwable; native=PENDING");
 }
}
