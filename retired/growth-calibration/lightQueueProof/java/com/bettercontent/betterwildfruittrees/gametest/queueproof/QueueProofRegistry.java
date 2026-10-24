package com.bettercontent.betterwildfruittrees.gametest.queueproof;

import java.util.*;

/** Cold fixture registry oracle; names are native unqualified names, ownership is template namespace. */
public final class QueueProofRegistry {
 public record Entry(String name,String templateNamespace,boolean required,int attempts,int successes) {}
 public static final Set<String> PROOF_NAMES=Set.of("samechunkstablepriority","crosschunkmixedpriority","neighborpropagation");
 public static void validate(Set<String> expected,Collection<Entry> registry,String namespace){
  if(expected.size()!=29||!expected.containsAll(PROOF_NAMES))throw new IllegalStateException("Expected literal original26 + proof3 inventory");
  Set<String> actual=new TreeSet<>();int owned=0;
  for(Entry entry:registry){
   if(!namespace.equals(entry.templateNamespace()))continue;
   owned++;
   if(!entry.required()||entry.attempts()!=1||entry.successes()!=1)throw new IllegalStateException("Required/no-retry registration drift "+entry.name());
   if(!actual.add(entry.name()))throw new IllegalStateException("Duplicate native test identity "+entry.name());
  }
  if(owned!=29||!actual.equals(expected))throw new IllegalStateException("Required original26 + proof3 registration drift expected="+expected+" actual="+actual+" owned="+owned);
 }
 private QueueProofRegistry(){}
}
