package com.bettercontent.betterwildfruittrees.gametest;

import java.util.*;

/** Reviewed nonshipping Coconut structural reference; not full-cap wood or actual fruit maturity. */
public final class CoconutArchitecture {
    public static final String VERSION = "native_coconut_fruiting_crown_v1";
    public record Pos(int x, int y, int z) {
        Pos add(int dx,int dy,int dz) { return new Pos(x+dx,y+dy,z+dz); }
        long distance() { return Math.abs((long)x)+Math.abs((long)y)+Math.abs((long)z); }
    }
    public record Branch(int radius, boolean exactNativeCoconutBranch) {}
    public record Leaf(int hydration, boolean exactNativeCoconutLeaves) {}
    private static final List<Pos> STEPS = List.of(new Pos(1,0,0),new Pos(-1,0,0),new Pos(0,1,0),new Pos(0,-1,0),new Pos(0,0,1),new Pos(0,0,-1));
    public static boolean matches(Map<Pos,Branch> branches, Map<Pos,Leaf> leaves,
            int actualFruitingRadius, int actualFamilyCap, int actualSecondaryThickness, double actualGrowthTaper) {
        if (actualFruitingRadius!=5 || actualFamilyCap!=8 || actualSecondaryThickness!=2 || !Double.isFinite(actualGrowthTaper) || Math.abs(actualGrowthTaper-.22)>0.000001)
            throw new IllegalArgumentException("Pinned live Coconut authority drift");
        if (branches.size()>4096 || leaves.size()>4096 || branches.keySet().stream().anyMatch(p->p.distance()>128) || leaves.keySet().stream().anyMatch(p->p.distance()>128))
            throw new IllegalArgumentException("Existing observation bound exhausted, never maturity");
        Pos base=new Pos(0,1,0); Branch root=branches.get(base);
        if (root==null || root.radius()<actualFruitingRadius || root.radius()>actualFamilyCap) return false;
        var seen=new HashSet<Pos>();var pending=new ArrayDeque<Pos>();pending.add(base);Pos terminal=null;
        while (!pending.isEmpty()) {
            Pos p=pending.remove();if (!seen.add(p)) continue;
            Branch b=branches.get(p);
            if (!b.exactNativeCoconutBranch() || b.radius()<1 || b.radius()>actualFamilyCap) return false;
            var adjacent=new ArrayList<Pos>();
            for (Pos d:STEPS) { Pos n=p.add(d.x(),d.y(),d.z());if (branches.containsKey(n)) adjacent.add(n); }
            if (p.equals(base)) { if (adjacent.size()!=1) return false; }
            else if (adjacent.size()==1) { if (terminal!=null) return false;terminal=p; }
            else if (adjacent.size()!=2) return false;
            pending.addAll(adjacent.stream().filter(n->!seen.contains(n)).toList());
        }
        if (seen.size()!=branches.size() || terminal==null) return false;
        // Native single-path taper envelope: this is NOT a chosen height or plateau threshold.
        if (root.radius()>Math.floor(actualSecondaryThickness+actualGrowthTaper*seen.size())) return false;
        Branch tip=branches.get(terminal);Leaf crown=leaves.get(terminal.add(0,1,0));
        // CoconutCellKit supplies leaf hydration only from branch radius <=1. A leaf-bearing tip,
        // not random fruit appearance, accompanies the native radius5 reproductive-eligibility gate.
        return tip.radius()==1 && crown!=null && crown.exactNativeCoconutLeaves() && crown.hydration()>0 && crown.hydration()<=7;
    }
}
