package com.bettercontent.betterwildfruittrees.gametest;

import java.nio.file.*;
import java.util.*;

/** Synthetic correlation controls, explicitly not log authentication or native acceptance. */
public final class CompletionReceiptSelfTest {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]).toAbsolutePath();Files.createDirectories(root);
        Path attempt=root.resolve(UUID.randomUUID().toString());Files.createDirectory(attempt);
        Path events=attempt.resolve("original-events");Files.createDirectory(events);
        Properties p=OrderedEventsSelfTest.metadata();
        p.setProperty("nonce",attempt.getFileName().toString());p.setProperty("attemptDirectory",attempt.toString());
        p.setProperty("sourceCommit","a".repeat(40));p.setProperty("sourceDirty","false");p.setProperty("sample","1");p.setProperty("lastPulse","1");p.setProperty("workerRecordCount","0");
        Path manifest=attempt.resolve("original-events-classes.sha256"),input=attempt.resolve("before-own-prepare.properties");
        Files.writeString(manifest,"synthetic class manifest\n");Files.writeString(input,"synthetic input receipt\n");
        p.setProperty("classHashSetSha256",OriginalEventCompare.hash(manifest));p.setProperty("nativeInputReceiptSha256",OriginalEventCompare.hash(input));
        try(var out=Files.newOutputStream(attempt.resolve("original-events-bindings.properties"))){p.store(out,"synthetic only");}
        Path trace=events.resolve("sample-1.wftord");OrderedEventsSelfTest.save(trace,OrderedEventsSelfTest.scenario(15,true,11,false),p);
        var capture=OrderedEvents.read(trace);
        String receipt="ORIGINAL_EVENTS path="+trace+" sha256="+OriginalEventCompare.hash(trace)+" records="+capture.count()+" workerRecords=0 incomplete=false scopeOutcome=UNPROVEN\n";
        String marker="GROWTH_SAMPLE orange seed=1 pulses=1\n";
        String[] negatives={marker,receipt.replace(trace.toString(),trace.resolveSibling("sample-2.wftord").toString())+marker,
            receipt.replace(OriginalEventCompare.hash(trace),"0".repeat(64))+marker,
            receipt+receipt+marker,receipt+marker+marker,marker+receipt,
            receipt.replace("incomplete=false","incomplete=true")+marker,
            receipt.replace("records="+capture.count(),"records="+(capture.count()+1))+marker,
            receipt+marker.replace("pulses=1","pulses=2")};
        for(int i=0;i<negatives.length;i++){
            Path log=attempt.resolve("negative-"+i+".log");Files.writeString(log,negatives[i],StandardOpenOption.CREATE_NEW);
            OrderedEventsSelfTest.rejects(()->OriginalEventCompare.completion(capture,trace,log,OriginalEventCompare.hash(log)));
        }
        Path positive=attempt.resolve("positive.log");Files.writeString(positive,"[INFO] "+receipt+"intervening aggregate lines\n[INFO] "+marker,StandardOpenOption.CREATE_NEW);
        OriginalEventCompare.completion(capture,trace,positive,OriginalEventCompare.hash(positive));
        OrderedEventsSelfTest.rejects(()->OriginalEventCompare.completion(capture,trace,positive,"0".repeat(64)));
        System.out.println("PASS receipt correlation positive + marker-only/wrong-path/wrong-SHA/duplicate-receipt/duplicate-marker/order/incomplete/count/pulse/log-SHA negatives; NOT authentication");
    }
}
