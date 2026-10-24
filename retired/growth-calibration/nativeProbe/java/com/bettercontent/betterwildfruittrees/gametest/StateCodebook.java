package com.bettercontent.betterwildfruittrees.gametest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToLongFunction;

/** Bootstrap-only builder; immutable identity lookups afterwards. Generic for pure JDK controls. */
public final class StateCodebook<S> {
    public static final long NULL = 0, UNKNOWN = Long.MIN_VALUE;
    public static final String SCHEMA = "original-state-v1";
    private final IdentityHashMap<S, Long> codes;
    public final boolean complete;
    public final int entries, collisions, excess;
    public final String digest;
    private StateCodebook(Builder<S> builder) {
        codes = new IdentityHashMap<>(builder.codes);
        complete = builder.collisions == 0 && builder.excess == 0 && !codes.isEmpty();
        entries = codes.size(); collisions = builder.collisions; excess = builder.excess;
        var values = new ArrayList<>(builder.canonicals.values());
        Collections.sort(values);
        MessageDigest hash = sha();
        hash.update(SCHEMA.getBytes(StandardCharsets.UTF_8));
        for (String value : values) hash.update(scalar(value).getBytes(StandardCharsets.UTF_8));
        digest = java.util.HexFormat.of().formatHex(hash.digest());
    }
    public long lookup(S state) {
        if (state == null) return NULL;
        if (!complete) return UNKNOWN;
        Long code = codes.get(state);
        return code == null ? UNKNOWN : code;
    }
    private static MessageDigest sha() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String scalar(String value) { return value.length() + ":" + value; }
    static String canonical(String block, Map<String, String> properties) {
        StringBuilder value = new StringBuilder(SCHEMA).append(scalar(block)).append(properties.size()).append(':');
        new TreeMap<>(properties).forEach((key, item) -> value.append(scalar(key)).append(scalar(item)));
        return value.toString();
    }
    private static long code(String canonical) {
        return ByteBuffer.wrap(sha().digest(canonical.getBytes(StandardCharsets.UTF_8))).getLong();
    }
    public static final class Builder<S> {
        private final int cap;
        private final ToLongFunction<String> hasher;
        private final IdentityHashMap<S, Long> codes = new IdentityHashMap<>();
        private final Map<Long, String> canonicals = new HashMap<>();
        private int collisions, excess;
        public Builder(int cap) { this(cap, StateCodebook::code); }
        // Package-private collision injection for pure controls; native bootstrap uses SHA-256 only.
        Builder(int cap, ToLongFunction<String> hasher) {
            if (cap < 1 || cap > 131072) throw new IllegalArgumentException("state codebook cap");
            this.cap = cap; this.hasher = hasher;
        }
        public void add(S state, String block, Map<String, String> properties) {
            if (state == null) { collisions++; return; }
            if (codes.size() >= cap && !codes.containsKey(state)) { excess++; return; }
            String canonical = canonical(block, properties);
            long code = hasher.applyAsLong(canonical);
            String prior = canonicals.get(code);
            Long existing = codes.get(state);
            if (code == NULL || code == UNKNOWN || (prior != null && !prior.equals(canonical))
                    || (existing != null && existing != code)) { collisions++; return; }
            canonicals.put(code, canonical);
            codes.put(state, code);
        }
        public void exceeded() { excess++; }
        public StateCodebook<S> freeze() { return new StateCodebook<>(this); }
    }
}
