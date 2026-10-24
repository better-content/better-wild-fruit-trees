package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class QueueProofNativeRegistry {
 public static void validate(){
  try(var in=QueueProofNativeRegistry.class.getResourceAsStream("/light-queue-proof-required-tests.txt")){
   if(in==null)throw new IllegalStateException("Missing literal required29 inventory");
   var expected=new TreeSet<String>(new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().filter(s->!s.isBlank()).toList());
   var registry=new ArrayList<QueueProofRegistry.Entry>();
   for(TestFunction f:GameTestRegistry.getAllTestFunctions()){
    var template=ResourceLocation.tryParse(f.getStructureName());if(template==null)throw new IllegalStateException("Malformed native structure identity");
    registry.add(new QueueProofRegistry.Entry(f.getTestName(),template.getNamespace(),f.isRequired(),f.getMaxAttempts(),f.getRequiredSuccesses()));
   }
   QueueProofRegistry.validate(expected,registry,WildFruitTrees.MOD_ID);
  }catch(java.io.IOException failure){throw new IllegalStateException("Cannot read native required29 inventory",failure);}
 }
 private QueueProofNativeRegistry(){}
}
