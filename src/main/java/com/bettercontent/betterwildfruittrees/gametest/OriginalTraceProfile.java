package com.bettercontent.betterwildfruittrees.gametest;

/** Cold, explicit nonshipping observation selection. Never selects a fixture protocol or acceptance. */
public final class OriginalTraceProfile {
    private OriginalTraceProfile() {}
    public static boolean kiwi() {
        return Boolean.getBoolean("better_wild_fruit_trees.originalEventTrace")
            && "kiwi-v1".equals(System.getProperty("better_wild_fruit_trees.originalEventTraceScope"));
    }
    public static String species() { return kiwi() ? "kiwi" : "orange"; }
    public static String scope() { return kiwi() ? "original-kiwi-blocks-v1" : "original-orange-blocks-v1"; }
}
