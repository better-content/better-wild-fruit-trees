package com.bettercontent.betterwildfruittrees;

import com.bettercontent.betterwildfruittrees.gametest.CactusArchitecture;
import com.bettercontent.betterwildfruittrees.gametest.CactusArchitecture.Node;
import com.bettercontent.betterwildfruittrees.gametest.CactusArchitecture.Point;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CactusArchitectureTest {
    public static void main(String[] args) {
        var tests = new CactusArchitectureTest();
        tests.freshTrunkAndStraightArmsAreNotAdultArchitecture();
        tests.allArmsMustBeNativeOriginConnectedAndTurnUp();
        tests.megaRequiresExactProvenanceAndUpperTierNotThicknessOrPlateau();
        System.out.println("CactusArchitecture: 3 cases passed");
    }
    static List<Node> stem(int height) {
        var nodes = new ArrayList<Node>();
        for (int y = 1; y <= height; y++) nodes.add(new Node(new Point(0,y,0),0,-1,0));
        return nodes;
    }
    static List<Node> adult(int height) {
        var nodes = stem(height);
        for (int[] direction : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            int x = direction[0], z = direction[1];
            nodes.add(new Node(new Point(x,2,z),-x,0,-z));
            nodes.add(new Node(new Point(x,3,z),0,-1,0));
        }
        return nodes;
    }
    @Test void freshTrunkAndStraightArmsAreNotAdultArchitecture() {
        assertFalse(CactusArchitecture.reached(stem(1),false,false,5));
        assertFalse(CactusArchitecture.reached(stem(20),false,false,5));
        var straight = adult(6);
        straight.removeIf(n -> n.point().y()==3 && (n.point().x()!=0 || n.point().z()!=0));
        assertFalse(CactusArchitecture.reached(straight,false,false,5));
    }
    @Test void allArmsMustBeNativeOriginConnectedAndTurnUp() {
        assertTrue(CactusArchitecture.reached(adult(3),false,false,5));
        var disconnected = adult(3);
        disconnected.removeIf(n -> n.point().equals(new Point(0,2,0)));
        assertFalse(CactusArchitecture.reached(disconnected,false,false,5));
        var wrongOrigin = adult(3);
        wrongOrigin.removeIf(n -> n.point().equals(new Point(1,2,0)));
        wrongOrigin.add(new Node(new Point(1,2,0),1,0,0));
        assertFalse(CactusArchitecture.reached(wrongOrigin,false,false,5));
    }
    @Test void megaRequiresExactProvenanceAndUpperTierNotThicknessOrPlateau() {
        assertFalse(CactusArchitecture.reached(adult(6),true,false,5));
        assertFalse(CactusArchitecture.reached(adult(5),true,true,5));
        assertTrue(CactusArchitecture.reached(adult(6),true,true,5));
        assertFalse(CactusArchitecture.reached(stem(30),true,true,5));
    }
}
