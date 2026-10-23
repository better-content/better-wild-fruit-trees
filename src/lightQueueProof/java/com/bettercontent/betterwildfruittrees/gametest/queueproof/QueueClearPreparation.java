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
 public static QueueClearPolicy.Lease prepare(BoundingBox bounds,ServerLevel world){
  if(!Boolean.getBoolean("better_wild_fruit_trees.lightQueueProof")||!(world.getServer() instanceof GameTestServer))throw new IllegalStateException("Wrong preparation profile/server");
  return QueueClearPolicy.prepare(new Port(world),QueueClearPolicy.footprint(bounds.minX(),bounds.minZ(),bounds.maxX(),bounds.maxZ()));
 }
 private record Port(ServerLevel world,ServerChunkCache cache,PreparationMapAccess map,BlockableEventLoop<Runnable> loop) implements QueueClearPolicy.Port {
  Port(ServerLevel world){this(world,world.getChunkSource(),(PreparationMapAccess)world.getChunkSource().chunkMap,((PreparationMapAccess)world.getChunkSource().chunkMap).proof$executor());}
  public boolean onThread(){return loop.isSameThread();}public long now(){return System.nanoTime();}
  public boolean forced(QueueClearPolicy.Tile tile){return world.getForcedChunks().contains(ChunkPos.asLong(tile.x(),tile.z()));}
  public void force(QueueClearPolicy.Tile tile,boolean value){if(!world.setChunkForced(tile.x(),tile.z(),value))throw new IllegalStateException("Native forced ticket result");}
  public void admit(){((PreparationCacheAccess)cache).proof$admit();}
  public void pump(BooleanSupplier stop){loop.managedBlock(stop);}
  public QueueClearPolicy.Stage snapshot(QueueClearPolicy.Tile tile){long key=ChunkPos.asLong(tile.x(),tile.z());ChunkHolder holder=map.proof$visible(key);if(holder==null||!holder.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))throw new IllegalStateException("No native BLOCK_TICKING holder");var stage=new Stage(this,key,holder,holder.getTickingChunkFuture());stage.done();return stage;}
 }
 private record Stage(Port port,long key,ChunkHolder holder,CompletableFuture<Either<LevelChunk,ChunkHolder.ChunkLoadingFailure>> future) implements QueueClearPolicy.Stage {
  public boolean done(){if(!future.isDone())return false;successful();return true;}
  private LevelChunk successful(){var result=future.getNow(null);if(result==null||result.left().isEmpty())throw new IllegalStateException("Native TICKING future Right/null/error");return result.left().orElseThrow();}
  public void verify(){if(!port.onThread()||!future.isDone()||port.map.proof$visible(key)!=holder||holder.getTickingChunkFuture()!=future||!holder.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))throw new IllegalStateException("Stale native holder/TICKING future");LevelChunk chunk=successful();if(chunk.getPos().toLong()!=key||chunk.getLevel()!=port.world)throw new IllegalStateException("Wrong ready chunk/world");}
 }
 private QueueClearPreparation(){}
}
