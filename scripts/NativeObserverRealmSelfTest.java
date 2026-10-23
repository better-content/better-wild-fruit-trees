package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin.*;
import it.unimi.dsi.fastutil.objects.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
/** Actual compiled observer mixin bodies + actual cached SDK declarations; NO World/engine/stubs. */
public final class NativeObserverRealmSelfTest {
 static final class Pipeline extends ThreadedPipelineMixin {}static final class Sorter extends SorterAdmissionMixin {}
 static Object call(Class<?> owner,Object target,String name,Object...args)throws Throwable{for(var method:owner.getDeclaredMethods())if(method.getName().equals(name)){method.setAccessible(true);try{return method.invoke(target,args);}catch(InvocationTargetException wrapped){throw wrapped.getCause();}}throw new AssertionError(name);}
 static final class ListPort extends ObjectArrayList<Object>{int calls;boolean result;RuntimeException failure;public boolean add(Object value){calls++;if(failure!=null)throw failure;return result;}}
 public static void main(String[] args)throws Throwable{
  var session=new QueueProofLedgerEngine.Session(new Properties());QueueProofLedgerEngine.active=session;Pipeline pipeline=new Pipeline();Sorter sorter=new Sorter();CallbackInfo ci=new CallbackInfo("inert",false);AtomicInteger calls=new AtomicInteger();RuntimeException original=new RuntimeException("original");
  for(Object type:new Object[]{null,new Object()}){
   call(ThreadedPipelineMixin.class,pipeline,"proof$issue",0,0,null,type,(Runnable)()->calls.incrementAndGet(),ci);
   for(boolean result:new boolean[]{false,true}){ListPort list=new ListPort();list.result=result;Object actual=call(ThreadedPipelineMixin.class,pipeline,"proof$staged",list,new Object(),type,(Runnable)()->{});if(!actual.equals(result)||list.calls!=1)throw new AssertionError("list result/count");}
   ListPort list=new ListPort();list.failure=original;try{call(ThreadedPipelineMixin.class,pipeline,"proof$staged",list,new Object(),type,(Runnable)()->{});throw new AssertionError();}catch(Throwable caught){if(caught!=original||list.calls!=1)throw new AssertionError("list throwable replaced",caught);}
   call(ThreadedPipelineMixin.class,pipeline,"proof$issued",0,0,null,type,null,ci);
  }
  for(Object message:new Object[]{null,new Object()})call(SorterAdmissionMixin.class,null,"proof$sent",null,0L,null,new CallbackInfoReturnable<Object>("inert",false,message));
  MessageAccess badMessage=new MessageAccess(){public Function<?,?> proof$task(){throw new AssertionError("diagnostic accessor");}};NativeObserver.factory(badMessage,null,0);
  TicketAccess badTicket=new TicketAccess(){public net.minecraft.server.level.TicketType<?> proof$type(){throw new AssertionError("diagnostic accessor");}};NativeObserver.ticket(badTicket,0,true);NativeObserver.ticket(null,0,false);NativeObserver.ticket(new Object(),0,true);
  NativeObserver.ticketSent(null,null);NativeObserver.notificationSent(null,null);NativeObserver.thrown(0,null);
  IntSupplier success=()->{calls.incrementAndGet();return 73;};Object result=call(SorterAdmissionMixin.class,sorter,"proof$priority",success,null,null,0L,null,false);if(!result.equals(73)||calls.get()!=1)throw new AssertionError("supplier result/count");
  IntSupplier failure=()->{calls.incrementAndGet();throw original;};try{call(SorterAdmissionMixin.class,sorter,"proof$priority",failure,null,null,0L,null,false);throw new AssertionError();}catch(Throwable caught){if(caught!=original||calls.get()!=2)throw new AssertionError("supplier throwable/count",caught);}
  Runnable task=()->{calls.incrementAndGet();throw original;};try{call(ThreadedPipelineMixin.class,pipeline,"proof$task",task);throw new AssertionError();}catch(Throwable caught){if(caught!=original||calls.get()!=3)throw new AssertionError("task throwable/count",caught);}
  Runnable successTask=()->calls.incrementAndGet();call(ThreadedPipelineMixin.class,pipeline,"proof$task",successTask);if(calls.get()!=4)throw new AssertionError("task count");
  if(session.observerFaults.get()<10||!session.bad.get())throw new AssertionError("diagnostic faults not explicit");
  System.out.println("PASS actual compiled mixin inert bodies: nullable/wrong enum/message/accessor/position, Error diagnostics, original list/supplier/task result/count/exactThrowable. No native World/engine/scheduler instantiated; native application=PENDING");
 }
}
