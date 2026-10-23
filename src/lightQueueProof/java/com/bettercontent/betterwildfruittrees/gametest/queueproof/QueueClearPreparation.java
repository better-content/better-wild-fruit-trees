package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin.*;
import com.mojang.datafixers.util.Either;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.*;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
/** Mutative cold fixture preparation, explicitly NOT an observer/light fence. */
public final class QueueClearPreparation {
 public static AuditLease prepare(BoundingBox bounds,ServerLevel world){
  PreparationAudit.record("BOUNDS_ENTRY scope="+PreparationScope.scopeId()+" bounds="+bounds.minX()+","+bounds.minY()+","+bounds.minZ()+":"+bounds.maxX()+","+bounds.maxY()+","+bounds.maxZ());
  java.util.List<QueueClearPolicy.Tile> tiles;
  try{tiles=QueueClearPolicy.footprint(bounds.minX(),bounds.minZ(),bounds.maxX(),bounds.maxZ());}catch(Throwable original){PreparationAudit.record("FOOTPRINT_REJECTED scope="+PreparationScope.scopeId()+" incomplete=true beforeAdmission=true");throw original;}
  var audit=new PreparationLeaseAudit(PreparationScope.scopeId(),tiles);
  audit.entry(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ());
  Port port=null;
  try{
   if(!Boolean.getBoolean("better_wild_fruit_trees.lightQueueProof")||!(world.getServer() instanceof GameTestServer))throw new IllegalStateException("Wrong preparation profile/server");
   port=new Port(world,audit);audit.before=mask(port);audit.beforeKnown=true;audit.row("BEFORE_ADMISSION");
   var lease=QueueClearPolicy.prepare(port,tiles);
   try{audit.readyForced=mask(port);audit.readyKnown=true;}catch(Throwable diagnostic){audit.incomplete=true;}
   if(audit.ready!=audit.full||!audit.readyKnown||audit.readyForced!=audit.full)audit.incomplete=true;audit.row("READY");return new AuditLease(lease,port,audit);
  }catch(Throwable original){after(port,audit,"PREPARE_FAILED_AFTER_POLICY_CLEANUP",true);throw original;}
 }
 private record Port(ServerLevel world,ServerChunkCache cache,PreparationMapAccess map,BlockableEventLoop<Runnable> loop,PreparationLeaseAudit audit) implements QueueClearPolicy.Port {
  Port(ServerLevel world,PreparationLeaseAudit audit){this(world,world.getChunkSource(),(PreparationMapAccess)world.getChunkSource().chunkMap,((PreparationMapAccess)world.getChunkSource().chunkMap).proof$executor(),audit);}
  public boolean onThread(){return loop.isSameThread();}public long now(){return System.nanoTime();}
  public boolean forced(QueueClearPolicy.Tile tile){return world.getForcedChunks().contains(ChunkPos.asLong(tile.x(),tile.z()));}
  public void force(QueueClearPolicy.Tile tile,boolean value){if(value)audit.attempted(tile);if(!world.setChunkForced(tile.x(),tile.z(),value))throw new IllegalStateException("Native forced ticket result");}
  public void admit(){((PreparationCacheAccess)cache).proof$admit();}
  public void pump(BooleanSupplier stop){loop.managedBlock(stop);}
  public QueueClearPolicy.Stage snapshot(QueueClearPolicy.Tile tile){long key=ChunkPos.asLong(tile.x(),tile.z());ChunkHolder holder=map.proof$visible(key);if(holder==null||!holder.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))throw new IllegalStateException("No native BLOCK_TICKING holder");var stage=new Stage(this,key,holder,holder.getTickingChunkFuture());stage.done();return stage;}
 }
 private record Stage(Port port,long key,ChunkHolder holder,CompletableFuture<Either<LevelChunk,ChunkHolder.ChunkLoadingFailure>> future) implements QueueClearPolicy.Stage {
  public boolean done(){if(!future.isDone())return false;successful();return true;}
  private LevelChunk successful(){var result=future.getNow(null);if(result==null||result.left().isEmpty())throw new IllegalStateException("Native TICKING future Right/null/error");return result.left().orElseThrow();}
  public void verify(){if(!port.onThread()||!future.isDone()||port.map.proof$visible(key)!=holder||holder.getTickingChunkFuture()!=future||!holder.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))throw new IllegalStateException("Stale native holder/TICKING future");LevelChunk chunk=successful();if(chunk.getPos().toLong()!=key||chunk.getLevel()!=port.world)throw new IllegalStateException("Wrong ready chunk/world");port.audit.ready(key);}
 }
 private static long mask(Port port){long mask=0;for(int i=0;i<port.audit.tiles.size();i++)if(port.forced(port.audit.tiles.get(i)))mask|=1L<<i;return mask;}
 private static void after(Port port,PreparationLeaseAudit audit,String phase,boolean failed){try{audit.incomplete|=failed;if(port!=null){audit.after=mask(port);audit.afterKnown=true;if(audit.beforeKnown&&audit.after!=audit.before)audit.incomplete=true;}else audit.incomplete=true;}catch(Throwable diagnostic){audit.incomplete=true;}finally{audit.row(phase);}}
 public static final class AuditLease implements AutoCloseable {
  private QueueClearPolicy.Lease lease;private Port port;private final PreparationLeaseAudit audit;
  private AuditLease(QueueClearPolicy.Lease lease,Port port,PreparationLeaseAudit audit){this.lease=lease;this.port=port;this.audit=audit;}
  public void close(){QueueClearPolicy.Lease local=lease;Port nativePort=port;lease=null;port=null;if(local==null)return;boolean failed=true;try{local.close();failed=false;}finally{after(nativePort,audit,failed?"LEASE_CLEANUP_FAILED":"LEASE_CLOSED",failed);}}
 }
 private QueueClearPreparation(){}
}
