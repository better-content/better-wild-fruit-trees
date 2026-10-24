# Cause-first light readiness proposal — RETIRED, NOT implemented

> This historical calibration proposal is superseded by explicit retirement of the test category.
> It does not authorize implementation, new diagnostics or native campaigns.

## Decision and evidence

Do not add a readiness barrier to the acceptance fixture yet. Controlled-light-06 gives
**MECHANISM_SUPPORT_NOT_HISTORICAL_CAUSE** for orange samples 5/6 at pulse 208:
common ordered prefix, same-query SKY 15 versus 13, can-see 1 versus 0, immediate adequacy
1 versus 0, then different next write HEAD positions, with no in-flight write. Samples
0/1/3 have no corresponding adequacy change; 2/4/7 match. This distinguishes a mechanism
from mere publication coincidence, but does not establish historical causation or stability.
Run 1's persimmon failure remains blocking; run 2's exact-26 pass does not erase it.

The hardened comparator binds each unchanged trace to exactly one actual path/whole-file
SHA/count/complete receipt and exactly one matching **later** post-assert marker in the
hash-bound retained log. This is correlation, not cryptographic log authenticity. Original
four observation statements and their full-file stripped SHA remain unchanged in this work.
No fixture implementation, native experiment, rates/references/tolerance/count change,
readiness barrier, RNG wrapper/reset or light-state replacement is authorized here.

## Pinned public API findings (not assumed names)

Read-only `javap -p -c` inspection of Forge 1.20.1-47.4.13 mapped archive SHA-256
`86ce0a0aa09201b566430dfec17f7a03fd2acbd16e04b17992b6ad5e78df4b0f` establishes:

| API | Pinned reality |
| --- | --- |
| `ServerChunkCache.getLightEngine()` | Public, returns `ThreadedLevelLightEngine` (also a bridge returning `LevelLightEngine`). |
| `ThreadedLevelLightEngine.tryScheduleUpdate()V` | Public; schedules worker `runUpdate` when staged tasks or underlying light work exist, using an atomic scheduled flag. It is not an acknowledgement or drain future. |
| `ThreadedLevelLightEngine.waitForPendingTasks` | **Absent** from the complete pinned class and its inspected superclass, not merely private. Do not import a newer-version API or invent its signature. |
| `ThreadedLevelLightEngine.runLightUpdates()I` | Public override **throws UnsupportedOperationException**, “Ran automatically on a different thread!”. Never call it on the server thread to force a flush. |
| `LevelLightEngine.hasLightWork()Z` | Public inherited method checks the sky/block engines only. It does not account for commands still in the sorter mailbox, worker mailbox, or threaded staged `lightTasks`. False is not a global queue fence. |
| `LevelLightEngine.getMinLightSection()/getMaxLightSection()/getLightSectionCount()` | Public integer accessors; use the actual dimension/light range rather than inventing a fixed Y limit. |
| `BlockableEventLoop.managedBlock(BooleanSupplier)V` | Public, inherited by the server. While predicate false it polls tasks/waits; predicate already true permits immediate return. It is not a light-fence API by itself. |
| `ServerChunkCache.pollTask()Z` | Public, delegates to its **private** MainThreadExecutor; that executor updates distance management, then schedules light updates and polls tasks. |
| `MinecraftServer.pollTask()Z` | Public; polls server tasks, then chunk tasks only when `haveTime()` permits. Do not assume a long synchronous measurement callback automatically services chunk tasks. |

`checkBlock` queues a PRE_UPDATE through the chunk-priority sorter, not directly into the
solver. Worker `runUpdate` batches at most 1,000 staged tasks, runs PRE_UPDATE callbacks,
then `super.runLightUpdates`, then POST_UPDATE callbacks. `addTask` and both mailbox fields
are **private**. A no-op on some unrelated executor is not a fence through that pipeline.
`initializeLight`/`lightChunk` return futures, but are native initialization/mutation routes
(e.g. light-correct state), not passive wait APIs; do not misuse them as readiness queries.

Thus **the pinned public surface does not supply a proven pending-light drain future**.
A plain `future.join()` on the server thread risks deadlock if completion needs chunk/server
callbacks. A managed wait can avoid that class of deadlock only with a real, correctly
ordered completion condition and appropriate task pumping; it may execute unrelated tasks
and change RNG consumption/world state. No such neutrality is proved by its name.

## Exact ordering and footprint a future candidate would need

1. Before admitting measurement, cover prior fixture teardown, current clear, root/branch/
   leaves setup, heightmap/section transitions, and all resulting native light commands.
   Preserve the existing five-tick warmup, clock, fertility changes and seed recipes; do not
   replace them with repeated reseeding or retries. Admission needs a real queue fence, not
   just root brightness or `hasLightWork == false` at one instant.
2. Known original clear is inclusive `(4,160,4)` through `(92,220,92)` around root
   `(48,160,48)`: chunks X/Z 0..5. Pinned DT `TreeHelper.ageVolume` uses inclusive origin
   plus `(-radius,0,-radius)` through `(radius,height,radius)`: here X/Z 28..68, Y 161..209,
   plus each native leaf update's neighboring accesses/writes. Growth adds its native
   branches/leaves. Root-cell readiness alone is inadequate.
3. Conservative **initial candidate**, not a proven universal bound: all clear chunks plus
   one horizontal chunk halo, X/Z -1..6, full dimension light-section range. Sky propagation
   follows columns vertically, not merely the 61 cleared Y layers. Prove against complete
   actual read/write/task coordinates and native solver neighbor dependencies; any escape
   invalidates admission rather than silently loading/expanding during measurement. Preload
   status/light data before seeding; do not generate chunks opportunistically inside a wait.
   Surrounding terrain/other fixtures can still feed light across the boundary.
4. An initial fence alone cannot stop writes in growSignal/ageVolume from queuing new work.
   Even a fence after every **whole pulse** or between growSignal and ageVolume cannot make
   writes and later reads *within* those synchronous calls deterministic. Controlled-06
   pulse 208 is an ageVolume pulse; its first causal read follows an already matching prefix
   containing earlier native operations. The native microstep, not just pulse boundary,
   matters.
5. A sufficient serialization contract would need, before every relevant original light
   query, publication of all previously issued native opacity/section commands that can
   affect it, without future work overtaking it; preserve the exact original invocation,
   geometry/season/Forge checks and both RNG streams. This is a **fixture protocol change**,
   not observation. A write HEAD is too early to fence that write; its mutation/light tasks
   have not happened yet. A RETURN is after mutation/nested reads and cannot repair those
   earlier reads retroactively. Reentrant queries inside a still-running mutation require
   explicit handling; do not block from the light worker or recursively wait on oneself.

## Minimal controlled next experiment — requires separate authorization

**First gate: prove a real fence, not add a guessed barrier.** Retain this public-API absence
result. If the lead wants a candidate, request a separate native-only diagnostic hook exposing
the existing PRE/POST pipeline or another independently verified acknowledgement mechanism.
Prove sorter admission, per-chunk/type/priority ordering, all-footprint coverage, full solver
publication, late submissions, reentrancy and bounded failure. A POST_UPDATE marker for one
chunk is not automatically a fence for other priorities/chunks; audit the sorter before
asserting FIFO. An invoker of private `addTask` is not a pinned public API and needs explicit
new admission. No hook is authored or approved by this proposal.

Then preregister a tiny mechanism experiment using the unchanged fixed roots, seeds, pulse /
age schedule, clocks, original native growth calls and endpoints: current protocol versus
candidate fence at declared microstep boundaries, with exact ordered events and task/fence
provenance. Do not turn existing observation hooks into hidden controllers. Use fresh worlds,
a fixed finite run count (e.g. two runs per declared arm, no retry-until-green), matching
compiled inputs within an arm, and retained complete failed outputs. Bound waits by a
predeclared diagnostic timeout; timeout/footprint escape/late unaccounted work is inconclusive,
never a leaf-suitability bypass or success. Require evidence that no extra gameplay/RNG
callbacks, game ticks or world-time advancement occurred during pumping; if this cannot be
proved, the candidate cannot claim preservation of the original RNG/call contract.

Only after queue-ordering proof should a separate versioned fixture protocol be considered.
Its admission must explicitly reconcile original source-stripping controls; never silently
relax the guard. Keep references, strict 20% tolerance and all 26 required tests unchanged.
If a deterministic candidate falls outside existing references, retain that failure for lead
review; do not recalibrate/expand tolerance automatically. Initial-only and pulse-boundary
readiness may be useful diagnostic arms, but are **not** determinism guarantees.

## Separately authorized SOURCE queue mechanism v1 (still no fence admission)

The later [light queue proof profile](light-queue-proof.md) implements a distinct, explicit
`-PlightQueueProof=true` SOURCE-only mechanism experiment, not the barrier proposed above.
It retains the original26 and adds exactly three structural cases with two observations each.
Its conditional command/admission/batch/publication/ack provenance explicitly leaves global
coverage incomplete. Native application/liveness are lead-owned and pending; no counter-zero,
future acknowledgement or 29-case diagnostic completion admits a fixture repair. See that
versioned document for mutation-route gaps, AP synthetic-member warnings, exact retained format
and the strict future admission decision. The original growth protocol and this proposal's
rejection of an unproved fence remain unchanged.

## Separate persimmon ownership

Orange observations do not establish persimmon's cause. The pinned DT Fruits Delight
7079680 species JSON identifies persimmon family **spruce** (orange is oak), signal energy 12,
up probability 4 and persimmon leaves; its native path, measured reference and failure need
independent owning review. Current source `nativeFreshGrowthCalibration` iterates the species
set with the same fixed plot; the ordered scope deliberately admits only orange seeds 0..7.
Do not claim persimmon has been observed just because both occur in that test.

Before a persimmon candidate, independently bind its actual native branch/kit/leaves registry
identities, original seeded calls, full ordered light/decision/write prefix and productive
footprint. An explicitly versioned separate dictionary must cover its actual spruce branch
and persimmon leaf states; **do not reuse or silently widen** `original-orange-blocks-v1`,
which would emit UNKNOWN for those writes. Verify both species under their own exact contracts,
then the required present/absent/full native gates once separately authorized. No orange
mechanism finding, synthetic comparator positive, or passing modern run waives persimmon.
