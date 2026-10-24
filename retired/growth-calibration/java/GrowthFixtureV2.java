package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.TreeGrowthRates;
import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.entity.FallingTreeEntity;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/** Nonshipping native adapter. No leaf substitution, light flush, internal wait or new gameplay hook. */
@Mod.EventBusSubscriber(modid = WildFruitTrees.MOD_ID)
public final class GrowthFixtureV2 implements GrowthFixtureController.Backend {
    private static final BlockPos ROOT = new BlockPos(48, 160, 48);
    private static final BlockPos MIN = ROOT.offset(-44, 0, -44), MAX = ROOT.offset(44, 60, 44);
    private static final AABB DOMAIN = new AABB(4, 160, 4, 93, 221, 93);
    private static final TicketType<UUID> TICKET = TicketType.create("bc_wft_growth_v2", UUID::compareTo);
    public interface TickingAccess { net.minecraft.server.level.ChunkHolder growthV2$visible(long key); }
    public interface RandomStateAccess { java.util.concurrent.atomic.AtomicLong growthV2$seed(); }
    private static GrowthFixtureV2 active;
    private final GameTestHelper helper;
    private final ServerLevel level;
    private final GrowthFixtureController controller;
    private final List<ChunkPos> tickets = new ArrayList<>();
    private final Set<UUID> ownedEntities = new HashSet<>();
    private final UUID owner = UUID.randomUUID();
    private final GrowthRootIsolation isolation;
    private Object expectedRootBlock;
    private boolean isolationControlDone;
    private String contamination;
    private long controlledBlocked;
    private int autonomousBlocked;
    private final Thread owningThread = Thread.currentThread();
    private final Path receipt = Path.of("growth-v2-results.jsonl").toAbsolutePath();
    private final Gson json = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    private FileChannel output;
    private int sequence;
    private boolean environmentOwned, plotOwned;
    private boolean daylight, weather;
    private long day;
    private int clearWeather, rainTime, thunderTime;
    private boolean raining, thundering;
    private Species tree;
    private BranchBlock branch;
    private RandomSource random;
    private GrowthPulseMetrics metrics = new GrowthPulseMetrics();
    private long setupNanos, admittedNanos, cleanupNanos, admittedDay;
    private int growCalls, ageCalls;
    private long firstPulseTick, lastPulseTick, maxPulseGap, pulseTickDigest;

    public static void start(GameTestHelper helper) {
        if (!GrowthFixturePlan.v2() || active != null) throw new IllegalStateException("Growth V2 ownership conflict");
        var fixture = new GrowthFixtureV2(helper);
        active = fixture;
        try { fixture.open(); fixture.controller.start(); }
        catch (Throwable failure) { fixture.controller.cancel(failure); }
    }
    private GrowthFixtureV2(GameTestHelper helper) {
        this.helper = helper; level = helper.getLevel();
        isolation = new GrowthRootIsolation(level, ROOT.asLong());
        controller = new GrowthFixtureController(this, GrowthFixturePlan.species());
    }
    private void open() throws Exception {
        var functions = GameTestRegistry.getAllTestFunctions().stream()
            .filter(f -> f.getBatchName().equals(GrowthFixturePlan.BATCH)).toList();
        if (functions.size() != 1 || !functions.get(0).getTestName().equals(GrowthFixturePlan.NAME) ||
            functions.get(0).isFlaky() || functions.get(0).getMaxTicks() != GrowthFixtureController.MAX_TICKS)
            throw new IllegalStateException("Native singleton batch/identity/timeout drift");
        Properties binding = new Properties();
        Path bindingPath = Path.of("growth-v2-bindings.properties");
        try (var reader = Files.newBufferedReader(bindingPath)) { binding.load(reader); }
        if (!"v2".equals(binding.getProperty("fixtureVersion")) || !"false".equals(binding.getProperty("sourceDirty")) ||
            !GrowthFixturePlan.scope().equals(binding.getProperty("scope")) ||
            !GrowthFixturePlan.control().equals(binding.getProperty("control"))) throw new IllegalStateException("Native growth binding drift");
        output = FileChannel.open(receipt, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        emit("HEADER", Map.of("schema", "bc.wft.growth.v2", "bindings", binding,
            "bindingSha256", sha(Files.readAllBytes(bindingPath)), "species", GrowthFixturePlan.species(),
            "referenceProvenance", "UNKNOWN", "endpoint", "native_maximum_trunk_radius",
            "samples", 8, "pulseLimit", 4096, "setupLimitTicks", 200, "testTimeoutTicks", GrowthFixtureController.MAX_TICKS));
        var rules = level.getGameRules();
        daylight = rules.getBoolean(GameRules.RULE_DAYLIGHT);
        weather = rules.getBoolean(GameRules.RULE_WEATHER_CYCLE); day = level.getDayTime();
        var data = (ServerLevelData) level.getLevelData();
        clearWeather = data.getClearWeatherTime(); rainTime = data.getRainTime(); thunderTime = data.getThunderTime();
        raining = data.isRaining(); thundering = data.isThundering();
        environmentOwned = true;
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.setDayTime(6000); level.setWeatherParameters(0, 0, false, false); level.updateSkyBrightness();
    }
    @Override public long tick() { return level.getGameTime(); }
    @Override public void schedule(Runnable callback) { helper.runAfterDelay(1, callback); }
    @Override public void prepare(GrowthFixtureController.Sample sample) {
        setupNanos = System.nanoTime(); admittedNanos = 0; cleanupNanos = 0;
        metrics = new GrowthPulseMetrics();
        growCalls = ageCalls = autonomousBlocked = 0; firstPulseTick = lastPulseTick = maxPulseGap = pulseTickDigest = 0;
        isolation.disarm(); expectedRootBlock = null; contamination = null;
        if (!tickets.isEmpty() || plotOwned || !ownedEntities.isEmpty()) throw new IllegalStateException("Prior sample ownership not clean: " + ownershipSnapshot());
        for (int x = MIN.getX() >> 4; x <= MAX.getX() >> 4; x++) for (int z = MIN.getZ() >> 4; z <= MAX.getZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            level.getChunkSource().addRegionTicket(TICKET, chunk, 2, owner, true);
            tickets.add(chunk); // Remember before a subsequent native load can throw.
            level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, true);
        }
        tree = Species.REGISTRY.get(new ResourceLocation(GrowthFixturePlan.id(sample.species())));
        branch = tree.getFamily().getBranch().orElseThrow();
        emit("PREPARE", Map.of("sample", sample, "tick", tick(), "day", level.getDayTime(), "ticketCount", tickets.size(),
            "min", List.of(4,160,4), "max", List.of(92,220,92)));
    }
    @Override public boolean chunksReady() {
        if (tickets.size() != 36) return false;
        var cache = level.getChunkSource();
        var access = (TickingAccess)(Object)cache.chunkMap;
        for (var chunk : tickets) {
            var holder = access.growthV2$visible(chunk.toLong());
            if (holder == null || !holder.getPos().equals(chunk)) return false;
            var future = holder.getTickingChunkFuture();
            boolean failed = future.isCompletedExceptionally(), cancelled = future.isCancelled(), done = future.isDone();
            if (done && (failed || cancelled)) future.getNow(null); // Nonblocking: preserve native completion/cancellation cause.
            var either = done && !failed && !cancelled ? future.getNow(null) : null;
            var nativeChunk = either == null ? null : either.left().orElse(null);
            if (!GrowthTickingReadiness.ready(done, failed, cancelled, nativeChunk, cache.getChunkNow(chunk.x, chunk.z),
                nativeChunk == null ? null : nativeChunk.getLevel(), level, nativeChunk == null ? Long.MIN_VALUE : nativeChunk.getPos().toLong(), chunk.toLong(),
                holder, access.growthV2$visible(chunk.toLong())) || !cache.isPositionTicking(chunk.toLong()) ||
                !level.shouldTickBlocksAt(chunk.toLong()) || !level.areEntitiesLoaded(chunk.toLong())) return false;
        }
        return true;
    }
    @Override public void construct(GrowthFixtureController.Sample sample) {
        if (!chunksReady()) throw new IllegalStateException("Construct before actual ticking readiness");
        // Fresh above-ground plot: refuse preexisting/foreign state rather than taking ownership of it.
        for (var pos : BlockPos.betweenClosed(MIN, MAX)) if (!level.isEmptyBlock(pos))
            throw new IllegalStateException("Unowned nonempty plot at " + pos);
        if (!level.getEntities((Entity)null, DOMAIN, e -> true).isEmpty()) throw new IllegalStateException("Unowned entity in plot");
        plotOwned = true;
        level.setBlock(ROOT, Blocks.DIRT.defaultBlockState(), 2);
        tree.placeRootyDirtBlock(level, ROOT, 0);
        expectedRootBlock = level.getBlockState(ROOT).getBlock();
        branch.setRadius(level, ROOT.above(), 1, Direction.DOWN, 2);
        level.setBlock(ROOT.above(2), tree.getLeavesProperties().getDynamicLeavesState(4), 2);
        if (tree.getSignalEnergy() != WildFruitPolicy.ENERGY.get(sample.species()) ||
            tree.getMaxBranchRadius() <= 1 || level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) != 3 ||
            com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_MULTIPLIER.get() != .5 ||
            com.ferreusveritas.dynamictrees.init.DTConfigs.TREE_GROWTH_FOLDING.get() != 2)
            throw new IllegalStateException("Pinned native species/configuration mismatch");
        emit("CONSTRUCT", Map.of("sample", sample, "tick", tick(), "day", level.getDayTime(),
            "branch", branch.toString(), "leaves", tree.getLeavesProperties().getDynamicLeavesState(4).toString(),
            "endpoint", endpoint(), "reference", reference(sample.species()), "energy", tree.getSignalEnergy(),
            "biome", level.getBiome(ROOT).unwrapKey().map(x -> x.location().toString()).orElse("UNKNOWN"),
            "logicKit", tree.getGrowthLogicKit().getConfigurable().getClass().getName()));
    }
    private void environment() {
        if (active != this || !environmentOwned || level.getDayTime() != 6000 || level.isRaining() || level.isThundering() ||
            level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) || level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE))
            throw new IllegalStateException("Fixture environment ownership drift");
        if (Thread.currentThread() != owningThread || (isolation.admitted() &&
            (level.getBlockState(ROOT).getBlock() != expectedRootBlock ||
            ((RootyBlock)level.getBlockState(ROOT).getBlock()).getSpecies(level.getBlockState(ROOT), level, ROOT) != tree)))
            throw new IllegalStateException("Fixture root/thread ownership drift");
        if (isolation.admitted()) {
            if (contamination != null) throw new IllegalStateException(contamination);
            // Explicit effects/commands are not part of this isolated no-player fixture. Reject
            // actors and falling-tree root-decay routes, rather than counting hidden growth as PASS.
            if (!level.getEntities((Entity)null, DOMAIN, entity -> !(entity instanceof ItemEntity && ownedEntities.contains(entity.getUUID()))).isEmpty())
                throw new IllegalStateException("Actor/root-decay contamination during manual pulse measurement");
        }
    }
    @Override public boolean favorable() {
        environment();
        var root = level.getBlockState(ROOT);
        var leaf = level.getBlockState(ROOT.above(2));
        if (!(root.getBlock() instanceof RootyBlock) || root.getValue(RootyBlock.FERTILITY) != 0 ||
            ((RootyBlock)root.getBlock()).getSpecies(root, level, ROOT) != tree ||
            level.getBlockState(ROOT.above()).getBlock() != branch || branch.getRadius(level.getBlockState(ROOT.above())) != 1 ||
            leaf.getBlock() != tree.getLeavesProperties().getDynamicLeavesState(4).getBlock())
            throw new IllegalStateException("Fresh native root/branch/leaves identity drift");
        boolean sunlight = level.getMaxLocalRawBrightness(ROOT.above(3)) >= 13;
        boolean adequate = ((DynamicLeavesBlock)leaf.getBlock()).hasAdequateLight(leaf, level, tree.getLeavesProperties(), ROOT.above(2));
        boolean ready = chunksReady();
        emit("SETUP_CHECK", Map.of("tick", tick(), "day", level.getDayTime(), "sunlight", sunlight,
            "nativeAdequateLight", adequate, "tickingReady", ready, "fertility", 0));
        return sunlight && adequate && ready;
    }
    @Override public void admit(GrowthFixtureController.Sample sample) {
        ((RootyBlock)level.getBlockState(ROOT).getBlock()).setFertility(level, ROOT, 15);
        isolation.admit(expectedRootBlock);
        if (!isolationControlDone) {
            long blocked = isolation.blocked(), returns = isolation.returns();
            var state = level.getBlockState(ROOT);
            ((RootyBlock)state.getBlock()).randomTick(state, level, ROOT, RandomSource.create(713));
            if (isolation.blocked() != blocked + 1 || isolation.returns() != returns || branch.getRadius(level.getBlockState(ROOT.above())) != 1)
                throw new IllegalStateException("Transformed owned-root cancellation control failed");
            controlledBlocked++;
        }
        random = RandomSource.create(sample.growthSeed());
        level.random.setSeed(sample.worldSeed()); // ONCE at measurement admission, never during growth.
        if (seedSnapshot(level.random) != GrowthPulseMetrics.legacyState(sample.worldSeed()) ||
            seedSnapshot(random) != GrowthPulseMetrics.legacyState(sample.growthSeed()))
            throw new IllegalStateException("Read-only RNG accessor admission witness mismatch");
        admittedNanos = System.nanoTime(); admittedDay = level.getDayTime();
        emit("ADMIT", Map.of("sample", sample, "tick", tick(), "day", admittedDay, "growthSeed", sample.growthSeed(),
            "worldSeed", sample.worldSeed(), "fertility", level.getBlockState(ROOT).getValue(RootyBlock.FERTILITY),
            "setupDurationNs", admittedNanos - setupNanos, "worldRandomClass", level.random.getClass().getName(),
            "growthRandomClass", random.getClass().getName(), "seedAccessorWitness", true));
    }
    @Override public int pulse(int pulse) {
        environment();
        long now = tick();
        if (growCalls > 0 && now <= lastPulseTick) throw new IllegalStateException("Native pulse burst");
        if (growCalls == 0) firstPulseTick = now; else maxPulseGap = Math.max(maxPulseGap, now - lastPulseTick);
        lastPulseTick = now; pulseTickDigest = pulseTickDigest * 31 + now; growCalls++;
        long worldBefore = seedSnapshot(level.random), growthBefore = seedSnapshot(random);
        float energy = GrowthFixturePlan.control().equals("zero-energy") ? 0 : tree.getEnergy(level, ROOT);
        var result = branch.growSignal(level, ROOT.above(), new GrowSignal(tree, ROOT, energy, random, Direction.UP));
        long worldAfterGrow = seedSnapshot(level.random), growthAfterGrow = seedSnapshot(random);
        metrics.observe(energy, result.success, result.choked, result.numSteps);
        if (pulse % 16 == 0) { ageCalls++; TreeHelper.ageVolume(level, ROOT.above(), 20, 48, 1, SafeChunkBounds.ANY); }
        metrics.observeRandom(worldBefore, growthBefore, worldAfterGrow, growthAfterGrow, seedSnapshot(level.random));
        environment(); // A native join during grow/age must invalidate this pulse before acceptance.
        if (level.getBlockState(ROOT.above()).getBlock() != branch) throw new IllegalStateException("Native trunk identity lost");
        return branch.getRadius(level.getBlockState(ROOT.above()));
    }
    private static long seedSnapshot(RandomSource source) {
        if (!(source instanceof RandomStateAccess access)) throw new IllegalStateException("Unpinned fixture RNG implementation: " + source.getClass().getName());
        return access.growthV2$seed().get(); // Core-state witness only, NOT complete RNG/world equivalence.
    }
    @Override public int endpoint() { return tree.getMaxBranchRadius(); }
    @Override public double reference(String fruit) { return TreeGrowthRates.reference(GrowthFixturePlan.id(fruit)).pulses(); }
    public static boolean blockRootRandomTick(ServerLevel world, BlockPos position, Object rootBlock) {
        var fixture = active;
        if (fixture == null) return false;
        // The exact native root block identity is checked in addition to the spatial/thread lease.
        Object actual = world == fixture.level && position.equals(ROOT) && Thread.currentThread() == fixture.owningThread
            ? world.getBlockState(position).getBlock() : rootBlock;
        boolean blocked = fixture.isolation.block(world, position.asLong(), actual == rootBlock ? rootBlock : null);
        if (blocked) fixture.autonomousBlocked++;
        return blocked;
    }
    public static void rootRandomTickReturned(ServerLevel world, BlockPos position, Object rootBlock) {
        var fixture = active;
        if (fixture != null && world == fixture.level && Thread.currentThread() == fixture.owningThread)
            fixture.isolation.returned();
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        var fixture = active;
        if (fixture == null || !fixture.plotOwned || event.getLevel() != fixture.level) return;
        Entity entity = event.getEntity();
        if (DOMAIN.intersects(entity.getBoundingBox())) {
            if (fixture.isolation.admitted() && !(entity instanceof ItemEntity))
                fixture.contamination = "Native actor/falling-tree root-decay joined owned measurement plot: " + entity.getType();
            if (entity instanceof FallingTreeEntity || entity instanceof ItemEntity) fixture.ownedEntities.add(entity.getUUID());
        }
    }
    private void forwardingControl() {
                // A deliberate transformed unrelated-root forwarding check AFTER measurement only.
                // Its own native changes are cleaned below; it adds no measured growth opportunity.
                if (!isolationControlDone && admittedNanos != 0) {
                    var neighbor = ROOT.east(30);
                    var ordinary = Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak"));
                    level.setBlock(neighbor, Blocks.DIRT.defaultBlockState(), 2);
                    ordinary.placeRootyDirtBlock(level, neighbor, 0);
                    var nativeBranch = ordinary.getFamily().getBranch().orElseThrow();
                    nativeBranch.setRadius(level, neighbor.above(), 1, Direction.DOWN, 2);
                    level.setBlock(neighbor.above(2), ordinary.getLeavesProperties().getDynamicLeavesState(4), 2);
                    long blocked = isolation.blocked(), forwarded = isolation.forwarded(), returns = isolation.returns();
                    var state = level.getBlockState(neighbor);
                    ((RootyBlock)state.getBlock()).randomTick(state, level, neighbor, RandomSource.create(719));
                    if (isolation.blocked() != blocked || isolation.forwarded() != forwarded + 1 || isolation.returns() != returns + 1 || controlledBlocked != 1)
                        throw new IllegalStateException("Transformed unrelated-root original forwarding control failed");
                    isolationControlDone = true;
                    emit("ISOLATION_CONTROL", Map.of("ownedCancelled", 1, "unrelatedOriginalReturned", 1,
                        "measurementAlreadyEnded", true, "gateDisarmed", !isolation.admitted(), "tick", tick()));
                }
    }
    private Map<String,Object> ownershipSnapshot() {
        return Map.of("plotOwned", plotOwned, "ticketCount", tickets.size(), "tickets", tickets.stream().map(Object::toString).toList(),
            "ownedEntityCount", ownedEntities.size(), "ownedEntityIds", ownedEntities.stream().map(UUID::toString).sorted().toList());
    }
    private void checkForeignEntities() {
        if (!plotOwned) return; // Do not claim or dispose a plot already released by partial cleanup.
        for (var entity : level.getEntities((Entity)null, DOMAIN, e -> true))
            if (!ownedEntities.contains(entity.getUUID())) throw new IllegalStateException("Foreign entity contaminated owned plot: " + entity.getType() + "/" + entity.getUUID());
    }
    private static void rethrowCleanup(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new IllegalStateException(failure);
    }
    @Override public void cleanup(GrowthFixtureController.Sample sample) {
        isolation.disarm(); // FIRST, including cancellation/failure; stale callbacks cannot retain it.
        long before = System.nanoTime();
        var initial = ownershipSnapshot();
        Throwable failure = GrowthPlotCleanup.run(new GrowthPlotCleanup.Port() {
            public boolean plotOwned() { return GrowthFixtureV2.this.plotOwned; }
            public void checkForeignEntities() { GrowthFixtureV2.this.checkForeignEntities(); }
            public void beforeClear() { forwardingControl(); }
            public void clearBlocks() {
                // Clear crown before root: native on-remove effects remain native and any joined
                // owned entity stays tracked until the AFTER-clear drain below.
                for (int y = MAX.getY(); y >= MIN.getY(); y--)
                    for (var pos : BlockPos.betweenClosed(MIN.getX(), y, MIN.getZ(), MAX.getX(), y, MAX.getZ()))
                        if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            public void clearScheduledTicks() {
                var box = new BoundingBox(MIN.getX(), MIN.getY(), MIN.getZ(), MAX.getX(), MAX.getY(), MAX.getZ());
                Throwable tickFailure = null;
                try { level.getBlockTicks().clearArea(box); } catch (Throwable blockTicks) { tickFailure = blockTicks; }
                try { level.getFluidTicks().clearArea(box); }
                catch (Throwable fluidTicks) { if (tickFailure == null) tickFailure = fluidTicks; else if (tickFailure != fluidTicks) tickFailure.addSuppressed(fluidTicks); }
                rethrowCleanup(tickFailure);
            }
            public void drainOwnedEntities() {
                rethrowCleanup(GrowthOwnedResources.drain(ownedEntities, id -> {
                    var entity = level.getEntity(id);
                    if (entity != null) {
                        entity.discard();
                        if (!entity.isRemoved()) throw new IllegalStateException("Owned entity did not release: " + id);
                    }
                }));
            }
            public void verifyPlotAndEntitiesEmpty() {
                checkForeignEntities();
                if (plotOwned()) for (var pos : BlockPos.betweenClosed(MIN, MAX))
                    if (!level.isEmptyBlock(pos)) throw new IllegalStateException("Owned block survived cleanup: " + pos);
                if (!ownedEntities.isEmpty()) throw new IllegalStateException("Owned entities survived cleanup: " + ownershipSnapshot());
            }
            public void markPlotReleased() { GrowthFixtureV2.this.plotOwned = false; }
            public void releaseTickets() {
                rethrowCleanup(GrowthOwnedResources.releaseAll(tickets, chunk -> level.getChunkSource().removeRegionTicket(TICKET, chunk, 2, owner, true)));
            }
            public void verifyResourcesReleased() {
                if (GrowthFixtureV2.this.plotOwned || !tickets.isEmpty() || !ownedEntities.isEmpty())
                    throw new IllegalStateException("Unclosed cleanup resources: " + ownershipSnapshot());
            }
        }, contamination == null ? null : new IllegalStateException(contamination));
        cleanupNanos = System.nanoTime() - before;
        try { emit("CLEANUP", Map.of("sample", sample, "before", initial, "after", ownershipSnapshot(),
            "durationNs", cleanupNanos, "failure", failure == null ? "NONE" : failure.toString())); }
        catch (Throwable reporting) { if (failure == null) failure = reporting; else if (failure != reporting) failure.addSuppressed(reporting); }
        rethrowCleanup(failure);
    }
    @Override public void outcome(GrowthFixtureController.Outcome result) {
        long now = System.nanoTime();
        emit("SAMPLE", Map.of("result", result, "position", List.of(48,160,48), "terminalDay", level.getDayTime(),
            "admissionDay", admittedDay, "measurementDurationNs", admittedNanos == 0 ? 0 : Math.max(0, now - admittedNanos - cleanupNanos),
            "cleanupDurationNs", cleanupNanos, "pulseMetrics", metrics.summary(), "randomStateWitness", metrics.randomSummary(),
            "nativeCalls", Map.of("grow", growCalls, "age", ageCalls,
                "firstPulseTick", firstPulseTick, "lastPulseTick", lastPulseTick, "maximumGapTicks", maxPulseGap,
                "tickDigest", pulseTickDigest, "worldSeedCalls", admittedNanos == 0 ? 0 : 1,
                "blockedRootRandomTicks", autonomousBlocked)));
    }
    @Override public void mean(GrowthFixtureController.Mean result) { emit("MEAN", result); }
    @Override public void finish(boolean pass, boolean complete, Throwable original) {
        Throwable failure = original;
        try { restore(); }
        catch (Throwable cleanup) { if (failure == null) failure = cleanup; else failure.addSuppressed(cleanup); pass = false; complete = false; }
        try {
            emit("TERMINAL", Map.of("pass", pass, "complete", complete, "tick", tick(), "day", level.getDayTime(),
                "originalFailure", failure == null ? "NONE" : failure.toString()));
            if (output != null) output.close();
            System.out.println("WFT_GROWTH_V2_RECEIPT path=" + receipt + " sha256=" + sha(Files.readAllBytes(receipt)) +
                " rows=" + sequence + " complete=" + complete + " pass=" + pass);
        } catch (Throwable reporting) { if (failure == null) failure = reporting; else failure.addSuppressed(reporting); pass = false; }
        finally { active = null; }
        if (pass && failure == null) helper.succeed();
        else {
            if (failure != null) failure.printStackTrace(System.err);
            try { helper.fail("V2 growth calibration failed: " + (failure == null ? "INCOMPLETE" : failure.toString())); }
            catch (RuntimeException nativeFailure) { if (failure != null && failure != nativeFailure) nativeFailure.addSuppressed(failure); throw nativeFailure; }
        }
    }
    private void restore() {
        isolation.disarm();
        boolean dirty = plotOwned || !tickets.isEmpty() || !ownedEntities.isEmpty();
        if (environmentOwned) {
            var rules = level.getGameRules(); var data = (ServerLevelData)level.getLevelData();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(weather, level.getServer());
            level.setDayTime(day); data.setClearWeatherTime(clearWeather); data.setRainTime(rainTime); data.setThunderTime(thunderTime);
            data.setRaining(raining); data.setThundering(thundering); level.updateSkyBrightness(); environmentOwned = false;
        }
        if (dirty) throw new IllegalStateException("Unclosed sample resources: " + ownershipSnapshot());
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event) {
        var fixture = active;
        if (fixture != null) fixture.controller.cancel(new IllegalStateException("Native server stopped before fixture terminal"));
    }
    private void emit(String type, Object payload) {
        if (output == null) throw new IllegalStateException("Growth receipt not open");
        JsonObject row = new JsonObject(); row.addProperty("type", type); row.addProperty("sequence", sequence++);
        row.add("data", json.toJsonTree(payload));
        try {
            var buffer = ByteBuffer.wrap((json.toJson(row) + "\n").getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) output.write(buffer);
            output.force(true);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Growth receipt write failed", failure); }
    }
    private static String sha(byte[] data) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }
}
