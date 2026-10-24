# SOURCE native light queue mechanism v1 — RETIRED

> Historical calibration diagnostic only. Explicit user retirement prohibits further launches or
> treating the following protocol as an active gate. Source and evidence remain retained.

**Not a fence, protocol repair, original-growth qualification or deployment authorization.**
Explicit `-PlightQueueProof=true` selects a separate nonshipping source set/config/package.
False/absent does not create that source set, register its mixins, allocate its ledger or perform
its I/O. Only `gameTestServer` receives the source/property/config. Calibration, absence flags
and original ordered growth mode are incompatible. Ordinary client/server runs remain unchanged.
The original four observation mixins and original fixture observation/scope lines are unchanged.

## Preregistration (one lead-owned invocation; no helper native run)

Exactly three required cases, each exactly two observations (ordinal 0 then 1), no retries:

| Case | Real native experiment |
| --- | --- |
| SAME_CHUNK_STABLE_PRIORITY | Same chunk's dynamic-priority block checks and `lightChunk(true)` PRE/POST; record actual admission values, do not assume stability. |
| CROSS_CHUNK_MIXED_PRIORITY | Neighbor chunk's dynamic-priority check plus native fixed-priority `updateSectionStatus(false)`, then `lightChunk(true)`; actual priority mix is an observation, not an invented priority. |
| NEIGHBOR_PROPAGATION | Real emission change across a chunk boundary and `lightChunk(false)` native propagation arm. |

Each observation alternates glowstone then stone at the source. Setup places stone below source
and neighbor to keep their section nonempty. These are explicitly mutative experiments in their
own 97×225×97 GameTest template, **not** passive growth observations. Original roots, clocks,
seeds, references, endpoints, rate schedules, tolerance and all original mandatory tests are unchanged.
Runtime inventory must equal the retained original 26 names plus these 3, all required/no retry.
Native `TestFunction.getTestName()` is an unqualified lowercase **method name** here: every
holder uses `@PrefixGameTestTemplate(false)`. Namespace ownership comes from public
`getStructureName()` parsed as a resource location, not from a guessed test-name prefix.
The literal manifest therefore contains all 29 native method identities. The cold oracle filters
only by actual template namespace, then rejects extras, missing names, duplicates, nonrequired
cases or retry/success-count changes. It never filters by the expected-name list.
Default `verifyNativeLog` still demands 26; only this profile demands 29 and six unique outcome rows.
The existing native diagnostic classification remains blocking. Source mechanism outcomes cannot
waive persimmon, bayberry or any other original failure.

Readiness uses only nonloading `getChunkNow` and actual native full-status >= BLOCK_TICKING,
bounded at 100 ticks. Native future polling is asynchronous GameTest scheduling, bounded at 100
ticks, with the reviewed real `tryScheduleUpdate` scheduling API. No join, managed-block pump,
direct solver invocation, fake API, light freeze or suitability override. A timeout is a required
failure/incomplete observation, never a retry or a successful fence. Completed futures are read
only with `getNow` after exceptional/cancelled checks. The fixture may retain its own future/chunk
across its bounded steps; **the observer never retains a world, chunk, runnable, tag or throwable**.

## Original pipeline provenance

The separate required mixins observe:

1. Exact five-argument native `addTask` HEAD/RETURN and the native original `ProcessorHandle.tell`
   call before/after; command IDs and primitive task identity registration precede send.
2. Original message factory's returned function identity (temporary accessor only), linking command
   to the actual sorter function, not a fabricated public marker API.
3. Original sorter `lambda$submit$12` entry/exit and **the original** `IntSupplier.getAsInt` result
   exactly once. No priority supplier requery.
4. Native `lambda$addTask$15` original fastutil append exactly once, then scalar staging mirror.
5. Actual `Math.min(size,1000)` result, first-N mirror, PRE original runnable invocation, native
   superclass solver BEFORE/AFTER, actual visible-map PUTFIELD publication, POST original runnable
   invocation, AFTER each original iterator.remove, and native runUpdate return.
6. Unique issued POST command associated with each actual native future acknowledgement; ack
   does not imply retirement, callback completion, global admission or drain.
7. Native lifecycle setter requests plus cold before/after actual flags; original LIGHT-ticket
   set-remove return boolean (an attempted removal with false is **not** proven ticket removal).
8. Original release-ticket tell/notification execute callback identities, original main-executor
   callback entry/return and actual LIGHT removal attempts/results. Callback origin links include
   native command and solver-run IDs. No claim of callback/RNG neutrality.

Original redirected calls execute once, preserve original results, and rethrow original throwable
objects; task/list/supplier/send/ticket/callback-send exception identities/class identities are scalar
observations only. Solver entry without exit or other missing RETURN is incomplete; solver throwable
class itself is not intercepted. Exceptions are never wrapped or converted into successful probes.

### Pinned private anchors and AP limitation

Forge mapped SDK SHA256:
`86ce0a0aa09201b566430dfec17f7a03fd2acbd16e04b17992b6ad5e78df4b0f`.
Actual SDK, both real Mixin 0.8.5 processors, no private enum stubs/reflection. `@Coerce Object`
receives the real private TaskType solely for Enum.ordinal; actual SRG selectors include m_9317_,
m_284002_ and m_140595_. All required/expect/allow counts remain exact and noncancellable.

**Four AP warnings remain:** javac's language model hides the synthetic addTask/submit lambda
members. They are not described as resolved TypeElements. The independent actual pinned classfile
oracle must find every selector uniquely with its exact descriptor, verify original invocation
counts, and verify actual AP refmap mappings. Only these four specific synthetic warnings are
accepted; any other AP warning fails the control. Runtime Mixin application remains lead-pending.

## Coverage audit — why this is not global closure

The pinned `lightTasks` field is referenced by constructor initialization, tryScheduleUpdate reads,
runUpdate's native subList/iterator removals, and lambda$addTask$15 append. The mirror observes the
actual append and actual processed first-N range, not submitted-minus-retired as a substitute.
Preexisting/unmapped entries, duplicate identities, changed mirror size, missing phases or overflow
invalidate conditional provenance; unknown range retirement emits unknown IDs, not invented links.

Threaded native mutations routed through addTask include block checks, section-status changes,
chunk-status clearing, queue-section-data, retention/enabling and initialize/light-chunk work.
**Outside that route:** superclass solver internal propagation/storage writes; raw engines exposed
by `getLayerListener` (including mutation interfaces); retained mutable DataLayer/storage aliases;
constructor/pre-scope work; direct setter/field writes by other transformers; sorter priority changes,
mailboxes and callbacks after detach. Solver begin/publication/end observations do not cover every
internal node mutation, section-dirty transition or direct external raw-layer call. Ticket observation
covers original remove paths, not a complete ticket ownership/addition audit. Native notifications
can originate from mixed batches and need not belong to a single source command.

Thus every payload explicitly sets `globalCoverageIncomplete=true`, `incomplete=true`,
`untrackedLateWorkCannotBeExcluded=true`, `globalDrainProven=false`, and callback neutrality false.
`conditionalTraceIncomplete` describes detected bounded-record/provenance defects in the observed
stream only; false does **not** close the unobserved domain. Identity hashes are conditional scalar
keys, not object ownership proof; duplicate registration invalidates them, engine collisions and
unobserved identity reuse cannot be ruled out. Independent atomic reservation/commit slots do not
provide a global native happens-before total order or quiescence at detach.

## Bounds, bindings and retained format

262144 × 8-long primitive records, 16384 commands/callbacks, 65536 bounded role-map slots,
32 threads × 32 frame depth, four engine mirrors. No hot allocation, I/O, gameplay/RNG queries or
retained native objects. Primitive metadata and JDK atomics only in observer paths. Arrays exist
only after this profile's fixture boot. Native fixture cold queries/logging are explicitly separate.

Post-detach CREATE_NEW `light-queue-proof/pipeline-v1.wftq`: magic WFTQPR01, u32 Java-properties
header length, header, u32 record count, then records (u32 commit followed by eight i64 fields).
Fields: kind, slot, command/run/callback ID by kind, position key, a,b,c, thread ID. Kinds 1–17 are
named in QueueProofLedger; 18 original throwable scalar, 19 original tell begin/returned. Callback
b=-1 records send identity, b=-2 records originating command/run; b=0/1 records entry/return.
The separate whole-file SHA receipt is emitted after close; partial/duplicate outputs are retained.
ServerStopping flushes incomplete evidence if any case aborts. Flush failure never replaces a
pending original gameplay throwable.

Pre-run bindings include clean Source commit, actual main/nativeProbe/proof class hash manifest,
actual SDK hash, required29 profile, actual input receipt hash and nonce-owned attempt directory.
The log consumer verifies whole-file SHA, bounds/magic/schema/profile/nonce/source/SDK, binding
manifest/input hashes, committed record counts and six unique ACKs bound to issued POST IDs.
These hashes establish consistency, **not authenticity** or transformed-runtime class identity.
All classes/config/refmaps/bindings/payloads are excluded from runtime archives with negatives.

## Scoped pre-clear readiness candidate — SOURCE IMPLEMENTED / NATIVE PENDING

Correcting registry identity does not resolve native DUMMY/pending block-entity failures during
structure clearing. Existing BLOCK_TICKING checks in the test **body are too late** for native
spawn/clear preparation. Source preparation below is not native success or warning closure.
Strict failed-operation WARN audit remains blocking, including failures elsewhere in the world.
Only the lead may run the separately preregistered probe after Source handoff.

This preparation affects **only** the exact registered proof identities
`samechunkstablepriority`, `crosschunkmixedpriority`, `neighborpropagation`, each with template
`better_wild_fruit_trees:growth`, and only in this explicit source profile/GameTestServer. Validate
the full owned29 registry before admitting these identities. Neither batch-name prefix, template
alone, expected-name-only filtering nor a blanket StructureUtils wrapper is a sufficient scope.
The old26, original4 observers and every ordinary/default RunConfig must remain outside it.

Pinned native anchors, inspected before proposing any hook:

* `GameTestInfo.spawnStructure(BlockPos,int)` has exactly one call to
  `StructureUtils.spawnStructure(String,BlockPos,Rotation,int,ServerLevel,boolean)`.
  The latter computes its own rotated bounds, calls its original forceLoadChunks, then exactly
  one original clearSpaceForStructure, then original creation/tick/event handling. Preserve all.
* `GameTestInfo.clearStructure()` derives actual bounds from its native structure block entity
  and calls clearSpaceForStructure exactly once. Do not manufacture bounds from an assumed origin.
* Native `clearSpaceForStructure(BoundingBox,int,ServerLevel)` inflates min X/Y/Z by -2/-3/-3
  and max X/Y/Z by +3/+20/+3, inclusively. Its horizontal chunk footprint is therefore exactly
  floorDiv(minX-2,16)..floorDiv(maxX+3,16) × floorDiv(minZ-3,16)..floorDiv(maxZ+3,16).
  The unchanged97×225×97 template needs at most64 horizontal chunks; overflow/arithmetic errors,
  larger bounds or unsupported rotations must fail rather than expand silently.

Implemented Source contract (runtime application still pending):

1. Typed required wrappers around the above native GameTestInfo spawn/clear methods and exact
   native clear call sites. Finally-safe MixinExtras0.5.4 WrapMethod scopes are pinned and present
   only on the explicit proof source compile/runtime configurations, never the default profile.
   Qualify using actual public native name/template identities. Every nested GameTestInfo call,
   including unqualified old26 calls, must push a neutral mask rather than inherit a proof scope.
   A one-use scope token may qualify only the original spawn's exact clear call site; unrelated
   nested/direct clears must not inherit it. Reject inconsistent nesting/reentry. Do not put a
   generic readiness barrier around every native clear.
2. At the exact qualified clear invocation, after original native forceLoadChunks, calculate
   footprint from its actual BoundingBox argument. Keep arguments/floor/rotation/body/seed and
   call counts unchanged. Prepare only this cold source fixture, never a growth/light drain.
3. Use real native ServerChunkCache/ChunkMap and that cache's BlockableEventLoop on its owning
   thread. Snapshot initially forced chunks; force only missing footprint chunks and remember
   only that added lease. Run the actual private cache runDistanceManagerUpdates through a typed,
   verified native-only invoker so holder.updateFutures has actually happened before selection.
4. Select each actual visible ChunkHolder, require native status >= BLOCK_TICKING, capture the
   **actual getTickingChunkFuture**, and require completed Left<LevelChunk>, never Right/null,
   exceptional/cancelled or an unrelated FULL future. Bounded cache-loop managedBlock is allowed
   only for this declared mutative/non-neutral preparation; never join or manually tick worlds.
   Use one shared30-second budget, not renewed per chunk or after failure.
5. Recheck exact visible holder and future identities, status, chunk key/world, owning thread
   and lease ownership immediately before original clear. Preserve original clear exactly once
   and restore only added forced tickets in finally, including original failure/timeout paths.
   Retain original Throwable identity; attach cleanup failure rather than replace it. Never
   release original native forced tickets or claim callback/RNG neutrality.
6. Source controls required before handing off a candidate: all three exact identities versus wrong name/template/
   namespace/default/old26; neutral nested masks and one-use/reentrant scopes; original call once
   and Throwable/finally preservation; exhaustive97-width alignment/rotation/negative-coordinate
   footprint edges and64 cap; preexisting/partial-added ticket restoration; DM-before-selection;
   completed-Right/error/pending/stale holder/future/wrong-world/deadline/thread negatives; real
   actual-SDK typed accessor/invoker/handler descriptors, mappings and required-apply counts;
   unchanged default/archive/original-fixture/old4 gates and all four retained synthetic AP warnings.

The pinned native prepareTickingChunk pipeline invokes LevelChunk.postProcessGeneration and
ServerLevel.startTickingChunk in its original completion path. The Source adapter waits for that real
TICKING future; it must **not** call postProcessGeneration, purge pending tags, fake block entities,
return air defensively or exempt warnings. Rail's separately verified preparation is a useful
API/invariant reference, not proof that it covers WFT's warning producers. Even with Source controls, only one newly preregistered lead invocation with strict full native warning audit
and six correlated outcomes can evaluate it. Any remaining warning stays blocking; do not widen
scope onto old26 or claim historical persimmon causation from an improved fixture.

Preparation binds `fixturePreparation=exact3-native-ticking-v1`, common SDK SHA256
`e2de9240ce39af783a83bcd315445b84d675c01e4655f3ad55c163071ce15fee` and Forge runtime wrapper SHA256
`7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264` (MixinExtras0.5.4). The four
additional reserved mixins/accessors do not modify the original4 observers. Their GameTest-only
selectors are deliberately unmapped development names and independently checked against the exact
SDK classfile; ordinary observer selectors retain their real SRG mappings. Header/consumer/fixture
binding checks reject a payload lacking this preparation identity.

The shared30-second readiness budget starts before added ticket admission and is never reset.
It cannot preempt an individual native callback/FULL-generation call; the owning outer native
runner remains responsible for a hard process deadline. This is not a bounded gameplay wait or
RNG-neutral observation. Temporary fixture ports necessarily retain their world/holder/future
for the synchronous setup lease; these are not observer buffers and are not retained after scope.

## Native25 observer defect and blocked follow-up

Native25 at2372a2e failed persimmon (341.75 vs282.125) and allthree new cases
(`Index -16 out of bounds for length48`); there were no six observations. Root cause in the
observer: setup intent−2 reached `counts[slot*8]` because only−1 was treated as unknown.
This happened before an original task body and violated the observer forwarding contract.
The Source repair separates a nonthrowing observer facade from its bounded primitive engine,
canonicalizes unknown slots to−1, guards public tokens/counters, saturates reservations/counters,
masks overflowing/unknown nested frames, and resets fixture intents in finally before scheduling.
Internal diagnostic faults mark incomplete and increment `observerFaults`; catches contain no
original pipeline call. The separate temporary-reference `NativeObserver` adapter also encloses
all native enum/message/accessor/position/hash/Throwable derivations in diagnostic catches.
All eight observer mixin classes route those derivations there; the factory return getter has
its own diagnostic-only catch. Original list/remove/supplier/task/send/execute operations remain
outside diagnostic catches; original exceptions are recorded safely and rethrown by identity. This is not an assurance against arbitrary VM/process termination.

The new inert JDK controls (46 suite tests total) exercise idle−2/−1/less-than−2/at-least6, foreign thread, invalid
long tokens, bounded ID/record/engine/thread/frame capacity, all48 counter cells and scalar
saturation/underflow, unknown/racy frame masking, deliberately malformed diagnostic state,
original exactly-once return/Throwable identity and finally reset. The actual SDK gate additionally
compiles and invokes actual untransformed observer mixin bodies in an inert realm using real SDK
and Mixin declarations (no fake SDK/World/engine). It covers nullable/wrong enum/message/accessor/
position metadata, diagnostic Error, original list/supplier/task counts/results/exact Throwable.
Scheduler/solver/ticket execution is not run in that realm: pinned call-site/selector checks and
source separation controls cover those boundaries, not native liveness. `javap` of the adapter
rejects NEW/INVOKEDYNAMIC, pipeline-operation references and observer I/O. Native runtime remains pending.

A separate COLD bounded1024-row preparation audit records correlated scope IDs for admitted/neutral
INFO/SPAWN entry/exit and exact qualified clear consumption/return. Raw bounds are recorded before
footprint validation; valid ENTRY includes inflated bounds and indexed canonical chunk keys BEFORE
admission. The same ordered ≤64-tile footprint goes to policy and masks. Before/added-attempt/READY/
after forced identity masks and verified TICKING mask include known/incomplete flags. Preparation
failure emits AFTER_POLICY_CLEANUP even before READY; cleanup failures also emit incomplete rows.
A rejected unsupported footprint emits explicit before-admission rejection. Native references in
the lease wrapper are cleared in close; the audit buffer retains strings/primitives only. It performs no terrain/block-entity
query and no observer I/O. ServerStopping emits CREATE_NEW
`light-queue-proof-preparation.properties`, bound to Source/nonce/class-set/native-input receipts.
Missing/dirty/tampered bindings, symlink ancestry, existing output and overflow are tested.
Identity masks describe actual forced metadata observations and attempted lease ownership, not
callback neutrality, intervening actor exclusion or global lighting. Pure port controls cover
64-bit/negative-key/full READY, partial force throw, original failure identity and exact prior
mask restoration. Actual new runtime receipts
must be reviewed, not inferred from successful compilation or from a batch-header timestamp.

Native25 had no such audit. Its actual scope admission and warning-footprint membership are
**UNKNOWN**. The retained read-only `build/source-queue-proof-controls/native25-warning-inventory.json`
contains all18 matching raw warning/summary lines with log SHA, line positions, coordinates and
relative batch-header timing. That timing is not preparation timing. Native failed block-entity
loads are established; the exact pending-tag/producer/root cause is not. No speculative widening,
manual postprocessing/purge, warning exemption, old26 barrier or historical-cause inference follows.
**DO NOT RERUN29 until lead reviews the observer repair and scope audit design.** Persimmon and
strict failed-operation WARNs remain blocking. Source/JDK/AP success is not native acceptance.

## Strict future admission decision

**NOT ADMITTED as a fixture fence or repair.** Neither counter zero, hasLightWork=false nor a
cheap lightChunk(true) future proves a global drain. Even native 29/29 is this diagnostic profile,
not the original main26 shipping/native qualification. Only the lead may run the one preregistered
native invocation. No retry-until-green, fixture delay, growth barrier or reference adjustment.

A later finite-domain proposal would require independently proven block-ticking readiness,
complete mutation/admission/priority/batch/publication/callback coverage, no unknown/prior/late work,
non-colliding command provenance, a justified progress/ack ordering contract, accounted ticket and
lifecycle effects and callback/RNG/reentrancy neutrality. This v1 deliberately does not establish
those premises. Untracked late work always leaves the global inference INCONCLUSIVE. Preserve
all outputs and original failures; do not activate, stage, package, deploy, refresh or purge.
