import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofRegistry;
import java.nio.file.*;
import java.util.*;

/** Pure registry records, never invented Minecraft API/classes or runtime registration proof. */
public final class QueueProofRegistrySelfTest {
 static final String NS="better_wild_fruit_trees";
 static void rejected(Set<String> expected,List<QueueProofRegistry.Entry> entries){try{QueueProofRegistry.validate(expected,entries,NS);}catch(IllegalStateException expectedFailure){return;}throw new AssertionError("bad registry accepted");}
 public static void main(String[] args)throws Exception{
  var literal=Files.readAllLines(Path.of(args[0]));Set<String> expected=new TreeSet<>(literal);if(literal.size()!=29||expected.size()!=29)throw new AssertionError("exact29");
  var good=new ArrayList<QueueProofRegistry.Entry>();for(String name:literal)good.add(new QueueProofRegistry.Entry(name,NS,true,1,1));
  QueueProofRegistry.validate(expected,good,NS);
  var foreign=new ArrayList<>(good);foreign.add(new QueueProofRegistry.Entry(literal.get(0),"other_mod",false,7,4));QueueProofRegistry.validate(expected,foreign,NS);
  var missing=new ArrayList<>(good);missing.remove(0);rejected(expected,missing);
  var extra=new ArrayList<>(good);extra.add(new QueueProofRegistry.Entry("unexpected_30th",NS,true,1,1));rejected(expected,extra);
  var duplicate=new ArrayList<>(good);duplicate.add(good.get(0));rejected(expected,duplicate);
  for(var bad:List.of(new QueueProofRegistry.Entry(literal.get(0),"minecraft",true,1,1),new QueueProofRegistry.Entry(literal.get(0),NS,false,1,1),new QueueProofRegistry.Entry(literal.get(0),NS,true,2,1),new QueueProofRegistry.Entry(literal.get(0),NS,true,1,2),new QueueProofRegistry.Entry(NS+":"+literal.get(0),NS,true,1,1),new QueueProofRegistry.Entry("class."+literal.get(0),NS,true,1,1))){var list=new ArrayList<>(good);list.set(0,bad);rejected(expected,list);}
  var wrongExpected=new TreeSet<>(expected);wrongExpected.remove(literal.get(0));rejected(wrongExpected,good);
  System.out.println("PASS exact29 native-method identity synthetic controls: namespace/extra/missing/duplicate/required/attempts/successes/name-shape; nativeRuntime=PENDING");
 }
}
