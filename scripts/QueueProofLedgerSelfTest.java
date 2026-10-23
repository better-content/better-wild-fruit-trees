import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import java.nio.file.*;
import java.util.*;
import java.io.*;

/** Authored primitive records only; no game classes, private enum stubs or native claim. */
public final class QueueProofLedgerSelfTest {
 public static void main(String[] args)throws Exception{
  String mode=args[0];Path dir=Path.of(args[1]).toAbsolutePath();Files.createDirectory(dir);System.setProperty("user.dir",dir.toString());
  Properties p=new Properties();p.setProperty("nonce",dir.getFileName().toString());p.setProperty("attemptDirectory",dir.toString());p.setProperty("sourceCommit","a".repeat(40));p.setProperty("classHashSetSha256","b".repeat(64));p.setProperty("sdkSha256","c".repeat(64));p.setProperty("profile","primitive-JDK-control-not-native");if(mode.equals("binding"))p.remove("sourceCommit");QueueProofLedger.boot(p);
  for(int slot=0;slot<6;slot++){
   QueueProofLedger.intent(slot);
   for(int type=0;type<2;type++){int raw=100+slot*2+type,fn=200+slot*2+type;QueueProofLedger.issue(11,22,3,4,type,raw);QueueProofLedger.sent(fn,400+slot*2+type,((long)4<<32)|3);QueueProofLedger.originalSend(900+slot*2+type,0);QueueProofLedger.originalSend(900+slot*2+type,1);QueueProofLedger.issueEnd();QueueProofLedger.admissionBegin(fn);QueueProofLedger.admitted(((long)4<<32)|3,31);QueueProofLedger.admissionEnd();QueueProofLedger.staged(11,raw,type);}
   QueueProofLedger.intent(-1);QueueProofLedger.run(11,2,2);long pre=QueueProofLedger.taskBegin(100+slot*2);QueueProofLedger.taskEnd(pre,true);QueueProofLedger.solver(false);QueueProofLedger.published(33);QueueProofLedger.solver(true);long post=QueueProofLedger.taskBegin(101+slot*2);QueueProofLedger.lifecycle(0,44,1);QueueProofLedger.callbackSent(500+slot,((long)4<<32)|3,0);QueueProofLedger.taskEnd(post,true);QueueProofLedger.retired();QueueProofLedger.retired();QueueProofLedger.runEnd();QueueProofLedger.callback(500+slot,false);QueueProofLedger.ticket(((long)4<<32)|3,true,true);QueueProofLedger.callback(500+slot,true);QueueProofLedger.ack(slot,123,1,3,0,14);QueueProofLedger.completed();
   String facts=QueueProofLedger.outcome(slot);if(!facts.contains("submitted=2 admitted=2 staged=2 retired=2 ack=1")||!facts.contains("conditionalIncomplete=false"))throw new AssertionError(facts);
  }
  if(!QueueProofLedger.allCompleted())throw new AssertionError("six");
  switch(mode){
   case "normal":break;
   case "duplicate":QueueProofLedger.intent(0);QueueProofLedger.issue(11,22,3,4,0,100);QueueProofLedger.issueEnd();break;
   case "unknown-stage":QueueProofLedger.staged(11,9999,0);break;
   case "pending":QueueProofLedger.issue(11,22,3,4,0,9999);QueueProofLedger.issueEnd();break;
   case "throwable":QueueProofLedger.originalThrowable(1,777,888);break;
   case "overflow":for(int i=0;i<QueueProofLedger.LIMIT+1;i++)QueueProofLedger.lifecycle(0,44,1);break;
   case "binding":break;
   default:throw new AssertionError(mode);
  }
  QueueProofLedger.flush(false);if(QueueProofLedger.active())throw new AssertionError("must detach before output");
  Path output=dir.resolve("light-queue-proof/pipeline-v1.wftq");byte[] before=Files.readAllBytes(output);QueueProofLedger.flush(false);if(!Arrays.equals(before,Files.readAllBytes(output)))throw new AssertionError("overwrite");
  try(var in=new DataInputStream(Files.newInputStream(output))){if(in.readLong()!=0x5746545150523031L)throw new AssertionError("magic");int size=in.readInt();Properties meta=new Properties();meta.load(new ByteArrayInputStream(in.readNBytes(size)));if(!"false".equals(meta.getProperty("globalDrainProven"))||!"true".equals(meta.getProperty("incomplete")))throw new AssertionError(meta);
   boolean expected=!mode.equals("normal");if(Boolean.parseBoolean(meta.getProperty("conditionalTraceIncomplete"))!=expected)throw new AssertionError(meta);
   int n=in.readInt();if(n<1||n>QueueProofLedger.LIMIT)throw new AssertionError("bound");for(int i=0;i<n;i++){if(in.readInt()!=i+1)throw new AssertionError("commit");for(int j=0;j<8;j++)in.readLong();}if(in.read()!=-1)throw new AssertionError("tail");
  }
  System.out.println("PASS primitive-only mode="+mode+" nativeApplication=PENDING");
 }
}
