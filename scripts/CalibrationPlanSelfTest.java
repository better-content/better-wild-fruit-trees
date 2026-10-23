package com.bettercontent.betterwildfruittrees.gametest;

/** Pure JDK source-control oracle: does not link Minecraft, Forge, addons or the mod. */
public final class CalibrationPlanSelfTest {
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        var all = CalibrationPlan.select(null).samples();
        require(all.size() == 32 && CalibrationPlan.Form.values().length == 4);
        for (String selector : new String[]{"all", "ancient", "saguaro,saguaro_mega"}) {
            var selected = CalibrationPlan.select(selector).samples();
            require(selected.size() == (selector.equals("all") ? 32 : selector.equals("ancient") ? 8 : 16));
            int previous = -1;
            for (var sample : selected) {
                int oldIndex = sample.canonicalIndex();
                require(oldIndex > previous); previous = oldIndex;
                require(sample.equals(all.get(oldIndex)));
                require(sample.form().ordinal() == oldIndex / 8 && sample.sample() == oldIndex % 8);
                require(sample.growthSeed() == 12345 + 104729L * (oldIndex % 8));
                require(sample.levelSeed() == 54321 + 104729L * (oldIndex % 8));
                System.out.println(selector + " " + sample.form().species() + " " + oldIndex + " "
                    + sample.sample() + " " + sample.growthSeed() + " " + sample.levelSeed());
            }
            try { selected.clear(); throw new AssertionError("Mutable sample plan"); }
            catch (UnsupportedOperationException expected) { }
        }
        for (String invalid : new String[]{"", " ", "saguaro", "coconut", "mega", "unknown", "ancient,ancient",
                "saguaro,saguaro", "saguaro_mega,saguaro", "saguaro,saguaro_mega,", "all,ancient"}) {
            try { CalibrationPlan.select(invalid); throw new AssertionError("Invalid profile accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        for (int bad : new int[]{-1, 8}) {
            try { new CalibrationPlan.Sample(CalibrationPlan.Form.ANCIENT, bad); throw new AssertionError(); }
            catch (IllegalArgumentException expected) { }
        }
    }
}
