package com.bettercontent.betterwildfruittrees.gametest;

import java.util.ArrayList;
import java.util.List;

/** Nonshipping diagnostic selection. Canonical indices and the original per-form seeds never reindex. */
public final class CalibrationPlan {
    public static final String PROPERTY = "better_wild_fruit_trees.calibration.forms";
    public static final String NAMESPACE = "better_wild_fruit_calibration";
    public enum Form {
        SAGUARO("dynamictreesplus:saguaro_cactus"),
        COCONUT("dtnatures_spirit:coconut"),
        ANCIENT("dtquark:ancient"),
        MEGA("dynamictreesplus:mega_cactus");
        private final String species;
        Form(String species) { this.species = species; }
        public String species() { return species; }
    }
    public record Sample(Form form, int sample) {
        public Sample {
            if (form == null || sample < 0 || sample >= 8) throw new IllegalArgumentException("Invalid canonical sample");
        }
        public int canonicalIndex() { return form.ordinal() * 8 + sample; }
        // These are exactly the old index%8 recipes, NOT selected-list cursor/ordinal allocations.
        public long growthSeed() { return 12345 + 104729L * sample; }
        public long levelSeed() { return 54321 + 104729L * sample; }
    }
    private final String selector;
    private final List<Sample> samples;
    private CalibrationPlan(String selector, List<Form> forms) {
        this.selector = selector;
        var selected = new ArrayList<Sample>();
        for (var form : forms) for (int sample = 0; sample < 8; sample++) selected.add(new Sample(form, sample));
        samples = List.copyOf(selected);
    }
    public static CalibrationPlan select(String selector) {
        if (selector == null) selector = "all";
        return switch (selector) {
            case "all" -> new CalibrationPlan(selector, List.of(Form.values()));
            case "ancient" -> new CalibrationPlan(selector, List.of(Form.ANCIENT));
            case "saguaro,saguaro_mega" -> new CalibrationPlan(selector, List.of(Form.SAGUARO, Form.MEGA));
            default -> throw new IllegalArgumentException("Only all, ancient, saguaro,saguaro_mega calibration profiles are admitted");
        };
    }
    public String selector() { return selector; }
    public List<Sample> samples() { return samples; }
}
