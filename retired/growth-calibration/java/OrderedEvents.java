package com.bettercontent.betterwildfruittrees.gametest;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Opt-in, nonshipping primitive observations. STOP never certifies measurement success. */
public final class OrderedEvents {
    public static final int SKY=1, SEE_BEGIN=2, SEE_END=3, BRIGHT_BEGIN=4, BRIGHT_END=5,
        ADEQUATE=6, WRITE_HEAD=7, WRITE_RETURN=8, STOP=9;
    public static final int LIMIT=524288, WORKERS=4096, DEPTH=256;
    private static final long MAGIC=0x5746544f52443031L;
    private static volatile Scope active;
    private static boolean enabled;
    private static Path directory;
    private static Properties binding;
    private static int allocationFailed=-1;
    private OrderedEvents() {}

    public static void configure(boolean flag, Path output, Properties properties) {
        enabled=flag; directory=null; binding=null;
        if (!flag) return;
        Properties copy=new Properties(); copy.putAll(properties);
        try {
            Path parent=Path.of(copy.getProperty("attemptDirectory", "")).toAbsolutePath().normalize();
            Path target=output.toAbsolutePath().normalize();
            String nonce=copy.getProperty("nonce", "");
            if (!UUID.fromString(nonce).toString().equals(nonce) || !parent.getFileName().toString().equals(nonce)
                || !target.equals(parent.resolve("original-events")) || Files.isSymbolicLink(parent)
                || Files.isSymbolicLink(target)) throw new IOException("not nonce-owned original-events directory");
            for (Path p=parent; p!=null; p=p.getParent()) if (Files.isSymbolicLink(p)) throw new IOException("symlink ancestry");
            Files.createDirectories(target);
            directory=target;
        } catch (IOException | RuntimeException invalid) { copy.setProperty("bindingError", invalid.getClass().getName()); }
        binding=copy;
    }
    public static Object scopeToken() { return active; }
    public static boolean ownerActive() { Scope s=active; return s!=null && s.owner==Thread.currentThread().getId(); }
    public static void begin(int sample) {
        if (!enabled) return;
        try { active=new Scope(sample,LIMIT,WORKERS); }
        catch (OutOfMemoryError unavailable) { active=null; allocationFailed=sample; }
    }
    public static void pulse(int pulse) { Scope s=active; if (s!=null && s.owner==Thread.currentThread().getId()) s.pulse=pulse; }
    public static void queryBegin(boolean brightness,long pos) { Scope s=owned(); if(s!=null)s.begin(brightness?BRIGHT_BEGIN:SEE_BEGIN,pos,0); }
    public static void queryEnd(boolean brightness,long pos,int value) { Scope s=owned(); if(s!=null)s.end(brightness?BRIGHT_END:SEE_END,pos,value,0,0); }
    public static void writeHead(long pos,long requested) { Scope s=owned(); if(s!=null)s.begin(WRITE_HEAD,pos,requested); }
    public static void writeReturn(long pos,long requested,long returned) { Scope s=owned(); if(s!=null)s.end(WRITE_RETURN,pos,returned==0?0:1,requested,returned); }
    public static void sky(long pos,int value,int identity,long before,long after) { Scope s=owned(); if(s!=null)s.add(SKY,pos,value,identity,(before<<32)|(after&0xffffffffL),s.top(),s.parent()); }
    public static void adequate(long pos,int result) { Scope s=owned(); if(s!=null)s.add(ADEQUATE,pos,result,0,0,s.top(),s.parent()); }
    public static void publication(Object token,int identity,long version) {
        Scope s=token instanceof Scope ? (Scope)token : null; if(s==null || s!=active)return;
        int index=s.reserved.getAndIncrement();
        if(index>=s.workerLimit){s.workerBad.set(true);return;}
        int at=index*4;
        s.worker[at]=s.pulse; s.worker[at+1]=identity; s.worker[at+2]=version; s.worker[at+3]=s.count;
        s.committed.set(index,index+1);
    }
    private static Scope owned(){Scope s=active;return s!=null&&s.owner==Thread.currentThread().getId()?s:null;}
    public static void finish(boolean aggregateComplete) {
        Scope s=owned(); active=null;
        if(s==null){if(allocationFailed>=0){try { System.out.println("ORIGINAL_EVENTS incomplete=true allocationFailedSample="+allocationFailed); } catch(Throwable unavailableReceipt) {} allocationFailed=-1;}return;}
        try {
            s.bad |= !aggregateComplete || s.depth!=0 || s.workerBad.get();
            s.add(STOP,0,s.bad?1:0,0,0,s.top(),s.parent());
            Properties metadata=new Properties(); if(binding!=null)metadata.putAll(binding);
            metadata.setProperty("format","WFTORD01"); metadata.setProperty("sample",Integer.toString(s.sample));
            metadata.setProperty("lastPulse",Integer.toString(s.pulse)); metadata.setProperty("scopeOutcome","UNPROVEN_REQUIRE_POSTRUN_GROWTH_SAMPLE");
            metadata.setProperty("workerSemantics","post-PUTFIELD observation; detached/no-barrier; late reservations possible");
            metadata.setProperty("identityHashesMayCollide","true");
            boolean missing=false;
            for(String key:List.of("nonce","profile","sourceCommit","classHashSetSha256","nativeInputReceiptSha256","dictSchema","dictScope","dictDigest"))
                if(metadata.getProperty(key,"").isBlank())missing=true;
            missing |= !metadata.getProperty("sourceCommit","").matches("[a-f0-9]{40}");
            for (String key : List.of("classHashSetSha256", "nativeInputReceiptSha256", "dictDigest"))
                missing |= !metadata.getProperty(key, "").matches("[a-f0-9]{64}");
            missing |= !metadata.getProperty("dictComplete","false").equals("true") || metadata.containsKey("bindingError")
                || !metadata.getProperty("sourceDirty","true").equals("false");
            int reservations=s.reserved.get(), n=Math.min(reservations,s.workerLimit);
            int[] commits=new int[n]; boolean uncommitted=false;
            for(int i=0;i<n;i++){commits[i]=s.committed.get(i);if(commits[i]!=i+1){s.bad=true;uncommitted=true;}}
            boolean workerOverflow=reservations>s.workerLimit||s.workerBad.get();
            boolean raced=s.reserved.get()!=reservations;
            s.bad |= workerOverflow||raced;
            metadata.setProperty("ownerOverflow",Boolean.toString(s.ownerOverflow));
            metadata.setProperty("nestedOverflow",Boolean.toString(s.nestedOverflow));
            metadata.setProperty("canonicalUnknown",Boolean.toString(s.unknown));
            metadata.setProperty("unpaired",Boolean.toString(s.unpaired));
            metadata.setProperty("workerOverflow",Boolean.toString(workerOverflow));
            metadata.setProperty("workerUncommitted",Boolean.toString(uncommitted));
            metadata.setProperty("workerReservationRacedSnapshot",Boolean.toString(raced));
            metadata.setProperty("aggregateIncomplete",Boolean.toString(!aggregateComplete));
            metadata.setProperty("ownerRecordCount",Integer.toString(s.count));
            metadata.setProperty("workerRecordCount",Integer.toString(n));
            metadata.setProperty("workerReservedCount",Integer.toString(reservations));
            metadata.setProperty("maxOwnerRecords",Integer.toString(LIMIT));
            metadata.setProperty("maxWorkerRecords",Integer.toString(WORKERS));
            metadata.setProperty("incomplete",Boolean.toString(s.bad||missing));
            metadata.setProperty("missingBinding",Boolean.toString(missing)); metadata.setProperty("danglingDepth",Integer.toString(s.depth));
            if(directory==null)throw new IOException("missing nonce-owned output binding");
            Path file=directory.resolve("sample-"+s.sample+".wftord");
            write(file,metadata,s.rows,s.count,s.worker,n,commits);
            System.out.println("ORIGINAL_EVENTS path="+file+" sha256="+fileHash(file)+" records="+s.count+" workerRecords="+n+" incomplete="+metadata.getProperty("incomplete")+" scopeOutcome=UNPROVEN");
        } catch (Throwable diagnostic) {
            // This catch surrounds observations/flush ONLY, never a gameplay invocation. Even
            // diagnostic allocation/appender failure must not replace an original pending Throwable.
            try { System.out.println("ORIGINAL_EVENTS sample="+s.sample+" incomplete=true ioError="+diagnostic.getClass().getName()); }
            catch (Throwable unavailableReceipt) { /* retain any CREATE_NEW partial file; no replacement */ }
        }
    }
    static final class Scope {
        final long owner=Thread.currentThread().getId(); final int sample,limit,workerLimit;
        final long[] rows,worker,ids=new long[DEPTH],positions=new long[DEPTH],requests=new long[DEPTH];
        final int[] kinds=new int[DEPTH];
        final AtomicInteger reserved=new AtomicInteger(); final AtomicIntegerArray committed; final AtomicBoolean workerBad=new AtomicBoolean();
        volatile int count,pulse; int depth; long next; boolean bad,ownerOverflow,nestedOverflow,unknown,unpaired;
        Scope(int sample,int limit,int workerLimit){this.sample=sample;this.limit=limit;this.workerLimit=workerLimit;rows=new long[limit*8];worker=new long[workerLimit*4];committed=new AtomicIntegerArray(workerLimit);}
        long top(){return depth==0?0:ids[depth-1];} long parent(){return depth<2?0:ids[depth-2];}
        void begin(int kind,long pos,long request){
            if(depth>=DEPTH){bad=true;nestedOverflow=true;return;} long parent=top(),id=++next;
            ids[depth]=id;positions[depth]=pos;requests[depth]=request;kinds[depth]=kind;depth++;
            if(kind==WRITE_HEAD&&request==Long.MIN_VALUE){bad=true;unknown=true;}
            add(kind,pos,0,request,0,id,parent);
        }
        void end(int kind,long pos,long value,long request,long returned){
            int expected=kind==SEE_END?SEE_BEGIN:kind==BRIGHT_END?BRIGHT_BEGIN:WRITE_HEAD;
            if(depth==0||kinds[depth-1]!=expected||positions[depth-1]!=pos){bad=true;unpaired=true;return;}
            if(kind==WRITE_RETURN){
                if(requests[depth-1]!=request){bad=true;unpaired=true;}
                if(request==Long.MIN_VALUE||returned==Long.MIN_VALUE){bad=true;unknown=true;}
            }
            long id=top(),parent=parent();depth--;
            add(kind,pos,value,request,returned,id,parent);
        }
        void add(int kind,long pos,long value,long requested,long returned,long id,long parent){
            if(count>=limit){bad=true;ownerOverflow=true;return;}int at=count*8;
            rows[at]=kind;rows[at+1]=pulse;rows[at+2]=pos;rows[at+3]=value;rows[at+4]=requested;rows[at+5]=returned;rows[at+6]=id;rows[at+7]=parent;count++;
        }
    }
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] bytes){return HexFormat.of().formatHex(bytes);}
    private static String fileHash(Path file)throws IOException{
        MessageDigest hash=sha();byte[] buffer=new byte[16384];
        try(InputStream in=Files.newInputStream(file)){for(int n;(n=in.read(buffer))!=-1;)hash.update(buffer,0,n);}
        return hex(hash.digest());
    }
    static void write(Path file,Properties metadata,long[] rows,int count,long[] worker,int workerCount,int[] committed)throws IOException{
        ByteArrayOutputStream header=new ByteArrayOutputStream(); metadata.store(header,"nonshipping observation; outcome unproven");byte[] h=header.toByteArray();
        try(OutputStream raw=new BufferedOutputStream(Files.newOutputStream(file,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE),65536)){
            MessageDigest hash=sha();DigestOutputStream digest=new DigestOutputStream(raw,hash);DataOutputStream out=new DataOutputStream(digest);
            out.writeLong(MAGIC);out.writeInt(h.length);out.write(h);out.writeInt(count);out.writeInt(workerCount);
            for(int i=0;i<count*8;i++)out.writeLong(rows[i]);
            for(int i=0;i<workerCount;i++){out.writeInt(committed[i]);for(int j=0;j<4;j++)out.writeLong(committed[i]==i+1?worker[i*4+j]:0);}
            out.flush();raw.write(hash.digest());
        }
    }
    public record Capture(Properties metadata,long[] rows,int count) {}
    public static Capture read(Path file)throws IOException{
        long size=Files.size(file);if(size<52||size>40L*1024*1024)throw new IOException("size bound/truncation");byte[] bytes=Files.readAllBytes(file);
        byte[] expected=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length),actual=sha().digest(Arrays.copyOf(bytes,bytes.length-32));
        if(!MessageDigest.isEqual(expected,actual))throw new IOException("payload SHA256 mismatch");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-32))){
            if(in.readLong()!=MAGIC)throw new IOException("format");int length=in.readInt();if(length<0||length>65536)throw new IOException("header bound");
            byte[] h=in.readNBytes(length);if(h.length!=length)throw new EOFException();Properties p=new Properties();p.load(new ByteArrayInputStream(h));
            int n=in.readInt(),w=in.readInt();if(n<1||n>LIMIT||w<0||w>WORKERS||in.available()!=n*64+w*36)throw new IOException("record/truncation bound");
            long[] rows=new long[n*8];for(int i=0;i<rows.length;i++)rows[i]=in.readLong();
            for(int i=0;i<w;i++){int commit=in.readInt();if(commit!=i+1)throw new IOException("uncommitted worker");for(int j=0;j<4;j++)in.readLong();}
            validate(rows,n);if(!p.getProperty("incomplete","true").equals("false"))throw new IOException("diagnostic incomplete");
            return new Capture(p,rows,n);
        }
    }
    static void validate(long[] rows,int n)throws IOException{
        long[] ids=new long[DEPTH],pos=new long[DEPTH],request=new long[DEPTH];int[] kinds=new int[DEPTH];int depth=0;long next=0;int pulse=0;
        for(int i=0;i<n;i++){int at=i*8,k=(int)rows[at];if(rows[at+1]<pulse||rows[at+1]>4096)throw new IOException("pulse order");pulse=(int)rows[at+1];
            long parent=depth==0?0:ids[depth-1];
            if(k==SEE_BEGIN||k==BRIGHT_BEGIN||k==WRITE_HEAD){if(depth==DEPTH||rows[at+6]!=++next||rows[at+7]!=parent)throw new IOException("begin order/depth");ids[depth]=next;pos[depth]=rows[at+2];request[depth]=rows[at+4];kinds[depth++]=k;}
            else if(k==SEE_END||k==BRIGHT_END||k==WRITE_RETURN){int wanted=k==SEE_END?SEE_BEGIN:k==BRIGHT_END?BRIGHT_BEGIN:WRITE_HEAD;if(depth==0||kinds[depth-1]!=wanted||ids[depth-1]!=rows[at+6]||pos[depth-1]!=rows[at+2]||rows[at+7]!=(depth<2?0:ids[depth-2]))throw new IOException("unpaired return");if(k==WRITE_RETURN&&(request[depth-1]!=rows[at+4]||rows[at+4]==Long.MIN_VALUE||rows[at+5]==Long.MIN_VALUE))throw new IOException("unknown/write pairing");depth--;}
            else if(k==STOP){if(i!=n-1||depth!=0||rows[at+3]!=0)throw new IOException("dangling/stop");}
            else if(k!=SKY&&k!=ADEQUATE)throw new IOException("unknown kind");
            else if(rows[at+6]!=parent||rows[at+7]!=(depth<2?0:ids[depth-2]))throw new IOException("point context");
        }
        if(rows[(n-1)*8]!=STOP||depth!=0)throw new IOException("missing STOP");
    }
}
