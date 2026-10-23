package com.bettercontent.betterwildfruittrees.gametest;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Pure JDK authored observations, not native acceptance. All output retained. */
public final class OrderedEventsSelfTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    interface Throwing{void run()throws Exception;}
    static void rejects(Throwing action)throws Exception{try{action.run();throw new AssertionError("expected rejection");}catch(IOException expected){}}
    static Properties metadata(){Properties p=new Properties();for(String k:List.of("format","sample","profile","sourceCommit","classHashSetSha256","dictSchema","dictScope","dictDigest","dictComplete"))p.setProperty(k,"same");p.setProperty("incomplete","false");return p;}
    static OrderedEvents.Scope scenario(int value,boolean adequate,long request,boolean writeFirst){
        var s=new OrderedEvents.Scope(1,128,4);s.pulse=1;
        if(writeFirst)s.begin(OrderedEvents.WRITE_HEAD,20,request);
        s.begin(OrderedEvents.BRIGHT_BEGIN,10,0);s.add(OrderedEvents.SKY,10,value,123,456,s.top(),s.parent());s.end(OrderedEvents.BRIGHT_END,10,value,0,0);
        s.add(OrderedEvents.ADEQUATE,10,adequate?1:0,0,0,s.top(),s.parent());
        if(!writeFirst)s.begin(OrderedEvents.WRITE_HEAD,20,request);
        s.end(OrderedEvents.WRITE_RETURN,20,1,request,5);s.add(OrderedEvents.STOP,0,0,0,0,0,0);return s;
    }
    static void save(Path p,OrderedEvents.Scope s,Properties meta)throws Exception{OrderedEvents.write(p,meta,s.rows,s.count,s.worker,0,new int[0]);}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);Files.createDirectories(root);
        var a=scenario(4,false,10,false);var b=scenario(15,true,11,false);Path ap=root.resolve("a.bin"),bp=root.resolve("b.bin");save(ap,a,metadata());save(bp,b,metadata());
        var ar=OrderedEvents.read(ap);var br=OrderedEvents.read(bp);
        check(OriginalEventCompare.compare(ar,br).startsWith("MECHANISM_SUPPORT"),"query/adequacy/HEAD positive");
        byte[] bytes=Files.readAllBytes(ap);rejects(()->save(ap,a,metadata()));check(Arrays.equals(bytes,Files.readAllBytes(ap)),"CREATE_NEW cannot replace");
        byte[] corrupt=bytes.clone();corrupt[30]^=1;Path bad=root.resolve("bad-sha.bin");Files.write(bad,corrupt,StandardOpenOption.CREATE_NEW);rejects(()->OrderedEvents.read(bad));
        Path cut=root.resolve("truncated.bin");Files.write(cut,Arrays.copyOf(bytes,bytes.length-1),StandardOpenOption.CREATE_NEW);rejects(()->OrderedEvents.read(cut));
        var first=scenario(4,false,10,true);var second=scenario(15,true,11,true);
        check(OriginalEventCompare.compare(new OrderedEvents.Capture(metadata(),first.rows,first.count),new OrderedEvents.Capture(metadata(),second.rows,second.count)).startsWith("INCONCLUSIVE"),"false light-first from RETURN-only rejected: different HEAD first");
        second=scenario(15,true,10,true);
        check(OriginalEventCompare.compare(new OrderedEvents.Capture(metadata(),first.rows,first.count),new OrderedEvents.Capture(metadata(),second.rows,second.count)).startsWith("INCONCLUSIVE"),"identical but in-flight HEAD is conservative");
        var nested=new OrderedEvents.Scope(1,64,4);nested.pulse=1;nested.begin(OrderedEvents.WRITE_HEAD,1,2);nested.begin(OrderedEvents.SEE_BEGIN,3,0);nested.begin(OrderedEvents.WRITE_HEAD,4,5);nested.end(OrderedEvents.WRITE_RETURN,4,1,5,6);nested.end(OrderedEvents.SEE_END,3,1,0,0);nested.end(OrderedEvents.WRITE_RETURN,1,1,2,7);nested.add(OrderedEvents.STOP,0,0,0,0,0,0);OrderedEvents.validate(nested.rows,nested.count);
        long[] swapped=nested.rows.clone();swapped[6]=9;rejects(()->OrderedEvents.validate(swapped,nested.count));
        var dangling=new OrderedEvents.Scope(1,8,1);dangling.pulse=1;dangling.begin(OrderedEvents.WRITE_HEAD,1,2);dangling.add(OrderedEvents.STOP,0,0,0,0,0,0);rejects(()->OrderedEvents.validate(dangling.rows,dangling.count));
        var returnsOnly=new OrderedEvents.Scope(1,8,1);returnsOnly.pulse=1;returnsOnly.add(OrderedEvents.WRITE_RETURN,1,1,2,3,1,0);returnsOnly.add(OrderedEvents.STOP,0,0,0,0,0,0);rejects(()->OrderedEvents.validate(returnsOnly.rows,returnsOnly.count));
        var small=new OrderedEvents.Scope(1,2,1);for(int i=0;i<3;i++)small.add(OrderedEvents.ADEQUATE,1,1,0,0,0,0);check(small.bad&&small.count==2,"bounded owner overflow");
        var depth=new OrderedEvents.Scope(1,512,1);for(int i=0;i<=OrderedEvents.DEPTH;i++)depth.begin(OrderedEvents.SEE_BEGIN,1,0);check(depth.bad&&depth.depth==OrderedEvents.DEPTH,"nested overflow");
        var unknown=scenario(4,false,Long.MIN_VALUE,false);check(unknown.bad,"UNKNOWN incomplete");
        Path worker=root.resolve("uncommitted.bin");OrderedEvents.write(worker,metadata(),a.rows,a.count,new long[4],1,new int[]{0});rejects(()->OrderedEvents.read(worker));
        Properties mismatch=metadata();mismatch.setProperty("dictDigest","different");rejects(()->OriginalEventCompare.compare(ar,new OrderedEvents.Capture(mismatch,b.rows,b.count)));
        Path attempt=root.resolve(UUID.randomUUID().toString());Files.createDirectory(attempt);Path dir=attempt.resolve("original-events");Properties p=metadata();
        p.setProperty("nonce",attempt.getFileName().toString());p.setProperty("attemptDirectory",attempt.toAbsolutePath().toString());p.setProperty("sourceCommit","a".repeat(40));p.setProperty("sourceDirty","false");p.setProperty("dictComplete","true");p.setProperty("dictDigest","c".repeat(64));
        Path manifest=attempt.resolve("original-events-classes.sha256"),receipt=attempt.resolve("before-own-prepare.properties");Files.writeString(manifest,"classes\n");Files.writeString(receipt,"receipt\n");p.setProperty("classHashSetSha256",OriginalEventCompare.hash(manifest));p.setProperty("nativeInputReceiptSha256",OriginalEventCompare.hash(receipt));
        try(var out=Files.newOutputStream(attempt.resolve("original-events-bindings.properties"))){p.store(out,"fixture");}
        OrderedEvents.configure(false,dir,p);OrderedEvents.begin(1);check(!OrderedEvents.ownerActive()&&!Files.exists(dir),"flag false no arrays/output");
        OrderedEvents.configure(true,dir,p);OrderedEvents.begin(1);OrderedEvents.pulse(1);OrderedEvents.queryBegin(true,10);OrderedEvents.sky(10,4,1,0,1);OrderedEvents.queryEnd(true,10,4);OrderedEvents.adequate(10,0);OrderedEvents.writeHead(20,10);OrderedEvents.writeReturn(20,10,5);OrderedEvents.publication(OrderedEvents.scopeToken(),1,1);OrderedEvents.finish(true);
        Path trace=dir.resolve("sample-1.wftord");var capture=OrderedEvents.read(trace);Path log=attempt.resolve("verification.log");Files.writeString(log,"no marker\n");rejects(()->OriginalEventCompare.completion(capture,trace,log,OriginalEventCompare.hash(log)));
        Files.writeString(log,"GROWTH_SAMPLE orange seed=1 pulses=1\n");OriginalEventCompare.completion(capture,trace,log,OriginalEventCompare.hash(log));rejects(()->OriginalEventCompare.completion(capture,trace,log,"0".repeat(64)));
        byte[] prior=Files.readAllBytes(trace);var failure=new IllegalStateException("original measurement Throwable");
        OrderedEvents.begin(1);OrderedEvents.pulse(1);OrderedEvents.queryBegin(false,10);
        try{try{throw failure;}finally{OrderedEvents.finish(true);}}catch(IllegalStateException caught){check(caught==failure,"original exception identity");}
        check(Arrays.equals(prior,Files.readAllBytes(trace)),"duplicate flush preserved previous evidence");
        OrderedEvents.begin(2);OrderedEvents.pulse(1);OrderedEvents.queryBegin(false,10);OrderedEvents.finish(true);rejects(()->OrderedEvents.read(dir.resolve("sample-2.wftord")));
        OrderedEvents.begin(3);OrderedEvents.pulse(1);Object workerToken=OrderedEvents.scopeToken();
        for(int i=0;i<=OrderedEvents.WORKERS;i++)OrderedEvents.publication(workerToken,1,i);
        OrderedEvents.finish(true);rejects(()->OrderedEvents.read(dir.resolve("sample-3.wftord")));
        OrderedEvents.begin(4);OrderedEvents.pulse(1);Object stale=OrderedEvents.scopeToken();((OrderedEvents.Scope)stale).reserved.set(1);
        OrderedEvents.finish(true);rejects(()->OrderedEvents.read(dir.resolve("sample-4.wftord")));
        OrderedEvents.begin(5);OrderedEvents.pulse(1);OrderedEvents.publication(stale,1,1);
        check(((OrderedEvents.Scope)OrderedEvents.scopeToken()).reserved.get()==0,"old callback contaminated next scope");OrderedEvents.finish(true);OrderedEvents.read(dir.resolve("sample-5.wftord"));
        Properties missing=new Properties();missing.putAll(p);missing.remove("dictDigest");OrderedEvents.configure(true,dir,missing);
        OrderedEvents.begin(6);OrderedEvents.pulse(1);OrderedEvents.finish(true);rejects(()->OrderedEvents.read(dir.resolve("sample-6.wftord")));
        OrderedEvents.configure(false,dir,p);
        System.out.println("Ordered controls: roundtrip/SHA/truncation/order/nested/dangling/overflow/UNKNOWN/uncommitted/flag=false/exception/CREATE_NEW/return-only/logbinding passed; not native acceptance");
    }
}
