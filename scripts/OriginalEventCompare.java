package com.bettercontent.betterwildfruittrees.gametest;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.Pattern;

/** Direct JDK comparator. No Minecraft, no mutation, no acceptance verdict. */
public final class OriginalEventCompare {
    static String hash(Path p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
    static void completion(OrderedEvents.Capture capture,Path trace,Path log,String expected)throws Exception{
        if(!expected.matches("[a-f0-9]{64}")||!hash(log).equals(expected))throw new IOException("retained log SHA mismatch");
        Properties p=capture.metadata();Path attempt=trace.toAbsolutePath().normalize().getParent().getParent();
        if(!attempt.getFileName().toString().equals(p.getProperty("nonce"))||!attempt.toString().equals(p.getProperty("attemptDirectory")))throw new IOException("nonce directory binding");
        Properties original=new Properties();try(var in=Files.newInputStream(attempt.resolve("original-events-bindings.properties"))){original.load(in);}
        for(String key:List.of("nonce","profile","sourceCommit","classHashSetSha256","nativeInputReceiptSha256","attemptDirectory","sourceDirty"))
            if(!Objects.equals(p.getProperty(key),original.getProperty(key)))throw new IOException("binding mismatch: "+key);
        if(!p.getProperty("sourceCommit","").matches("[a-f0-9]{40}")||!p.getProperty("sourceDirty","").equals("false"))throw new IOException("source binding incomplete");
        if(!hash(attempt.resolve("original-events-classes.sha256")).equals(p.getProperty("classHashSetSha256"))
            ||!hash(attempt.resolve("before-own-prepare.properties")).equals(p.getProperty("nativeInputReceiptSha256")))throw new IOException("compiled/input receipt SHA mismatch");
        // Caller-supplied hashes bind bytes, not log authenticity. Correlate the exact
        // actual payload receipt and its later post-assert marker; never trust marker-only logs.
        String receiptPrefix="ORIGINAL_EVENTS path="+trace.toAbsolutePath().normalize()+" ";
        String receipt=receiptPrefix+"sha256="+hash(trace)+" records="+capture.count()
            +" workerRecords="+p.getProperty("workerRecordCount", "missing")+" incomplete=false scopeOutcome=UNPROVEN";
        Pattern receiptPattern=Pattern.compile("(?:^|.*\\s)"+Pattern.quote(receipt)+"\\s*$");
        String marker="GROWTH_SAMPLE orange seed="+p.getProperty("sample")+" pulses="+p.getProperty("lastPulse");
        Pattern markerPattern=Pattern.compile("(?:^|.*\\s)"+Pattern.quote(marker)+"\\s*$");
        int receipts=0,markers=0,receiptLine=-1,markerLine=-1,index=0;
        for(String line:Files.readAllLines(log)){
            if(line.contains(receiptPrefix)){receipts++;if(receiptPattern.matcher(line).matches())receiptLine=index;}
            if(markerPattern.matcher(line).matches()){markers++;markerLine=index;}
            index++;
        }
        if(receipts!=1||receiptLine<0)throw new IOException("scope payload unbound: require exactly one exact path/actual SHA/count/complete ORIGINAL_EVENTS receipt");
        if(markers!=1||markerLine<=receiptLine)throw new IOException("scope outcome unproven: require unique matching post-assert GROWTH_SAMPLE strictly after receipt");
    }
    static boolean equal(long[] a,int ai,long[] b,int bi){
        for(int j=0;j<8;j++){if(a[ai]==OrderedEvents.SKY&&(j==4||j==5))continue;if(a[ai+j]!=b[bi+j])return false;}return true;
    }
    static int adequate(long[] data,int n,int start,long pos){for(int i=start;i<n;i++){int at=i*8;if(data[at]==OrderedEvents.WRITE_HEAD)return -1;if(data[at]==OrderedEvents.ADEQUATE&&data[at+2]==pos)return i;}return -1;}
    static int nextHead(long[] data,int n,int start){for(int i=start;i<n;i++)if(data[i*8]==OrderedEvents.WRITE_HEAD)return i;return -1;}
    public static String compare(OrderedEvents.Capture a,OrderedEvents.Capture b)throws IOException{
        for(String key:List.of("format","sample","profile","sourceCommit","classHashSetSha256","dictSchema","dictScope","dictDigest","dictComplete"))
            if(a.metadata().getProperty(key)==null||!Objects.equals(a.metadata().getProperty(key),b.metadata().getProperty(key)))throw new IOException("incomparable binding: "+key);
        long[] x=a.rows(),y=b.rows();int n=Math.min(a.count(),b.count()),i=0,writeDepth=0;long queryPos=-1;
        for(;i<n;i++){if(!equal(x,i*8,y,i*8))break;if(x[i*8]==OrderedEvents.WRITE_HEAD)writeDepth++;if(x[i*8]==OrderedEvents.WRITE_RETURN)writeDepth--;}
        if(i==n)return "INCONCLUSIVE_IDENTICAL_PREFIX records="+i+" lengths="+a.count()+","+b.count();
        int at=i*8;String context=" index="+i+" pulse="+x[at+1]+" left="+Arrays.toString(Arrays.copyOfRange(x,Math.max(0,at-16),Math.min(x.length,at+32)))+" right="+Arrays.toString(Arrays.copyOfRange(y,Math.max(0,at-16),Math.min(y.length,at+32)));
        int kind=(int)x[at];
        if(x[at]!=y[at]||x[at+1]!=y[at+1]||x[at+2]!=y[at+2]||x[at+3]==y[at+3]||writeDepth!=0
            ||(kind!=OrderedEvents.SKY&&kind!=OrderedEvents.SEE_END&&kind!=OrderedEvents.BRIGHT_END))return "INCONCLUSIVE_FIRST_KIND_POSITION_REQUEST_OR_INFLIGHT_WRITE"+context;
        long id=x[at+6];
        for(int j=i-1;j>=0;j--)if(x[j*8+6]==id&&(x[j*8]==OrderedEvents.SEE_BEGIN||x[j*8]==OrderedEvents.BRIGHT_BEGIN)){queryPos=x[j*8+2];break;}
        if(queryPos==-1)return "INCONCLUSIVE_UNATTRIBUTED_SKY"+context;
        int ax=adequate(x,a.count(),i+1,queryPos),ay=adequate(y,b.count(),i+1,queryPos);
        if(ax<0||ay<0||x[ax*8+3]==y[ay*8+3])return "INCONCLUSIVE_NO_MATCHING_ADEQUACY_CHANGE"+context;
        int hx=nextHead(x,a.count(),ax+1),hy=nextHead(y,b.count(),ay+1);
        if(hx<0||hy<0||(x[hx*8+2]==y[hy*8+2]&&x[hx*8+4]==y[hy*8+4]))return "INCONCLUSIVE_NO_NEXT_REQUEST_CHANGE"+context;
        return "MECHANISM_SUPPORT_NOT_HISTORICAL_CAUSE samePositionQueryThenAdequacyThenDifferentWriteHEAD"+context;
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=6)throw new IllegalArgumentException("TRACE1 LOG1 LOG1_SHA256 TRACE2 LOG2 LOG2_SHA256");
        try{
            var a=OrderedEvents.read(Path.of(args[0]));var b=OrderedEvents.read(Path.of(args[3]));
            completion(a,Path.of(args[0]),Path.of(args[1]),args[2]);completion(b,Path.of(args[3]),Path.of(args[4]),args[5]);
            System.out.println("LOG_BINDINGS sha256="+args[2]+","+args[5]+" outcomes=receiptThenPostAssertCorrelatedNotAuthenticatedNotNativeGateWaiver");
            System.out.println(compare(a,b));
        }catch(IOException invalid){System.out.println("INCONCLUSIVE_INVALID_OR_INCOMPLETE "+invalid.getMessage());System.exit(2);}
    }
}
