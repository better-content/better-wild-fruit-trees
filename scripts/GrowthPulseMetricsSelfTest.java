import com.bettercontent.betterwildfruittrees.gametest.GrowthPulseMetrics;

/** Controlled already-evaluated arguments/results and core-state witness accounting only. */
public final class GrowthPulseMetricsSelfTest {
    static void check(boolean x){if(!x)throw new AssertionError();}
    public static void main(String[] args){
        check(GrowthPulseMetrics.legacyState(0)==0x5DEECE66DL);
        var m=new GrowthPulseMetrics();check(m.summary().calls()==0&&m.summary().minimumEnergy()==null);
        m.observe(35F,true,false,12);m.observeRandom(1,2,3,4,5);
        m.observe(17.5F,false,true,4);m.observeRandom(6,4,6,8,6);
        var s=m.summary();check(s.calls()==2&&s.successful()==1&&s.choked()==1&&s.totalSteps()==16&&s.maximumSteps()==12);
        check(s.minimumEnergy()==17.5F&&s.maximumEnergy()==35F&&s.suppliedEnergyBits().values().stream().mapToInt(x->x).sum()==2);
        var r=m.randomSummary();check(r.betweenWorldChanges()==1&&r.betweenGrowthChanges()==0&&r.growWorldChanges()==1&&r.growGrowthChanges()==2&&r.ageWorldChanges()==1);
        try{m.observe(Float.NaN,true,false,1);throw new AssertionError();}catch(IllegalStateException expected){}
        try{m.observe(1,true,false,-1);throw new AssertionError();}catch(IllegalStateException expected){}
        var bounded=new GrowthPulseMetrics();for(int i=0;i<4096;i++)bounded.observe(35F,true,false,1);
        try{bounded.observe(35F,true,false,1);throw new AssertionError();}catch(IllegalStateException expected){}
        System.out.println("Energy/native result/core-state witness accounting controls PASS, not causal or native acceptance");
    }
}
