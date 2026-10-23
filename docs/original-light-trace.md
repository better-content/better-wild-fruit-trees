# Original orange light trace (nonshipping diagnostic)

This is observation, **not** a growth/lighting protocol repair or evidence that historical
orange drift has been explained. It adds no GameTests and changes neither the exact-26
main gate nor the original root `(48,160,48)`, seeds, five-tick delay, clock, pulse limit,
age interval, endpoint, references or strict tolerance. A full-file SHA-256 control strips
only the four new observation statements/scope lines and requires byte-for-byte equality
with the witness-bearing original fixture at `8d4c441`.

## Isolation and actual observations

`nativeProbe` is a separate source set added only to the `gameTestServer` mod sources.
Its required mixin configuration is an explicit native-run argument, not a global Mixin
configuration or manifest entry. Client/server runs and the runtime JAR do not include
its classes/resources. The JDK-only ledger lives alongside the already excluded GameTest
classes. The artifact gate rejects both native trace resources and all GameTest classes.
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
are bounded noncryptographic summaries, not full-state/geometry equality proofs. Actual
states cannot be decoded from this trace without an additional, separately reviewed design.

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

## Controls and remaining acceptance

The native-only `verifyNativeLightProbe` dependency validates pinned archives, original
invocation descriptors/counts and SRG mappings, then checks real compiled native annotation
counts/refmaps. It also runs the pinned 0.8.5 Mixin AP on authored ABI stubs with exact
expected mappings. Ordinary Python tests run pure JDK authored-stub controls for forwarded
arguments, exactly-once calls, unchanged results, original exception identity, inactive
no-observation behavior, absence of loop I/O, scope cleanup, and overflow/seed bounds.
These controls do not launch Minecraft or replace actual required Mixin application.

Only the lead runs Gradle/native verification under the canonical shared cache lock.
Compilation, native configuration loading/application and present/absent/stage-default
profiles remain pending for this diagnostic candidate. Do not infer deployment, activation,
packaging or an orange causal repair from these source/pure checks.
