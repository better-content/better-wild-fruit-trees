package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import java.nio.file.*;
import java.security.*;
import java.util.*;
/** Bounded COLD fixture metadata only. Never called by a light observer; no terrain/BE queries. */
public final class PreparationAudit {
 private static final String[] rows=new String[1024];private static int count,scopeSequence;private static boolean incomplete;
 public static synchronized int nextScopeId(){if(scopeSequence>=1000000){incomplete=true;return 0;}return ++scopeSequence;}
 public static synchronized void record(String text){try{if(count>=rows.length){incomplete=true;return;}if(text.length()>2048){incomplete=true;text=text.substring(0,2048);}rows[count++]=text;}catch(Throwable ignored){incomplete=true;}}
 public static synchronized void flush(){try{
  Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();Properties p=new Properties();try(var in=Files.newInputStream(root.resolve("light-queue-proof-bindings.properties"))){p.load(in);}
  write(root,p);
 }catch(Throwable failure){try{System.out.println("LIGHT_QUEUE_PREPARATION_RECEIPT incomplete=true failure="+failure.getClass().getName());}catch(Throwable ignored){}}}
 static Path write(Path root,Properties p)throws Exception {
  for(Path ancestor=root;ancestor!=null;ancestor=ancestor.getParent())if(Files.isSymbolicLink(ancestor))throw new IllegalStateException("audit symlink ancestry");
  if(!root.getFileName().toString().equals(p.getProperty("nonce"))||!root.toString().equals(p.getProperty("attemptDirectory")))throw new IllegalStateException("audit nonce binding");
  if(!"false".equals(p.getProperty("sourceDirty"))||!p.getProperty("sourceCommit","").matches("[a-f0-9]{40}")||!"light-queue-proof-v1-required29".equals(p.getProperty("profile")))throw new IllegalStateException("audit source binding");
  for(String[] binding:new String[][]{{"classHashSetSha256","light-queue-proof-classes.sha256"},{"nativeInputReceiptSha256","before-own-prepare.properties"}}){Path input=root.resolve(binding[1]);if(Files.isSymbolicLink(input)||!digest(input).equals(p.getProperty(binding[0])))throw new IllegalStateException("audit input binding");}
  p.setProperty("schema","light-queue-preparation-audit-v1");p.setProperty("incomplete",Boolean.toString(incomplete));p.setProperty("rows",Integer.toString(count));p.setProperty("globalDrainProven","false");for(int i=0;i<count;i++)p.setProperty("event."+i,rows[i]);
  Path file=root.resolve("light-queue-proof-preparation.properties");try(var out=Files.newOutputStream(file,StandardOpenOption.CREATE_NEW)){p.store(out,"COLD source fixture scope/footprint/ticking/lease facts; not warning closure");}
  System.out.println("LIGHT_QUEUE_PREPARATION_RECEIPT path="+file+" sha256="+digest(file)+" rows="+count+" incomplete="+incomplete);return file;
 }
 private static String digest(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
 private PreparationAudit(){}
}
