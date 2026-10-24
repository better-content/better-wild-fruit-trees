package com.bettercontent.betterwildfruittrees.gametest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class GrowthControllerSelfTest {
    static final class Fake implements GrowthFixtureController.Backend {
        final ArrayDeque<Runnable> queue = new ArrayDeque<>();
        final List<GrowthFixtureController.Outcome> rows = new ArrayList<>();
        final List<GrowthFixtureController.Mean> means = new ArrayList<>();
        long time, lastPulse = -1;
        int prepares, admits, cleanup, terminals, calls, readyAfter;
        boolean favorable = true, cleanupFailure, engineFailure, badMean, boundary, limitFirst, sameTick, finalPass, complete;
        Throwable original;
        GrowthFixtureController.Sample current;
        public long tick() { return sameTick ? 1 : time; }
        public void schedule(Runnable callback) { queue.add(callback); }
        public void prepare(GrowthFixtureController.Sample sample) { prepares++; current = sample; }
        public boolean chunksReady() { return time >= readyAfter; }
        public void construct(GrowthFixtureController.Sample sample) {}
        public boolean favorable() { return favorable; }
        public void admit(GrowthFixtureController.Sample sample) { admits++; }
        public int pulse(int pulse) {
            if (engineFailure) throw new IllegalArgumentException("original native failure");
            if (lastPulse == time) throw new AssertionError("pulse burst");
            lastPulse = time; calls++;
            return limitFirst && current.species().equals("bayberry") && current.seed() == 0 ? 1 : pulse == 2 ? 8 : 1;
        }
        public int endpoint() { return 8; }
        public double reference(String species) { return boundary ? 2.5 : badMean && species.equals("bayberry") ? 10 : 2; }
        public void cleanup(GrowthFixtureController.Sample sample) { cleanup++; if (cleanupFailure) throw new IllegalStateException("cleanup defect"); }
        public void outcome(GrowthFixtureController.Outcome row) { rows.add(row); }
        public void mean(GrowthFixtureController.Mean mean) { means.add(mean); }
        public void finish(boolean pass, boolean all, Throwable failure) { terminals++; finalPass = pass; complete = all; original = failure; }
        void run() {
            int budget = 100000;
            while (!queue.isEmpty() && budget-- > 0) { time++; queue.remove().run(); }
            if (budget <= 0 || terminals != 1) throw new AssertionError("controller failed containment/terminal");
        }
    }
    static GrowthFixtureController controller(Fake fake) {
        return new GrowthFixtureController(fake, List.of("bayberry", "kiwi"));
    }
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] ignored) {
        var good = new Fake(); var g = controller(good); g.start(); good.run();
        check(good.finalPass && good.complete && good.rows.size() == 16 && good.calls == 32 && good.admits == 16 && good.cleanup == 16);
        check(good.means.size() == 2 && good.rows.stream().allMatch(x -> x.endTick() - x.admissionTick() == 2));
        check(good.rows.get(7).sample().growthSeed() == 12345 + 104729L * 7);
        check(good.rows.get(15).sample().worldSeed() == 54321 + 104729L * 7);
        var early = new Fake(); early.badMean = true; controller(early).start(); early.run();
        check(!early.finalPass && early.complete && early.rows.size() == 16 && early.means.get(1).pass() && early.original != null);
        var strict = new Fake(); strict.boundary = true; controller(strict).start(); strict.run();
        check(!strict.finalPass && strict.complete && strict.means.stream().allMatch(x -> x.deviationNumerator() * 5 == x.deviationDenominator() && !x.pass()));
        var limit = new Fake(); limit.limitFirst = true; controller(limit).start(); limit.run();
        check(!limit.finalPass && limit.complete && limit.rows.get(0).pulses() == 4096 && limit.rows.get(0).status() == GrowthFixtureController.Status.ASSERTION_FAILURE);
        check(limit.rows.size() == 16 && limit.means.get(0).measured() == null && limit.means.get(1).pass());
        var light = new Fake(); light.favorable = false; controller(light).start(); light.run();
        check(!light.finalPass && !light.complete && light.admits == 0 && light.calls == 0 && light.rows.size() == 16);
        check(light.rows.get(0).status() == GrowthFixtureController.Status.INVALID_SETUP && light.rows.get(1).reason().equals("NOT_MEASURED_AFTER_CONTAMINATION"));
        var delayed = new Fake(); delayed.readyAfter = 180; controller(delayed).start(); delayed.run(); check(delayed.finalPass);
        var late = new Fake(); late.readyAfter = 201; controller(late).start(); late.run(); check(!late.finalPass && late.admits == 0);
        var clean = new Fake(); clean.cleanupFailure = true; controller(clean).start(); clean.run();
        check(!clean.finalPass && !clean.complete && clean.cleanup == 1 && clean.prepares == 1 && clean.original.getMessage().equals("cleanup defect"));
        var engine = new Fake(); engine.engineFailure = true; controller(engine).start(); engine.run();
        check(!engine.finalPass && !engine.complete && engine.original instanceof IllegalArgumentException && engine.cleanup == 1);
        var both = new Fake(); both.engineFailure = both.cleanupFailure = true; controller(both).start(); both.run();
        check(both.original instanceof IllegalArgumentException && both.original.getSuppressed().length == 1);
        var cancelled = new Fake(); var c = controller(cancelled); c.start(); cancelled.time++; cancelled.queue.remove().run();
        c.cancel(new IllegalStateException("cancelled")); c.cancel(new IllegalStateException("again")); cancelled.run();
        check(!cancelled.finalPass && cancelled.prepares == 1 && cancelled.admits == 0 && cancelled.cleanup == 1);
        var duplicate = new Fake(); duplicate.sameTick = true; controller(duplicate).start(); duplicate.run(); check(!duplicate.finalPass && duplicate.calls == 0);
        boolean rejected = false; try { g.start(); } catch (IllegalStateException expected) { rejected = true; } check(rejected);
        rejected = false; try { new GrowthFixtureController(new Fake(), List.of("kiwi", "kiwi")); } catch (IllegalArgumentException expected) { rejected = true; } check(rejected);
        check(GrowthFixtureController.MAX_TICKS > 13 * 8 * (4096 + 200));
        Object world = new Object(), block = new Object(); var root = new GrowthRootIsolation(world, 42);
        check(!root.block(world, 42, block)); // Before admission/setup and v1 has no registered hook.
        root.admit(block); check(root.block(world, 42, block));
        check(!root.block(new Object(), 42, block) && !root.block(world, 43, block) && !root.block(world, 42, new Object()));
        var otherThread = new java.util.concurrent.atomic.AtomicBoolean(true);
        Thread thread = new Thread(() -> otherThread.set(root.block(world, 42, block)));
        thread.start(); try { thread.join(1000); } catch (InterruptedException e) { throw new AssertionError(e); }
        check(!thread.isAlive() && !otherThread.get()); root.disarm(); check(!root.block(world, 42, block));
        int[] forwards = {0}; RuntimeException marker = new IllegalArgumentException("native identity");
        try { if (!root.block(world, 42, block)) { forwards[0]++; throw marker; } } catch (RuntimeException actual) { check(actual == marker); }
        check(root.blocked() == 1 && forwards[0] == 1 && !root.admitted());
        Object chunk = new Object(), holder = new Object();
        check(GrowthTickingReadiness.ready(true,false,false,chunk,chunk,world,world,42,42,holder,holder));
        check(!GrowthTickingReadiness.ready(false,false,false,chunk,chunk,world,world,42,42,holder,holder));
        check(!GrowthTickingReadiness.ready(true,false,false,null,chunk,world,world,42,42,holder,holder)); // RIGHT
        check(!GrowthTickingReadiness.ready(true,false,false,chunk,new Object(),world,world,42,42,holder,holder));
        check(!GrowthTickingReadiness.ready(true,false,false,chunk,chunk,new Object(),world,42,42,holder,holder));
        check(!GrowthTickingReadiness.ready(true,false,false,chunk,chunk,world,world,43,42,holder,holder));
        check(!GrowthTickingReadiness.ready(true,false,false,chunk,chunk,world,world,42,42,holder,new Object()));
        for (boolean cancelledFuture : new boolean[]{false,true}) {
            rejected=false; try { GrowthTickingReadiness.ready(true,!cancelledFuture,cancelledFuture,chunk,chunk,world,world,42,42,holder,holder); }
            catch (IllegalStateException expected) { rejected=true; } check(rejected);
        }
        var resources = new ArrayList<>(List.of(1,2,3)); var attempts = new ArrayList<Integer>();
        RuntimeException removal = new IllegalArgumentException("ticket removal"), anotherRemoval = new IllegalStateException("later ticket");
        Throwable released = GrowthOwnedResources.releaseAll(resources, item -> { attempts.add(item); if(item==1)throw removal; if(item==3)throw anotherRemoval; });
        check(released == removal && released.getSuppressed()[0] == anotherRemoval && attempts.equals(List.of(1,2,3)) && resources.equals(List.of(1,3)));
        System.out.println("14 controller scenarios plus root lease/forwarding and completed-LEFT identity controls PASS; all-release containment PASS; not native geometry acceptance");
    }
}
