package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import java.util.Properties;
/** Nonthrowing observer boundary. No original/native pipeline call is inside any catch below. */
public final class QueueProofLedger {
 public static final String SCHEMA=QueueProofLedgerEngine.SCHEMA;
 public static final int LIMIT=QueueProofLedgerEngine.LIMIT,TASKS=QueueProofLedgerEngine.TASKS,MAP=QueueProofLedgerEngine.MAP,THREADS=QueueProofLedgerEngine.THREADS,DEPTH=QueueProofLedgerEngine.DEPTH;
 private QueueProofLedger(){}
 private static void fault(){try{var s=QueueProofLedgerEngine.active;if(s!=null){s.bad.set(true);s.unknown.set(true);s.observerFaults.incrementAndGet();}}catch(Throwable ignored){}}
 public static void observerFault(){fault();}
 private static boolean slot(int value){if(value>=0&&value<6)return true;invalid();return false;}
 public static boolean active(){return QueueProofLedgerEngine.active();}
 public static void boot(Properties p){QueueProofLedgerEngine.boot(p);}
 public static void invalid(){try{QueueProofLedgerEngine.invalid();}catch(Throwable failure){fault();}}
 public static void intent(int s){try{QueueProofLedgerEngine.intent(s);}catch(Throwable failure){fault();}}
 /** Cold fixture lifetime; closes to UNKNOWN, never restores a previous case across async scheduling. */
 public static Intent intentScope(int slot){intent(slot);return new Intent();}
 public static final class Intent implements AutoCloseable {private Intent(){}public void close(){try{var s=QueueProofLedgerEngine.active;if(s!=null)s.intent=-1;}catch(Throwable failure){fault();}}}
 public static void issue(int e,int m,int x,int z,int t,int task){try{if(t<0||t>1)invalid();QueueProofLedgerEngine.issue(e,m,x,z,t,task);}catch(Throwable failure){fault();}}
 public static void originalSend(int i,int o){try{QueueProofLedgerEngine.originalSend(i,o);}catch(Throwable failure){fault();}}
 public static void originalThrowable(int p,int i,int c){try{QueueProofLedgerEngine.originalThrowable(p,i,c);}catch(Throwable failure){fault();}}
 public static void issueEnd(){try{QueueProofLedgerEngine.issueEnd();}catch(Throwable failure){fault();}}
 public static void sent(int f,int w,long p){try{QueueProofLedgerEngine.sent(f,w,p);}catch(Throwable failure){fault();}}
 public static void admissionBegin(int f){try{QueueProofLedgerEngine.admissionBegin(f);}catch(Throwable failure){fault();}}
 public static void admitted(long p,int l){try{QueueProofLedgerEngine.admitted(p,l);}catch(Throwable failure){fault();}}
 public static void admissionEnd(){try{QueueProofLedgerEngine.admissionEnd();}catch(Throwable failure){fault();}}
 public static void staged(int e,int t,int k){try{QueueProofLedgerEngine.staged(e,t,k);}catch(Throwable failure){fault();}}
 public static void run(int e,int n,int total){try{if(n<0||n>1000||total<n){invalid();return;}QueueProofLedgerEngine.run(e,n,total);}catch(Throwable failure){fault();}}
 public static void solver(boolean a){try{QueueProofLedgerEngine.solver(a);}catch(Throwable failure){fault();}}
 public static long taskBegin(int t){try{return QueueProofLedgerEngine.taskBegin(t);}catch(Throwable failure){fault();return 0;}}
 public static void taskEnd(long id,boolean returned){try{if(id<0||id>=TASKS){invalid();return;}QueueProofLedgerEngine.taskEnd(id,returned);}catch(Throwable failure){fault();}}
 public static void retired(){try{QueueProofLedgerEngine.retired();}catch(Throwable failure){fault();}}
 public static void runEnd(){try{QueueProofLedgerEngine.runEnd();}catch(Throwable failure){fault();}}
 public static void published(int i){try{QueueProofLedgerEngine.published(i);}catch(Throwable failure){fault();}}
 public static void lifecycle(int k,int i,int v){try{QueueProofLedgerEngine.lifecycle(k,i,v);}catch(Throwable failure){fault();}}
 public static void ticket(long p,boolean r,boolean l){try{QueueProofLedgerEngine.ticket(p,r,l);}catch(Throwable failure){fault();}}
 public static void notification(long p,boolean c){try{QueueProofLedgerEngine.notification(p,c);}catch(Throwable failure){fault();}}
 public static void callbackSent(int i,long p,int k){try{QueueProofLedgerEngine.callbackSent(i,p,k);}catch(Throwable failure){fault();}}
 public static void callback(int i,boolean e){try{QueueProofLedgerEngine.callback(i,e);}catch(Throwable failure){fault();}}
 public static int submitted(int s){try{return slot(s)?QueueProofLedgerEngine.submitted(s):0;}catch(Throwable failure){fault();return 0;}}
 public static void ack(int s,long p,int b,int a,int lb,int la){try{if(slot(s))QueueProofLedgerEngine.ack(s,p,b,a,lb,la);}catch(Throwable failure){fault();}}
 public static String outcome(int s){try{return slot(s)?QueueProofLedgerEngine.outcome(s):"invalid-slot incomplete=true";}catch(Throwable failure){fault();return "observer-fault incomplete=true";}}
 public static void completed(){try{QueueProofLedgerEngine.completed();}catch(Throwable failure){fault();}}
 public static boolean allCompleted(){try{return QueueProofLedgerEngine.allCompleted();}catch(Throwable failure){fault();return false;}}
 public static void flush(boolean failure){QueueProofLedgerEngine.flush(failure);}
}
