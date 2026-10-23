package com.bettercontent.betterwildfruittrees.gametest;

import java.util.HashMap;
import java.util.List;

/** Nonshipping versioned structural reference, not maximum size or production maturity state. */
public final class CactusArchitecture {
    public static final String VERSION = "native_upturned_four_arm_v1";
    public record Point(int x, int y, int z) {
        Point plus(int dx, int dy, int dz) { return new Point(x + dx, y + dy, z + dz); }
    }
    /** Origin is the native direction pointing from child to its growth parent. */
    public record Node(Point point, int originX, int originY, int originZ) {
        Point parent() { return point.plus(originX, originY, originZ); }
    }
    private CactusArchitecture() {}

    public static boolean reached(List<Node> nodes, boolean mega, boolean provenConversion, int stopBranchingHeight) {
        if (mega && !provenConversion) return false;
        if (nodes.size() > 4096) throw new IllegalArgumentException("Calibration topology exceeds bounded fixture");
        var byPosition = new HashMap<Point, Node>();
        for (var node : nodes) byPosition.put(node.point(), node);
        // Require a continuous native-origin central stem, rooted at relative (0,1,0).
        int stemHeight = 0;
        while (true) {
            var stem = byPosition.get(new Point(0, stemHeight + 1, 0));
            if (stem == null || stem.originX() != 0 || stem.originY() != -1 || stem.originZ() != 0) break;
            stemHeight++;
        }
        if (stemHeight == 0 || (mega && stemHeight <= stopBranchingHeight)) return false;
        int arms = 0;
        int[][] cardinal = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int d = 0; d < cardinal.length; d++) {
            int dx = cardinal[d][0], dz = cardinal[d][1];
            for (int y = 1; y <= stemHeight; y++) {
                var elbow = byPosition.get(new Point(dx, y, dz));
                var upright = byPosition.get(new Point(dx, y + 1, dz));
                if (elbow != null && upright != null
                    && elbow.originX() == -dx && elbow.originY() == 0 && elbow.originZ() == -dz
                    && upright.originX() == 0 && upright.originY() == -1 && upright.originZ() == 0) {
                    arms |= 1 << d;
                    break;
                }
            }
        }
        return arms == 15;
    }
}
