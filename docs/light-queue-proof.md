# SOURCE native light queue mechanism v1

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
