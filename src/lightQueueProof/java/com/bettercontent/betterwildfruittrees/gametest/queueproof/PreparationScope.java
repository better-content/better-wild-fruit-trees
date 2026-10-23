package com.bettercontent.betterwildfruittrees.gametest.queueproof;

/** Cold fixture scope, not the observation ledger. Every wrapper closes even on original failure. */
public final class PreparationScope implements AutoCloseable {
 private enum Kind { INFO,SPAWN }
 private static final ThreadLocal<PreparationScope> CURRENT=new ThreadLocal<>();
 private final PreparationScope previous;
 private final long owner=Thread.currentThread().getId();
 private final Kind kind;
 private final boolean qualified;
 private boolean used,closed;
 private PreparationScope(Kind kind,boolean qualified){this.previous=CURRENT.get();this.kind=kind;this.qualified=qualified;CURRENT.set(this);}
 public static PreparationScope info(String name,String structure){
  return new PreparationScope(Kind.INFO,QueueProofRegistry.PROOF_NAMES.contains(name)&&"better_wild_fruit_trees:growth".equals(structure));
 }
 public static PreparationScope spawn(){
  PreparationScope parent=CURRENT.get();boolean qualified=parent!=null&&parent.kind==Kind.INFO&&parent.qualified&&!parent.used;
  if(qualified)parent.used=true;
  return new PreparationScope(Kind.SPAWN,qualified);
 }
 public static boolean takeSpawnClear(){return take(Kind.SPAWN);}
 public static boolean takeInfoClear(){return take(Kind.INFO);}
 private static boolean take(Kind expected){PreparationScope scope=CURRENT.get();if(scope==null||scope.kind!=expected||!scope.qualified)return false;if(scope.used)throw new IllegalStateException("Repeated qualified native clear");scope.used=true;return true;}
 public boolean qualified(){return qualified;}
 public static boolean active(){return CURRENT.get()!=null;}
 @Override public void close(){if(Thread.currentThread().getId()!=owner)throw new IllegalStateException("Preparation scope wrong thread");if(closed)throw new IllegalStateException("Scope closed twice");closed=true;boolean correct=CURRENT.get()==this;if(previous==null)CURRENT.remove();else CURRENT.set(previous);if(!correct)throw new IllegalStateException("Unbalanced native preparation scope");}
}
