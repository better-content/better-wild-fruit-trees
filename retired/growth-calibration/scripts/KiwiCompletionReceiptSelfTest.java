package com.bettercontent.betterwildfruittrees.gametest;

import java.nio.file.*;
import java.util.*;

/** Synthetic binding/comparison negatives, NOT native history or authentication. */
public final class KiwiCompletionReceiptSelfTest {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]).toAbsolutePath(); Files.createDirectories(root);
        Path attempt=root.resolve(UUID.randomUUID().toString()); Files.createDirectory(attempt);
        Path events=attempt.resolve("original-events"); Files.createDirectory(events);
        Properties p=OrderedEventsSelfTest.metadata();
        p.setProperty("nonce",attempt.getFileName().toString()); p.setProperty("attemptDirectory",attempt.toString());
        p.setProperty("sourceCommit","a".repeat(40)); p.setProperty("sourceDirty","false");
        p.setProperty("sample","1"); p.setProperty("lastPulse","1"); p.setProperty("workerRecordCount","0");
        p.setProperty("dictSchema","original-state-v1"); p.setProperty("dictComplete","true"); p.setProperty("dictDigest","c".repeat(64));
        p.setProperty("dictScope","original-kiwi-blocks-v1"); p.setProperty("observedSpecies","kiwi");
        p.setProperty("traceScope","kiwi-v1"); p.setProperty("profile","main26:tcon=false:ars=false:wild=false:traceScope=kiwi-v1");
        Path manifest=attempt.resolve("original-events-classes.sha256"),input=attempt.resolve("before-own-prepare.properties");
        Files.writeString(manifest,"synthetic class manifest\n"); Files.writeString(input,"synthetic input receipt\n");
        p.setProperty("classHashSetSha256",OriginalEventCompare.hash(manifest)); p.setProperty("nativeInputReceiptSha256",OriginalEventCompare.hash(input));
        try(var out=Files.newOutputStream(attempt.resolve("original-events-bindings.properties"))){p.store(out,"synthetic only");}
        Path trace=events.resolve("sample-1.wftord"); OrderedEventsSelfTest.save(trace,OrderedEventsSelfTest.scenario(15,true,11,false),p);
        var capture=OrderedEvents.read(trace);
        String receipt="ORIGINAL_EVENTS path="+trace+" sha256="+OriginalEventCompare.hash(trace)+" records="+capture.count()+" workerRecords=0 incomplete=false scopeOutcome=UNPROVEN\n";
        String marker="GROWTH_SAMPLE kiwi seed=1 pulses=1\n";
        String[] negatives={marker, receipt+marker.replace("kiwi","orange"), marker+receipt, receipt+receipt+marker,
            receipt+marker+marker, receipt.replace("incomplete=false","incomplete=true")+marker,
            receipt.replace("sha256="+OriginalEventCompare.hash(trace),"sha256="+"0".repeat(64))+marker};
        for(int i=0;i<negatives.length;i++){
            Path log=attempt.resolve("negative-"+i+".log"); Files.writeString(log,negatives[i],StandardOpenOption.CREATE_NEW);
            OrderedEventsSelfTest.rejects(()->KiwiOriginalEventCompare.completion(capture,trace,log,OriginalEventCompare.hash(log)));
        }
        Path positive=attempt.resolve("positive.log"); Files.writeString(positive,receipt+marker,StandardOpenOption.CREATE_NEW);
        KiwiOriginalEventCompare.completion(capture,trace,positive,OriginalEventCompare.hash(positive));
        OrderedEventsSelfTest.rejects(()->OriginalEventCompare.completion(capture,trace,positive,OriginalEventCompare.hash(positive)));
        for(String key:List.of("dictSchema","dictComplete","dictDigest","dictScope","observedSpecies","traceScope","profile")){
            Properties bad=new Properties(); bad.putAll(capture.metadata()); bad.setProperty(key,"wrong");
            var invalid=new OrderedEvents.Capture(bad,capture.rows(),capture.count());
            OrderedEventsSelfTest.rejects(()->KiwiOriginalEventCompare.scope(invalid,trace));
        }
        Properties other=new Properties(); other.putAll(capture.metadata()); other.setProperty("profile","main26:tcon=true:ars=true:wild=true:traceScope=kiwi-v1");
        OrderedEventsSelfTest.rejects(()->OriginalEventCompare.compare(capture,new OrderedEvents.Capture(other,capture.rows(),capture.count())));
        for(String key:List.of("sourceCommit","classHashSetSha256","dictDigest")){
            Properties bad=new Properties(); bad.putAll(capture.metadata()); bad.setProperty(key,"different");
            OrderedEventsSelfTest.rejects(()->OriginalEventCompare.compare(capture,new OrderedEvents.Capture(bad,capture.rows(),capture.count())));
        }
        System.out.println("PASS kiwi exact scope/receipt/marker/source/dictionary and cross-profile negatives; NOT native acceptance");
    }
}
