package com.bettercontent.betterwildfruittrees.gametest;

/** Inert authored-stub selection controls; NOT native bootstrap/delivery or acceptance. */
public final class KiwiStateSelectionSelfTest {
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    public static void main(String[] args) {
        System.clearProperty("better_wild_fruit_trees.originalEventTrace");
        System.setProperty("better_wild_fruit_trees.originalEventTraceScope", "kiwi-v1");
        check(OriginalTraceProfile.species().equals("orange"), "scope without ordered opt-in activated kiwi");
        check(NativeStateCodes.selected("dtfruitsdelight:orange_leaves"), "old orange scope changed");
        check(!NativeStateCodes.selected("dtfruitsdelight:kiwi_leaves"), "kiwi silently widened orange");
        System.setProperty("better_wild_fruit_trees.originalEventTrace", "true");
        check(OriginalTraceProfile.scope().equals("original-kiwi-blocks-v1"), "wrong explicit scope");
        for (String key : new String[]{"minecraft:stone", "dynamictrees:rooty_dirt", "dynamictrees:jungle_branch", "dtfruitsdelight:kiwi_leaves"})
            check(NativeStateCodes.selected(key), "required kiwi key rejected: " + key);
        for (String key : new String[]{"dtfruitsdelight:orange_leaves", "dynamictrees:oak_branch", "dynamictrees:trunk_shell", "dtfruitsdelight:kiwi", "other:kiwi_leaves", "minecraft:", ":stone", "minecraft:a:b"})
            check(!NativeStateCodes.selected(key), "unadmitted kiwi key selected: " + key);
        check(!NativeStateCodes.selected(null), "null key selected");
        System.setProperty("better_wild_fruit_trees.growthDiagnostics", "true");
        OriginalLightTrace.begin("orange", 0); check(!OriginalLightTrace.ownerActive(), "orange activated in kiwi mode");
        OriginalLightTrace.begin("kiwi", -1); check(!OriginalLightTrace.ownerActive(), "invalid seed activated");
        OriginalLightTrace.begin("kiwi", 8); check(!OriginalLightTrace.ownerActive(), "seed overflow activated");
        OriginalLightTrace.begin("kiwi", 0); check(OriginalLightTrace.ownerActive(), "kiwi scope inactive");
        OriginalLightTrace.pulse(1); OriginalLightTrace.finish(); check(!OriginalLightTrace.ownerActive(), "kiwi scope retained");
        System.setProperty("better_wild_fruit_trees.originalEventTraceScope", "orange-v1");
        check(OriginalTraceProfile.species().equals("orange") && NativeStateCodes.selected("dynamictrees:oak_branch")
            && !NativeStateCodes.selected("dynamictrees:jungle_branch"), "explicit old scope changed");
        System.clearProperty("better_wild_fruit_trees.originalEventTraceScope");
        System.clearProperty("better_wild_fruit_trees.originalEventTrace");
        System.clearProperty("better_wild_fruit_trees.growthDiagnostics");
        System.out.println("PASS explicit kiwi selection/default-orange isolation/bounded seeds; NOT native acceptance");
    }
}
