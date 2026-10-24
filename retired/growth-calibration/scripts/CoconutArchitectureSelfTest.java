package com.bettercontent.betterwildfruittrees.gametest;

import java.util.*;
public final class CoconutArchitectureSelfTest {
    static final CoconutArchitecture.Pos BASE=new CoconutArchitecture.Pos(0,1,0), TIP=new CoconutArchitecture.Pos(1,21,0), CROWN=new CoconutArchitecture.Pos(1,22,0);
    static Map<CoconutArchitecture.Pos,CoconutArchitecture.Branch> path() {
        var m=new HashMap<CoconutArchitecture.Pos,CoconutArchitecture.Branch>();
        for(int y=1;y<=3;y++)m.put(new CoconutArchitecture.Pos(0,y,0),new CoconutArchitecture.Branch(y==1?6:5,true));
        for(int y=3;y<=21;y++)m.put(new CoconutArchitecture.Pos(1,y,0),new CoconutArchitecture.Branch(y==21?1:2,true));
        return m;
    }
    static boolean check(Map<CoconutArchitecture.Pos,CoconutArchitecture.Branch>b,Map<CoconutArchitecture.Pos,CoconutArchitecture.Leaf>l) {return CoconutArchitecture.matches(b,l,5,8,2,.22);}
    static void need(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args) {
        var leaves=Map.of(CROWN,new CoconutArchitecture.Leaf(4,true));need(check(path(),leaves));
        var below=path();below.put(BASE,new CoconutArchitecture.Branch(4,true));need(!check(below,leaves)); // Tall alone is not maturity.
        var fresh=Map.of(BASE,new CoconutArchitecture.Branch(1,true));need(!check(fresh,Map.of(new CoconutArchitecture.Pos(0,2,0),new CoconutArchitecture.Leaf(4,true))));
        need(!check(path(),Map.of()));
        need(!check(path(),Map.of(CROWN,new CoconutArchitecture.Leaf(0,true))));
        need(!check(path(),Map.of(CROWN,new CoconutArchitecture.Leaf(4,false))));
        var foreign=path();foreign.put(TIP,new CoconutArchitecture.Branch(1,false));need(!check(foreign,leaves));
        var thickTip=path();thickTip.put(TIP,new CoconutArchitecture.Branch(2,true));need(!check(thickTip,leaves));
        var separated=path();separated.put(new CoconutArchitecture.Pos(8,8,8),new CoconutArchitecture.Branch(1,true));need(!check(separated,leaves));
        var fork=path();fork.put(new CoconutArchitecture.Pos(2,10,0),new CoconutArchitecture.Branch(1,true));need(!check(fork,leaves));
        var fakeStub=new HashMap<CoconutArchitecture.Pos,CoconutArchitecture.Branch>();fakeStub.put(BASE,new CoconutArchitecture.Branch(5,true));fakeStub.put(new CoconutArchitecture.Pos(0,2,0),new CoconutArchitecture.Branch(1,true));
        need(!check(fakeStub,Map.of(new CoconutArchitecture.Pos(0,3,0),new CoconutArchitecture.Leaf(4,true))));
        boolean bounds=false;try{var b=path();b.put(new CoconutArchitecture.Pos(129,0,0),new CoconutArchitecture.Branch(1,true));check(b,leaves);}catch(IllegalArgumentException expected){bounds=true;}need(bounds);
        boolean drift=false;try{CoconutArchitecture.matches(path(),leaves,6,8,2,.22);}catch(IllegalArgumentException expected){drift=true;}need(drift);
        boolean nonfinite=false;try{CoconutArchitecture.matches(path(),leaves,5,8,2,Double.NaN);}catch(IllegalArgumentException expected){nonfinite=true;}need(nonfinite);
        System.out.println("COCONUT_REVIEWED_CRITERION_13_SCALAR_SCENARIOS_PLUS_NONFINITE_AUTHORITY_PASS; NO_NATIVE_MEASUREMENT_OR_ENDPOINT_ACCEPTANCE");
    }
}
