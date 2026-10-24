import com.bettercontent.betterwildfruittrees.gametest.GrowthPlotCleanup;
import com.bettercontent.betterwildfruittrees.gametest.GrowthOwnedResources;
import java.util.*;

/** Fake-port regression, not native world/geometry acceptance. */
public final class GrowthCleanupSelfTest {
    static void check(boolean x) { if (!x) throw new AssertionError(); }
    static void raise(Throwable x) { if (x instanceof RuntimeException r) throw r; if (x != null) throw new AssertionError(x); }
    static final class Fake implements GrowthPlotCleanup.Port {
        boolean plot=true, blocks=true, ticks=true, foreign, failBlocks, failTicks, failEntity, failTicket, failProbe, reentrantDiscard;
        final Set<String> entities=new LinkedHashSet<>(List.of("old"));
        final List<String> tickets=new ArrayList<>(List.of("first","last")), calls=new ArrayList<>();
        final RuntimeException entityFailure=new IllegalStateException("entity"), ticketFailure=new IllegalStateException("ticket");
        public boolean plotOwned(){return plot;}
        public void checkForeignEntities(){if(foreign)throw new IllegalStateException("foreign preserved");}
        public void beforeClear(){calls.add("probe");if(failProbe)throw new IllegalStateException("probe");}
        public void clearBlocks(){calls.add("blocks");entities.add("during-clear");if(failBlocks)throw new IllegalStateException("block");blocks=false;}
        public void clearScheduledTicks(){calls.add("ticks");if(failTicks)throw new IllegalStateException("ticks");ticks=false;}
        public void drainOwnedEntities(){raise(GrowthOwnedResources.drain(entities,id->{calls.add("entity:"+id);if(failEntity&&id.equals("old"))throw entityFailure;if(reentrantDiscard&&id.equals("old"))entities.add("during-discard");}));}
        public void verifyPlotAndEntitiesEmpty(){checkForeignEntities();if(!entities.isEmpty()||(plot&&(blocks||ticks)))throw new IllegalStateException("dirty plot/entities");}
        public void markPlotReleased(){plot=false;}
        public void releaseTickets(){raise(GrowthOwnedResources.releaseAll(tickets,id->{calls.add("ticket:"+id);if(failTicket&&id.equals("first"))throw ticketFailure;}));}
        public void verifyResourcesReleased(){if(plot||!entities.isEmpty()||!tickets.isEmpty())throw new IllegalStateException("dirty resources");}
    }
    public static void main(String[] args) {
        // Reproduce the failed old ordering without a world: clearing can join after the drain.
        var old=new Fake();old.drainOwnedEntities();old.clearBlocks();check(old.entities.contains("during-clear"));
        var good=new Fake();check(GrowthPlotCleanup.run(good,null)==null);check(!good.plot&&good.entities.isEmpty()&&good.tickets.isEmpty());
        check(good.calls.indexOf("ticks")<good.calls.indexOf("entity:old"));
        var reentrant=new Fake();reentrant.reentrantDiscard=true;check(GrowthPlotCleanup.run(reentrant,null)==null);check(reentrant.calls.contains("entity:during-discard"));
        var orphan=new Fake();orphan.plot=false;check(GrowthPlotCleanup.run(orphan,null)==null);check(orphan.entities.isEmpty()&&!orphan.calls.contains("blocks"));
        var foreign=new Fake();foreign.foreign=true;check(GrowthPlotCleanup.run(foreign,null)!=null);check(foreign.foreign&&foreign.entities.isEmpty()&&foreign.tickets.isEmpty()&&foreign.plot);
        var multi=new Fake();multi.failEntity=multi.failTicket=true;Throwable failure=GrowthPlotCleanup.run(multi,null);
        check(failure==multi.entityFailure&&Arrays.asList(failure.getSuppressed()).contains(multi.ticketFailure));
        check(multi.entities.equals(Set.of("old"))&&multi.tickets.equals(List.of("first")));check(multi.calls.contains("ticket:last")&&multi.calls.contains("entity:during-clear"));
        for (int kind=0;kind<3;kind++) {var f=new Fake();f.failBlocks=kind==0;f.failTicks=kind==1;f.failProbe=kind==2;check(GrowthPlotCleanup.run(f,null)!=null);check(f.entities.isEmpty()&&f.tickets.isEmpty());}
        var original=new IllegalStateException("native original");var f=new Fake();f.failTicket=true;check(GrowthPlotCleanup.run(f,original)==original);check(original.getSuppressed().length>0);
        var repeat=new Fake();check(GrowthPlotCleanup.run(repeat,null)==null);check(GrowthPlotCleanup.run(repeat,null)==null);
        // Infinite reentrant disposal is bounded failure, never a manufactured clean result.
        var flood=new LinkedHashSet<Integer>(Set.of(0));check(GrowthOwnedResources.drain(flood,id->flood.add(id+1))!=null&&!flood.isEmpty());
        System.out.println("Reentrant block/discard cleanup, orphan leases, foreign preservation, all-release exceptions and bounded cascade controls PASS; not native acceptance");
    }
}
