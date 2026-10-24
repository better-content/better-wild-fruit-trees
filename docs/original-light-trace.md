# Original orange light trace — RETIRED

> Historical calibration diagnostic only. Explicit user retirement prohibits further launches or
> treating the following protocol as an active gate. Source and evidence remain retained.

This is observation, **not** a growth/lighting protocol repair or evidence that historical
orange drift has been explained. It adds no GameTests and changes neither the exact-26
main gate nor the original root `(48,160,48)`, seeds, five-tick delay, clock, pulse limit,
age interval, endpoint, references or strict tolerance. A full-file SHA-256 control strips
only the four new observation statements/scope lines and requires byte-for-byte equality
with the witness-bearing original fixture at `8d4c441`.

## Default qualification is diagnostic-free

Default/`-PgrowthDiagnostics=false` native qualification keeps all required26 tests and the
unchanged fixture arithmetic, roots, seeds, warmup, clocks, endpoints, references and tolerance.
It does **not** source/register `nativeProbe` or its four hooks. `OriginalLightTrace.begin` returns
before constructing a session or calling ordered observers unless the diagnostic JVM property
is true. The four original fixture statements/scope lines remain unchanged and become no-ops.
`LegacyGrowthWitness` also requires opt-in before sample scans or event world/entity queries;
its Forge event entry handlers return immediately when disabled. No actor purge/barrier is added.

Use explicit validated `-PgrowthDiagnostics=true` only for optional native diagnostics.
`-PoriginalEventTrace=true` or `-PlightQueueProof=true` implies diagnostics automatically;
combining either with explicit `-PgrowthDiagnostics=false` is rejected. The two diagnostic
profiles remain mutually exclusive and are **deferred from the current fresh-Dist critical path**.
Client/ordinary server runs receive neither diagnostic property, source set nor configuration.
The source set and static API/AP/archive controls remain available; compiled evidence on disk
is not evidence of a runtime classpath. Actual RunConfig guards check both all-source and mod-source
sets, exact diagnostic-free/opted-in SDK args, and native-only properties. The before-own-prepare
receipt records `growthDiagnostics`, `actualAllSourceSets` and `actualDiagnosticMixin` (`NONE` by default).
Existing diagnostic class manifests remain restricted to their explicit ordered/queue profiles.

Source log gates require no light-trace/witness markers; no required26 assertion or error/WARN
classification was removed or filtered. This isolates optional observation, not a native timing
fence or protocol repair. Any eventual pass qualifies only the current unobserved default;
it cannot establish that observers caused historical variation. Historical failures/evidence remain.

## Isolation and actual observations

`nativeProbe` is a separate source set added only to explicitly opted-in `gameTestServer` mod sources.
Its required mixin configuration is an explicit opted-in native-run argument, not a global Mixin
configuration or manifest entry. Client/server runs and the runtime JAR do not include
its classes/resources. The JDK-only ledger lives alongside the already excluded GameTest
classes. The artifact gate rejects both native trace resources and all GameTest classes.
The configuration's `gametest.nativeprobe` package and **all descendants** are reserved
exclusively for its four configured Mixin classes. Ordinary `NativeStateCodes` and
`StateCodebook` helpers instead live in the sibling `gametest` package within the native
source set. Mixin forbids ordinary direct class loading from a reserved package; ordinary
JDK/AP compilation alone cannot establish this loader rule or actual event delivery. Source
and compiled-class guards reject every unconfigured/non-Mixin class there, including nested
helper classes and descendant packages. Startup delivery/codebook completeness still require
actual native validation; no previous pure/AP pass substitutes for that evidence.

Outside the original orange sample loops, hooks return before observation conversions,
callbacks/results, or engine lookups. Forwarding hooks still execute the original invocation
exactly once when inactive, as required for native behavior.

Pinned bytecode was inspected before selecting hooks:

* DT Curse file `7502488`, mapped SHA-256
  `ca21e8d1796bdd66b4fad163822761492262354181788b31783497b0ae322315`:
  `DynamicLeavesBlock.hasAdequateLight(BlockState,LevelAccessor,LeavesProperties,BlockPos)Z`
  calls `LevelAccessor.canSeeSkyFromBelowWater(BlockPos)Z` once, and conditionally
  `LevelAccessor.getBrightness(LightLayer,BlockPos)I` once. Each redirect forwards the
  original arguments/invocation, then records its actual result. No re-query occurs.
  RETURN callbacks record actual adequacy results at all three original return sites.
* Forge 47.4.13 mapped SHA-256
  `86ce0a0aa09201b566430dfec17f7a03fd2acbd16e04b17992b6ad5e78df4b0f`:
  HEAD/RETURN callbacks around `SkyLightSectionStorage.getLightValue(J)I` record actual
  receiver identity and returned light, with observed publication versions before/after.
  This method delegates to `(JZ)I`; the diagnostic never invokes either method itself.
* `LayerLightSectionStorage.swapSectionMap()V` has one original
  `PUTFIELD visibleSectionData:DataLayerStorageMap`. Its callback runs **after** that
  instruction. It does not redirect/replace the field write or wait for the light worker.
* All four original `LevelChunk.setBlockState(BlockPos,BlockState,Z)BlockState` return
  sites record the argument position, requested-state identity, actual returned-old-state
  identity and whether that result was non-null. Null returns are retained too; this is an
  attempted-write RETURN trace, not an invented post-write world-state query.

## Bounds and interpretation

Only original orange samples 0–7 activate the ledger. Scope is detached in `finally`,
including when the original callback throws. Pulse indices are explicit and bounded at
4096. The ledger retains only primitives/primitive arrays, atomics and immutable scalar
publication records: no level, block position/state, registry, codec or storage object.
There are at most 16 storage identity slots, 64 detailed owner-thread events and 64 worker
publication tail entries. Counts per pulse/channel and publication versions saturate or
stop at a one-million-event diagnostic budget. Failed publication-counter CAS, excess
storage IDs, unmatched sky entry/return and invalid pulse/seed scope cannot become an
acceptance substitute; diagnostic overflow is explicit. No observer monitor/wait/join,
lighting readiness barrier, chunk load, RNG draw, extra world/property/registry query,
block change or file/log output is introduced inside the original measured loop.

`ORIGINAL_LIGHT_PULSE` emits channels in this order: actual skylight read,
can-see-below-water result, brightness result, adequacy result, attempted-write RETURN
position. Counts and hashes for the first four use only positions and actual results.
The write hash is **position-only**. None includes object identity, publication sequence,
version, slot or thread ID. These channels can locate a first cross-run divergence; they
are bounded noncryptographic summaries, not full-state/geometry equality proofs. Opaque
state identities cannot be decoded or compared across JVMs. The separately versioned
canonical-state refinement below does not change these original five channels.

Opaque state/storage identity hashes, event sequence and version intervals are separate
provenance/tails, **not comparable between JVMs**, and identity hashes can collide. A
publication version counts *post-PUTFIELD callback observations in this sample*, not exact
visible-map generations. A worker can publish and be preempted before the callback; a
read can happen in that gap. Detachment intentionally takes no worker barrier, so late
in-flight publication observations can race the printed snapshot. `detachedNoBarrier=true`
and the publication-version label explicitly disclose those limitations. These are not
coherent engine snapshots, and neither a passing retry nor a changed trace proves causality.

Output is flushed only after each measured loop, before its unchanged acceptance checks.
Logging/appender and observation overhead can affect later scheduling; interpret comparisons
accordingly. No automatic acceptance logic consumes these diagnostic records.

## Separate location/intensity and canonical-state refinement

`ORIGINAL_LIGHT_REFINED` adds independent per-pulse `skyPositionHash` and a fixed 16-bin
`skyIntensityHistogram`, ordered by the actual returned intensities 0–15. The position-only
hash uses the same ordered original `(J)I` read positions but **not their results**. The
histogram counts actual returns but **not their positions**. Together with the unchanged
original ordered position+result hash, these help distinguish changes in queried locations
from changes in the light returned at those locations. Histograms discard order; none of
these compact channels is a full positional light snapshot or collision-free equality proof.

A native-only `NativeStateCodes` subscriber builds its codebook at Forge `ServerStarting`.
Pinned `GameTestServer.initServer` calls `handleServerStarting` after level loading and
before tests are started later by `tickServer`. Declared scope `original-orange-blocks-v1`
selects **all `minecraft` blocks plus exactly** `dynamictrees:rooty_dirt`,
`dynamictrees:oak_branch`, and `dtfruitsdelight:orange_leaves`. It selects registered block
keys **before** calling `getStateDefinition().getPossibleStates()`, so unrelated palettes are
never enumerated. Only selected blocks' globally interned immutable possible states are
included, never world/position states. This scope is for the original orange fixture only;
it does **not** claim to cover every block/state that any world or another tree may write. It
stops after at most 131,072 admitted identities plus one excess detection. It retains only
bounded, globally interned immutable BlockStates as Minecraft objects; the JDK ledger still
retains **no Minecraft objects**. Registry/property access and SHA work occur only here,
not in measured callbacks. There is no lazy bootstrap, readiness wait, retry or barrier.
Missing startup remains unknown/incomplete rather than forcing bootstrap during measurement.

Canonical schema `original-state-v1` concatenates the fixed schema string, a length-prefixed
namespaced block key, the property count followed by `:`, then sorted property-name/value
scalars. Each scalar is decimal Java string length (UTF-16 units), `:`, and its string value.
The code is the first 64 bits, big-endian signed, of SHA-256 of that canonical UTF-8 string.
Zero is reserved for an actual null returned state and `Long.MIN_VALUE` for unknown. Reserved
hashes, conflicting codes/canonical strings or identities, an empty book, or exceeding the
cap make the **entire** book incomplete; its non-null lookups then return unknown. Unknown
identities never trigger registry/property queries or insertion. A frozen private identity-map
copy cannot be mutated by its builder, and lookup never calls state equality/property methods.

The startup-only `ORIGINAL_STATE_CODEBOOK` marker records schema, entry count, completeness,
declared scope version, sorted selected block keys, scope completeness,
collision/invalid and excess counters, and a SHA-256 digest of the schema plus the sorted,
length-prefixed distinct canonical strings. The digest is independent of registration order
and opaque object identities. Repeated equivalent canonical strings share a code. Digest and
counts are diagnostic domain provenance, not proof of measured world state. All three exact
native keys must be present for scope completeness; missing keys leave the whole lookup
incomplete. The original 131,072-state budget, collision/unknown rules and sentinel behavior
are unchanged. Any actually observed unregistered **or out-of-scope** write still produces
UNKNOWN and marks that measured session canonical-incomplete; it is never silently admitted.
A startup adapter
failure emits `complete=false` without retaining the exception/server/event object.

The existing chunk RETURN callback still records its original position/opaque identity
channel. It additionally performs only two frozen codebook identity lookups and sends primitive
requested/actually-returned-old-state codes to the ledger. New per-pulse `canonicalWriteCount`
and `canonicalWriteHash` hash the ordered position/requested-code/returned-code triples;
`ORIGINAL_CANONICAL_WRITE` keeps a separate 64-entry tail of those triples with pulse/within-pulse
index. No new lookup of the post-write world state occurs. Null old-state returns have code zero;
this channel remains an attempted-write RETURN trace, not proof of full realized geometry.

`canonicalUnknown` counts writes with an unknown component. Session-level
`canonicalIncomplete=true` is set if bootstrap was not complete before the scope or any
unknown was encountered. Never interpret matching unknown/sentinel codes as state equality.
Only matching canonical schema, declared scope version, selected keys/digest and complete
codebook/observations permit canonical cross-run comparison;
64-bit codes and rolling aggregate hashes still have finite collision risk. Incomplete canonical
evidence does not erase the independently captured light/original channels or change native
acceptance. Startup allocation and extra observations may affect later scheduling, so neither
this refinement nor any passing retry proves the historical cause of orange drift.

## Opt-in full ordered events (`-PoriginalEventTrace=true`)

Default is **false**. Only the source-native `gameTestServer` RunConfig receives
`better_wild_fruit_trees.originalEventTrace=true`; ordinary client/server runs do not.
The RunConfig guard rejects escape. With false, no ordered Scope/32-MiB arrays, directory,
class binding files or event-file I/O are created. Existing five aggregate channels remain.
The four original fixture observation lines and whole stripped-file SHA remain unchanged.

The SAME four mixin types now include a required `LevelChunk.setBlockState` **HEAD**
(require/expect/allow 1), retaining all four original RETURN callbacks. HEAD records request
position/canonical state **before** original mutation/nested light calls; paired RETURN
records the actual old-state result, including null. A later RETURN alone can never prove
light-before-write. HEAD is request intent, not an exact physical storage-mutation timestamp.
Existing exact-once leaf-query redirects emit BEGIN/RETURN around the original invocation;
exceptions are not wrapped/caught/replaced by those redirects. A bounded combined call-ID /
parent-ID stack pairs nested query/write activity. Raw SKY records carry that active context.

`OrderedEvents` retains at most 524,288 fixed eight-long owner records (32 MiB), a 256-entry
context stack, and a separate 4,096-slot primitive worker buffer with atomic reservation /
release-commit. There is no per-record object allocation, native query, random draw, registry
lookup, logger/file I/O, wait, monitor or readiness barrier added by the ordered ledger in
measured callbacks. Canonical HEAD lookup uses the already frozen identity codebook only.
Existing aggregate/framework bookkeeping is retained, not replaced. Publication callbacks
capture their originating aggregate-session token so a delayed old callback cannot be
attributed to a later sample. Detachment takes no worker barrier; uncommitted slots, observed
reservation races, overflow, dangling/unpaired contexts, UNKNOWN or incomplete aggregates /
bindings make the diagnostic incomplete, never change gameplay or acceptance. Unseen late
reservations and post-PUTFIELD callback gaps remain possible; worker sequence/version is
**not** an exact map-generation order. Identity hashes can collide.

After compilation and before native execution, `prepareOriginalEventBindings` writes, only
when opted in, CREATE_NEW class-hash manifest and binding properties into the actual admitted
UUID working directory. They bind nonce, profile/fixture choices, clean source commit,
SHA-256 of sorted main + nativeProbe class hashes, and the existing before-own-prepare native
input receipt. Startup adds exact dictionary schema/scope/digest/completeness. Missing/malformed
bindings are diagnostic-incomplete, not permission to bypass any native gate.

Only after scope detachment, binary files are written with CREATE_NEW beneath that directory's
`original-events/sample-N.wftord`. Existing files are never replaced; failures retain partial
files and emit a brief incomplete receipt. Observation/flush failures cannot replace a pending
measurement Throwable; original gameplay invocations are never inside this diagnostic catch.
Successful receipts contain path, whole-file SHA-256, record counts, incomplete status and
`scopeOutcome=UNPROVEN`. The file contains magic/version, bounded properties header, owner /
worker counts, full primitive records, worker commit markers, and trailing SHA-256 of the
entire preceding payload. Buffers, hashing and buffered binary I/O happen post-scope only.

Owner rows are `(kind,pulse,packedPos,result,requested,returned,callID,parentID)`; for SKY,
fields `requested/returned` instead hold opaque receiver identity and packed before/after
observed versions. Kinds are SKY=1, SEE_BEGIN=2, SEE_RETURN=3, BRIGHT_BEGIN=4,
BRIGHT_RETURN=5, ADEQUATE=6, WRITE_HEAD=7, WRITE_RETURN=8, STOP=9. Worker rows retain
observed pulse/identity/version/owner-cursor snapshot with a separate commit marker. Semantic
cross-run prefix comparison excludes SKY's opaque identity/version fields, not actual results.

**Balanced STOP does not prove measurement completion.** The unchanged finish call lives in
finally and has no access to an escaping Throwable. Thus every scope's outcome is UNPROVEN
until offline validation correlates **exactly one** `ORIGINAL_EVENTS` receipt for the exact
actual nonce-owned trace path, actual whole-file SHA-256, record counts, complete status and
UNPROVEN outcome, with exactly one matching unchanged post-assert
`GROWTH_SAMPLE orange seed=N pulses=P` **strictly later** in the supplied retained native log.
Its expected SHA-256 must also be explicitly provided. Marker-only logs, wrong path/payload
hash/counts, duplicate receipts/markers or reversed ordering are inconclusive, including
exceptions after otherwise balanced hooks. These checks establish consistency/correlation,
**not authenticity**: a caller can fabricate a mutually consistent log and its hash. Trusted
retention/provenance must be established separately; caller-supplied SHA alone never authenticates
a log. This completion evidence does not waive
persimmon/bayberry/other native failures or imply overall exact-26 acceptance.

The direct JDK comparator is `scripts/OriginalEventCompare.java`. Compile it alongside
`src/main/java/com/bettercontent/betterwildfruittrees/gametest/OrderedEvents.java` into a new
retained, task-owned output directory (no Minecraft/Gradle required), then run:

```text
java -cp <classes> com.bettercontent.betterwildfruittrees.gametest.OriginalEventCompare \
  <nonce1>/original-events/sample-N.wftord <retained-log1> <expected-log1-sha256> \
  <nonce2>/original-events/sample-N.wftord <retained-log2> <expected-log2-sha256>
```

It checks payload SHA/length/structure, paired nesting/order, dictionary/class/source/profile
bindings, actual nonce-directory receipts, exact payload receipts and strictly later post-assert
outcomes before comparing complete
semantic prefixes. Kind/position/request-first changes, an in-flight write, missing evidence,
overflow, or a light value change without matching adequacy/next-request change are
inconclusive. Same-position query-result divergence before any differing write HEAD, then a
corresponding adequacy change followed by different next write HEAD, is labelled **mechanism
support, NOT historical cause**. No extra RNG observer or protocol fix is introduced.

Observation allocation, atomic traffic and post-scope flushing **can perturb scheduling**.
Even complete ordered evidence does not prove stability or the historical cause of drift.
Every ordered class/resource/binding/payload is excluded from runtime archives, with negative
artifact controls; no activation, deployment, acceptance relaxation or cleanup is authorized.

Cause-first readiness design (proposal only; no barrier implementation):
[Light readiness and queue-ordering proposal](light-readiness-proposal.md).

## Controls and remaining acceptance

The native-only `verifyNativeLightProbe` dependency validates pinned archives, original
invocation descriptors/counts and SRG mappings, then checks real compiled native annotation
counts/refmaps. It also runs the pinned 0.8.5 Mixin AP on authored ABI stubs with exact
expected mappings. Ordinary Python tests run pure JDK authored-stub controls for forwarded
arguments, exactly-once calls, unchanged results, original exception identity, inactive
no-observation behavior, absence of loop I/O, scope cleanup, and overflow/seed bounds.
Refinement controls also pin the canonical SHA code, property/registration-order invariance,
frozen identity-only lookup, collision/reserved/cap/empty-book failure, missing bootstrap,
known/unknown hot-path query counters, location/intensity separation and canonical versus
opaque-write channels. Actual compiled subscriber metadata and pinned immutable-state APIs/
GameTest startup ordering are checked without running Minecraft. These controls do not replace
actual event delivery, startup marker completeness or required native Mixin application.

Only the lead runs Gradle/native verification under the canonical shared cache lock.
Compilation, native configuration loading/application and present/absent/stage-default
profiles remain pending for this diagnostic candidate. Do not infer deployment, activation,
packaging or an orange causal repair from these source/pure checks.
