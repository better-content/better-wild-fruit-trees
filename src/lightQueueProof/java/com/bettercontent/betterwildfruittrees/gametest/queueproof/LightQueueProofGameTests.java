package com.bettercontent.betterwildfruittrees.gametest.queueproof;

import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Real mutative SOURCE experiments, never observations of the original growth callback. */
@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LightQueueProofGameTests {
 private static final String[] CASES={"SAME_CHUNK_STABLE_PRIORITY","CROSS_CHUNK_MIXED_PRIORITY","NEIGHBOR_PROPAGATION"};
 @GameTest(template="growth",batch="zz_light_queue_proof_v1",timeoutTicks=400)
 public static void sameChunkStablePriority(GameTestHelper h){start(h,0);}
 @GameTest(template="growth",batch="zz_light_queue_proof_v1",timeoutTicks=400)
 public static void crossChunkMixedPriority(GameTestHelper h){start(h,1);}
 @GameTest(template="growth",batch="zz_light_queue_proof_v1",timeoutTicks=400)
 public static void neighborPropagation(GameTestHelper h){start(h,2);}
 private static void start(GameTestHelper h,int which){
  h.assertTrue(Boolean.getBoolean("better_wild_fruit_trees.lightQueueProof"),"Queue proof source requested without explicit flag");
  for(String name:List.of("tconstruct","ars_nouveau","wild_addons"))h.assertTrue(Boolean.getBoolean("better_wild_fruit_trees.fixture."+name),"Queue proof requires default present fixture "+name);
  try{
   var expected=new TreeSet<String>();
   try(var in=LightQueueProofGameTests.class.getResourceAsStream("/light-queue-proof-required-tests.txt")){if(in==null)throw new IllegalStateException("missing required-29 inventory");expected.addAll(new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().filter(s->!s.isBlank()).toList());}
   var registry=new ArrayList<QueueProofRegistry.Entry>();
   for(TestFunction f:GameTestRegistry.getAllTestFunctions()){
    var template=net.minecraft.resources.ResourceLocation.tryParse(f.getStructureName());
    if(template==null)throw new IllegalStateException("Malformed native template identity "+f.getStructureName());
    registry.add(new QueueProofRegistry.Entry(f.getTestName(),template.getNamespace(),f.isRequired(),f.getMaxAttempts(),f.getRequiredSuccesses()));
   }
   QueueProofRegistry.validate(expected,registry,WildFruitTrees.MOD_ID);
   Properties binding=new Properties();try(var in=Files.newInputStream(Path.of("light-queue-proof-bindings.properties"))){binding.load(in);}
   h.assertTrue("light-queue-proof-v1-required29".equals(binding.getProperty("profile"))&&"false".equals(binding.getProperty("sourceDirty")),"Queue proof clean Source/profile binding");
   h.assertTrue("86ce0a0aa09201b566430dfec17f7a03fd2acbd16e04b17992b6ad5e78df4b0f".equals(binding.getProperty("sdkSha256")),"Queue proof SDK binding");
   h.assertTrue("exact3-native-ticking-v1".equals(binding.getProperty("fixturePreparation")),"Queue proof precise preparation binding");
   h.assertTrue("e2de9240ce39af783a83bcd315445b84d675c01e4655f3ad55c163071ce15fee".equals(binding.getProperty("preparationSdkSha256"))&&"7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264".equals(binding.getProperty("preparationRuntimeSha256")),"Queue proof preparation SDK binding");
   for(String key:List.of("classHashSetSha256","nativeInputReceiptSha256"))h.assertTrue(binding.getProperty(key,"").matches("[a-f0-9]{64}"),"Missing binding "+key);
   h.assertTrue(binding.getProperty("sourceCommit","").matches("[a-f0-9]{40}"),"Source commit binding");QueueProofLedger.boot(binding);
  }catch(java.io.IOException malformed){h.fail("Queue proof binding/inventory unavailable: "+malformed.getClass().getName());return;}
  h.assertTrue(QueueProofLedger.active(),"Queue proof already closed after an incomplete observation");
  System.out.println("LIGHT_QUEUE_PROOF_REGISTRY case="+CASES[which]+" required=29 original=26 proof=3 attempts=1 schema="+QueueProofLedger.SCHEMA);
  ready(h,which,h.getTick()+100);
 }
 private static void ready(GameTestHelper h,int which,long deadline){
  var level=h.getLevel();BlockPos base=h.absolutePos(new BlockPos(32,200,32));
  BlockPos pos=new BlockPos((base.getX()&~15)+15,base.getY(),(base.getZ()&~15)+8),other=pos.east();
  LevelChunk a=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4),b=level.getChunkSource().getChunkNow(other.getX()>>4,other.getZ()>>4);
  if(a==null||b==null||!a.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)||!b.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)){if(h.getTick()>=deadline){QueueProofLedger.flush(true);h.fail("Queue proof native BLOCK_TICKING chunks not ready within predeclared bound; no implicit chunk load");}else h.runAfterDelay(1,()->ready(h,which,deadline));return;}
  QueueProofLedger.intent(-2);
  try{level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(other.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);level.setBlock(other,Blocks.AIR.defaultBlockState(),2);}finally{QueueProofLedger.intent(-1);}
  h.runAfterDelay(5,()->observe(h,which,0,pos));
 }
 private static int lifecycle(LevelChunk chunk){return (chunk.isLightCorrect()?1:0)|(chunk.isUnsaved()?2:0);}
 private static void observe(GameTestHelper h,int which,int ordinal,BlockPos pos){
  int slot=which*2+ordinal;var level=h.getLevel();var source=level.getChunkSource();LevelChunk chunk=source.getChunkNow(pos.getX()>>4,pos.getZ()>>4);
  if(chunk==null||!chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)){QueueProofLedger.flush(true);h.fail("Queue proof chunk unloaded; no hidden reload");return;}
  var engine=source.getLightEngine();BlockPos other=which==0?pos.west():pos.east();
  int before=lifecycle(chunk),lightBefore=engine.getLayerListener(LightLayer.BLOCK).getLightValue(other);CompletableFuture<ChunkAccess> future;
  QueueProofLedger.intent(slot);
  try{
   level.setBlock(pos,ordinal==0?Blocks.GLOWSTONE.defaultBlockState():Blocks.STONE.defaultBlockState(),2);
   if(which==1)engine.updateSectionStatus(SectionPos.of(pos),false); // actual fixed-priority native route, NOT passive
   engine.checkBlock(other);
   future=engine.lightChunk(chunk,which!=2); // neighbor case explicitly exercises real false-arm propagation
  }finally{QueueProofLedger.intent(-1);}
  h.assertTrue(QueueProofLedger.submitted(slot)>=3,"Required original addTask issue hooks missing");
  poll(h,which,ordinal,pos,other,chunk,future,before,lightBefore,h.getTick()+100);
 }
 private static void poll(GameTestHelper h,int which,int ordinal,BlockPos pos,BlockPos other,LevelChunk original,CompletableFuture<ChunkAccess> future,int before,int lightBefore,long deadline){
  // This is an async GameTest step, not a main-thread join/managed fence or original growth call.
  h.getLevel().getChunkSource().getLightEngine().tryScheduleUpdate();
  if(!future.isDone()){
   if(h.getTick()>=deadline){QueueProofLedger.flush(true);h.fail("Queue proof native POST acknowledgement timeout; no retry/fence inference");}
   else h.runAfterDelay(1,()->poll(h,which,ordinal,pos,other,original,future,before,lightBefore,deadline));return;
  }
  if(future.isCompletedExceptionally()||future.isCancelled()){QueueProofLedger.flush(true);h.fail("Queue proof original native future failed");return;}
  h.assertTrue(future.getNow(null)==original,"Native acknowledgement changed chunk identity");
  int slot=which*2+ordinal,after=lifecycle(original),lightAfter=h.getLevel().getChunkSource().getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(other);
  QueueProofLedger.ack(slot,pos.asLong(),before,after,lightBefore,lightAfter);
  System.out.println("LIGHT_QUEUE_PROOF_OBSERVATION case="+CASES[which]+" ordinal="+ordinal+" schema="+QueueProofLedger.SCHEMA+" booleanArm="+(which!=2)+" lifecycleBefore="+before+" lifecycleAfter="+after+" lightBefore="+lightBefore+" lightAfter="+lightAfter+" structural=PASS "+QueueProofLedger.outcome(slot));
  QueueProofLedger.completed();
  if(ordinal==0)h.runAfterDelay(2,()->observe(h,which,1,pos));else{System.out.println("LIGHT_QUEUE_PROOF_CASE case="+CASES[which]+" observations=2 required=true inference=INCONCLUSIVE_NO_GLOBAL_ORDERING_PROOF");h.succeed();}
  if(QueueProofLedger.allCompleted())QueueProofLedger.flush(false);
 }
}
