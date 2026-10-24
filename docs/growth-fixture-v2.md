# Versioned native growth calibration

## Authority and measured claim

Owner: this repository's `GrowthRateGameTests`, `GrowthFixturePlan`, `GrowthFixtureController`,
`GrowthFixtureV2`, `growthFixtureV2` source set and `scripts/check_growth_v2.py`.
Production `WildFruitPolicy.ENERGY` and `TreeGrowthRates.REFERENCES` remain authoritative inputs.
The shared [testing procedure](../../../better-content-modpack/docs/testing.md) controls release
qualification, timing, resource ownership and retention. This document describes the candidate
fixture; source/JDK/SDK controls are not native or distribution acceptance.

Measurement is the number of explicit whole native growth pulses required for a fresh native tree
to attain its native maximum trunk radius in this favorable, isolated manual-pulse fixture. It is
not elapsed gameplay maturation time, a global lighting-determinism proof, natural soil/survival
acceptance, or a population confidence claim. Eight fixed growth seeds are `12345+104729*n`, world
seeds `54321+104729*n`, for `n=0..7`. The thirteen legacy species are the exact set shared by the
policy and legacy references: apple, bayberry, durian, fig, hawberry, kiwi, lychee, mango, mangosteen,
orange, peach, pear and persimmon. Optional cactus/coconut/ancient calibration remains separate.
Legacy reference provenance beyond the retained source description is **UNKNOWN**; no new
observations automatically change references or production growth rates.

## V1 compatibility and selection

During development **v1 remains default**. `-PgrowthFixtureVersion=v1|v2` affects only the native
GameTest RunConfig. Unknown versions fail before launch. V2 rejects all original diagnostic
selectors (including explicit false forms), queue-proof and optional calibration. Original
orange/kiwi ordered observations remain v1-only. V1's complete original body, five-tick setup,
seeds, native calls, endpoint, arithmetic and 2000-tick timeout remain pinned by the original
whole stripped-file SHA, after an exact separately pinned declarative registration migration and
identity frequency-input adapter. The old public test name remains `nativefreshgrowthcalibration`.
The generator creates exactly one required, nonflaky attempt with the original v1 template/batch;
v2 selects a distinct singleton batch and its own native bound. The other 25 identities remain.

V2 scope `full` requires thirteen by eight samples. `prototype` is explicitly diagnostic and
requires bayberry and kiwi by eight, never full growth/provider acceptance. Native-only control
values `none`, `zero-energy`, `double-cadence` reject unknown combinations and cannot qualify when
injected. Zero energy changes only the fixture's signal input; doubled cadence changes only the
frequency check input. Neither changes a production getter, reference or leaf suitability.

## Native scheduling, ownership and favorable setup

The dedicated controller executes at most one whole native pulse in each scheduled server-tick
callback, with one `growSignal` and the original `ageVolume` after every sixteenth pulse. It never
waits inside those calls, batches catch-up pulses, repeatedly reseeds growth, or uses a light
solver operation. Per-sample growth RNG is created once and world RNG seeded once at admission.
Normal native leaf aging/hydration and native internal growth reads remain; no v1 RNG/history
or deterministic internal-light equivalence is claimed.

The fixed root is `(48,160,48)`; the original inclusive clear/owned domain is `(4,160,4)` through
`(92,220,92)`, horizontal chunks 0..5 in each dimension. Custom fixture-owned tickets preload this
domain. Admission requires current native holders with completed **LEFT TICKING futures**, matching
world/chunk/current-cache/holder identity, plus native block/entity ticking predicates. Polling
uses scheduled callbacks and nonblocking `getNow`, never join, force-drain or global quiescence.
A fresh plot must contain no unowned nonair state or entities. Native growth/age geometry remains
unchanged; the declared domain is not opportunistically widened after a failure.

A singleton nonflaky batch excludes other WFT test bodies under the pinned engine's serial batch
runner, not native background activity. Batch names do not determine order. Setup occurs inside
the test, after actual structure preparation; all cleanup, receipts, resource release and global
state restoration precede `succeed`/`fail`, whose listeners may synchronously start another batch.
Scoped native daylight/weather rules hold favorable day 6000 and clear weather, then restore their
previous rules, day and weather timers. Random tick speed 3, growth multiplier .5 and folding 2
remain asserted. Setup has at most 200 native ticks, with local sunlight, actual native leaf
adequacy, fertility, fresh root/species/branch/leaves identity, energy/config and endpoint checks.
These local checks are **not a global light fence**. The world RNG is an intentional seeded and
subsequently consumed measurement input, not a restored v1 random history.

### Prevent uncounted autonomous root pulses

The native-only required `growthFixtureV2` Mixin set cancels `RootyBlock.randomTick` only while an
exact sample is admitted, in the same world, at the exact owned root, on its owning thread, with
its actual expected root-block identity. It is absent from v1, client/server RunConfigs and
runtime archives. This intentionally excludes that root's automatic growth/soil/fertility/survival
updates during manual measurement, not unrelated roots or native leaf ticks. The pinned native
route is randomTick → updateTree → Species.update → grow → TreePart.growSignal.

Explicit command/substance actors and falling-tree root decay can bypass randomTick. This is an
isolated no-player/no-command fixture, not a universal mod audit. Live actor/foreign-state checks
and an admitted falling-tree/actor join latch invalidate the measurement, including joins inside
a native grow/age operation; cleanup alone cannot turn their hidden growth into PASS. No scheduled
root tick or species-block-entity ticker exists in the pinned implementation. Native leaf
random/scheduled aging does not deliver a root pulse. Any unexpected entry route remains a
measurement defect, not permission to change references.

A deliberately invoked transformed owned-root callback must be cancelled before the first measured
pulse. After that sample's measurement ends and its gate is disarmed, a deliberately invoked
unrelated ordinary-root callback must forward and return exactly once; its native changes are
cleaned before the next sample. The receipt requires this separate cold delivery control, so a
stochastic count of zero automatic ticks cannot masquerade as hook proof. The gate is disarmed
first on cleanup/cancellation and restored even on failure; it adds no measured pulse.

## Outcomes and limits

Sample states are PASS, ASSERTION_FAILURE, INVALID_SETUP or INCOMPLETE. An endpoint/pulse-limit
assertion failure does not hide subsequent independent samples or species. Setup, ownership,
engine or cleanup contamination aborts unsafe continuation and emits explicit remaining INCOMPLETE
rows. There are no static v2 sums. Exactly eight valid endpoint samples are needed for a species
mean; partial successes or a 4096 failure placeholder never enter a passing mean. Exact relative
deviation uses integer eighth-pulse references and requires numerator*5 < denominator (strict20%).
Every mean and every individual outcome is retained. Original exceptions remain primary/suppressed
rather than becoming success; successful driver cleanup is not native success.

Per-sample setup is bounded at 200 native ticks and measurement at4096 pulses. The native test
budget447648 includes all104 setup/measurement and transition/cleanup callbacks. It is not an
empirical wallclock expectation. Shared durable timing uses explicit comparable successful raw
baselines for exact2×; absent such inputs expectation remains UNKNOWN with two-hour prototype and
eight-hour full-process safety caps. Setup, measurement and cleanup durations are distinct.

Only owned entity IDs, plot mutations/scheduled ticks and custom tickets are cleaned. Every owned
ticket release is independently attempted; failed leases remain dirty. Environment restoration is
attempted despite a sample cleanup failure. Unowned actors/state are not deleted. Worlds, outputs,
failed evidence and original v1 failures remain retained across handoffs under shared policy.

Cleanup uses a separately controlled synchronous port: disarm first; check foreign actors; clear
owned crown-to-root blocks and independently attempt both scheduled-tick stores; **then** drain
owned entity UUIDs, including joins caused by those clears or by another owned discard. Failed
leases remain dirty and are not retried in that invocation; new reentrant leases are attempted.
An abnormal cascade exceeding4096 distinct entity releases fails closed, never fabricates clean
state or waits for ticks. Entity draining also runs when partial cleanup already released plot
ownership. Foreign actors are never discarded, are rechecked before marking clean, and contamination
remains a failure. All tickets are attempted independently. A `CLEANUP` receipt binds before/after
owned IDs, ticket coordinates/counts and plot state; successful endpoint rows require a properly
ordered post-admission cleanup with every owned container empty. Dirty preparation/restoration
errors name the exact containers/IDs. Controlled tests reproduce the old drain-before-clear bug,
partial failures, reentrant disposal and preservation of foreign actors. They are not native proof.

### Bounded pulse and RNG observations

V2 records the **already evaluated** native getEnergy argument and GrowSignal return fields
(success/choked/steps), actual growth-kit class and biome identity. No extra native energy,
suitability or random operation is evaluated to obtain those metrics. A required native-fixture
LegacyRandomSource accessor reads its AtomicLong core state, never changes or consumes it. Its
admission witness checks the core state immediately after the existing once-per-sample seeds.
Before/after native growth (including energy evaluation), explicit ageVolume and consecutive
callbacks distinguish observed private-growth versus shared-world core-state changes. Core-state
inequality is evidence of intervening state change, **not** draw counts, a counterfactual cause,
complete RNG state (Gaussian cache is not included), light completion or world equivalence.
Metrics are bounded by4096 pulses and remain excluded from runtime archives. Instrumented
observations can perturb timing; they never qualify a failed mean or authorize recalibration.

## Evidence and finite admission

Pre-run clean-source bindings include both source heads, compiled-class and actual runtime-input
manifests, exact native attempt/profile, build and controller/reference hashes. A durable JSONL
receipt records header/constants, preparation/check/admission, sample terminal outcomes, exact
means, isolation delivery control and final native fixture result. The strict reader rejects
partial/reordered/duplicate results, wrong coverage/version/profile/seeds/constants, dirty bindings,
changed manifests, invalid setup and inconsistent means. Full acceptance additionally requires
all other25 native cases and fresh native timing health/log/provenance gates; receipt success alone
is insufficient. No fixture/controller/control/receipt bytes ship in the production JAR.

After focused controls and actual SDK/AP/refmap/archive/config gates, freeze clean inputs and
preregister exactly one prototype per dependency profile. Stop on the first setup/coverage/assertion
defect. Only if both pass, run exactly three fresh full worlds per profile, then the two absent
native designated-failure controls once each. No replacement trials, pooling away failures,
recalibration, arbitrary sleep, guessed fence or Debug. Promotion requires every gate separately;
then default/version/schema/provider fingerprints and docs change together and fresh canonical
qualification is mandatory. Otherwise retain a bounded actionable defect report and terminate that
native campaign without replacement trials. This is not project completion: continue a causally
corrected source/control cycle toward stable Dist, preserving the failed campaign; only validated,
clean changed inputs and a new explicit finite preregistration admit a later native campaign.
Unchanged retries, weakened assertions or quiet reference changes are never an allowed repair.
