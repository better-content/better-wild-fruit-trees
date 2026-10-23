import com.bettercontent.betterwildfruittrees.gametest.queueproof.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** JDK-only ports/scope contracts, not fake native APIs or Minecraft execution. */
public final class QueuePreparationSelfTest {
 static final List<QueueClearPolicy.Tile> TILES=List.of(new QueueClearPolicy.Tile(1,2),new QueueClearPolicy.Tile(2,2));
 static void check(boolean yes){if(!yes)throw new AssertionError();}
 static void reject(Runnable action){try{action.run();}catch(IllegalStateException|IllegalArgumentException|ArithmeticException expected){return;}throw new AssertionError("accepted invalid setup");}
 static final class Port implements QueueClearPolicy.Port {
  final Set<QueueClearPolicy.Tile> forced=new HashSet<>();final List<String> events=new ArrayList<>();
  String mode="";boolean admitted,ready,onThread=true;long clock;int forceCalls,cleanupCalls;
  public boolean onThread(){return onThread;}public long now(){return clock;}
  public boolean forced(QueueClearPolicy.Tile t){return forced.contains(t);}
  public void force(QueueClearPolicy.Tile t,boolean value){events.add(value?"add":"remove");if(value){forced.add(t);forceCalls++;if(mode.equals("partial-force")&&forceCalls==2)throw new IllegalStateException("after partial native add");}else{forced.remove(t);cleanupCalls++;if(mode.equals("cleanup"))throw new IllegalStateException("cleanup");}}
  public void admit(){events.add("admit");if(mode.equals("admit"))throw new IllegalStateException("admit");admitted=true;}
  public QueueClearPolicy.Stage snapshot(QueueClearPolicy.Tile tile){check(admitted);events.add("snapshot");if(mode.equals("snapshot"))throw new IllegalStateException("no holder");return new QueueClearPolicy.Stage(){
   public boolean done(){if(mode.equals("right")||mode.equals("exceptional")||mode.equals("cancelled"))throw new IllegalStateException(mode);return ready;}
   public void verify(){if(mode.equals("stale-holder")||mode.equals("stale-future")||mode.equals("wrong-world")||mode.equals("wrong-key"))throw new IllegalStateException(mode);check(ready);events.add("verify");}
  };}
  public void pump(BooleanSupplier stop){events.add("pump");if(mode.equals("reentry"))QueueClearPolicy.prepare(this,TILES);if(mode.equals("timeout")){clock=QueueClearPolicy.BUDGET_NANOS;check(stop.getAsBoolean());return;}if(mode.equals("early-pump"))return;ready=true;check(stop.getAsBoolean());}
 }
 static void scopes(){
  for(String name:QueueProofRegistry.PROOF_NAMES){try(var info=PreparationScope.info(name,"better_wild_fruit_trees:growth")){check(info.qualified());try(var spawn=PreparationScope.spawn()){
   try(var nested=PreparationScope.info("nativefreshgrowthcalibration","better_wild_fruit_trees:growth")){check(!nested.qualified());try(var ns=PreparationScope.spawn()){check(!PreparationScope.takeSpawnClear());}}
   try(var directNested=PreparationScope.spawn()){check(!PreparationScope.takeSpawnClear());}
   check(PreparationScope.takeSpawnClear());reject(()->PreparationScope.takeSpawnClear());
  }}check(!PreparationScope.active());}
  for(String template:List.of("other:growth","minecraft:growth","better_wild_fruit_trees:other","growth"))try(var info=PreparationScope.info("samechunkstablepriority",template)){check(!info.qualified());try(var spawn=PreparationScope.spawn()){check(!PreparationScope.takeSpawnClear());}}
  try(var info=PreparationScope.info("samechunkstablepriority","better_wild_fruit_trees:growth")){check(PreparationScope.takeInfoClear());reject(()->PreparationScope.takeInfoClear());}
  RuntimeException original=new RuntimeException("original");try{try(var info=PreparationScope.info("neighborpropagation","better_wild_fruit_trees:growth");var spawn=PreparationScope.spawn()){throw original;}}catch(RuntimeException caught){check(caught==original);}check(!PreparationScope.active());
  try(var info=PreparationScope.info("crosschunkmixedpriority","better_wild_fruit_trees:growth")){try{try(var spawn=PreparationScope.spawn()){throw original;}}catch(RuntimeException caught){check(caught==original);}try(var unrelated=PreparationScope.spawn()){check(!PreparationScope.takeSpawnClear());}}
  check(!PreparationScope.active());
 }
 static void footprint(){
  for(int x=-16;x<16;x++)for(int z=-16;z<16;z++){
   var actual=new HashSet<>(QueueClearPolicy.footprint(x,z,x+96,z+96));var expected=new HashSet<QueueClearPolicy.Tile>();
   for(int bx=x-2;bx<=x+99;bx++)for(int bz=z-3;bz<=z+99;bz++)expected.add(new QueueClearPolicy.Tile(Math.floorDiv(bx,16),Math.floorDiv(bz,16)));
   check(actual.equals(expected)&&actual.size()<=64);
  }
  reject(()->QueueClearPolicy.footprint(0,0,10000,10000));reject(()->QueueClearPolicy.footprint(1,0,0,0));reject(()->QueueClearPolicy.footprint(Integer.MIN_VALUE,0,0,0));reject(()->QueueClearPolicy.footprint(0,0,Integer.MAX_VALUE,0));
 }
 static void leases(){
  Port normal=new Port();normal.forced.add(TILES.get(0));try(var lease=QueueClearPolicy.prepare(normal,TILES)){check(normal.forced.size()==2);check(normal.events.indexOf("admit")<normal.events.indexOf("snapshot"));check(normal.events.indexOf("pump")<normal.events.indexOf("verify"));}check(normal.forced.equals(Set.of(TILES.get(0))));check(normal.forceCalls==1&&normal.cleanupCalls==1);
  for(String mode:List.of("partial-force","admit","snapshot","right","exceptional","cancelled","stale-holder","stale-future","wrong-world","wrong-key","timeout","early-pump","reentry")){
   Port bad=new Port();bad.mode=mode;reject(()->{try(var lease=QueueClearPolicy.prepare(bad,TILES)){}});check(bad.forced.isEmpty());
  }
  Port wrongThread=new Port();wrongThread.onThread=false;reject(()->QueueClearPolicy.prepare(wrongThread,TILES));check(wrongThread.forceCalls==0);
  Port body=new Port();RuntimeException original=new RuntimeException("original body");int[] calls={0};try{try(var lease=QueueClearPolicy.prepare(body,TILES)){calls[0]++;throw original;}}catch(RuntimeException actual){check(actual==original);}check(calls[0]==1&&body.forced.isEmpty());
  Port cleanup=new Port();cleanup.mode="cleanup";try{try(var lease=QueueClearPolicy.prepare(cleanup,TILES)){throw original;}}catch(RuntimeException actual){check(actual==original&&actual.getSuppressed().length==1);}check(cleanup.forced.isEmpty());
  Port standaloneCleanup=new Port();standaloneCleanup.mode="cleanup";reject(()->{try(var lease=QueueClearPolicy.prepare(standaloneCleanup,TILES)){}});check(standaloneCleanup.forced.isEmpty());
 }
 public static void main(String[] args){scopes();footprint();leases();System.out.println("PASS precise-scope/neutral-nested/one-use/foreign-namespace/97x97-negative-alignment/64cap/admit-before-futures/deadline/Right/stale/once/originalThrowable/finally/cleanup controls; native=PENDING");}
}
