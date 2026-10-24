# Native cactus adult-architecture reference — calibration RETIRED

> Historical endpoint design, not an active calibration gate. Explicit user retirement prohibits
> further adult-size/manual-pulse reference campaigns. Native product/geometry functional tests
> and existing production references remain unchanged.

This subject owns the nonshipping growth-calibration endpoint for protected saguaro and
**proven same-root saguaro-origin mega** forms. `gametest/CactusArchitecture.java` defines
`native_upturned_four_arm_v1` before measurement. This is a structural reference, **not**
a unique native adult flag, ultimate maximum size, thickness milestone, fruit milestone,
plateau, timeout, runtime age state or universal growth guarantee.

## Pinned native authority

Inspect the declared DT Plus 1.2.2 dependency (`curse.maven:dynamic-trees-plus-478155:7502535`)
and DT 1.4.10, not mutable warmup JARs. Exact upstream implementation paths within those
artifacts, expressed as Java source paths corresponding to their classfiles:

- `com/ferreusveritas/dynamictreesplus/systems/growthlogic/SaguaroCactusLogic.java`,
  `populateDirectionProbabilityMap`: horizontal weights are 1 only when
  `signal.isInTrunk() && signal.energy > 1`; off-trunk straight-out continuation is
  removed; the up weight depends on delta-X/Z parity. This supports a primary stem
  with horizontal emergence followed by a native upright arm, not arbitrary thickening.
- `.../systems/growthlogic/MegaCactusLogic.java`, `populateDirectionProbabilityMap`:
  default `stop_branching_height=5`; at relative height **greater than** that property,
  the direction map is `{0,1,0,0,0,0}` (upright-only tier). Default `max_height=7` is an
  80% stop, **not** a strict maximum. It is not used as this reference.
- `.../systems/growthlogic/StraightLogic.java`, `getEnergy`: native signal energy is
  biome-scaled with coordinate/month height variation. Our existing protected-energy
  policy remains unchanged. No pulse count or maximum geometry is deduced from that rate.
- `.../block/CactusBranchBlock.java`, `growIntoAir`: successful child placement stores
  `ORIGIN=signal.dir.getOpposite()`; horizontal placement rejects an occupied cactus
  immediately above/below; native upward growth therefore records `ORIGIN=DOWN`.
- DT `com/ferreusveritas/dynamictrees/systems/GrowSignal.java`, `step`: every native step
  decrements energy by one and ends at nonpositive energy. Individual routing has a finite
  energy envelope; stochastic repeated routing is **not** a finite guaranteed completion time.
- DT Plus `trees/dynamictreesplus/species/{saguaro_cactus,mega_cactus}.json` selects these
  logic kits. The fixture reads the actual typed mega `stop_branching_height` property,
  rather than changing its value or treating the soft max as a cap.

These facts were checked against pinned mapped bytecode with `javap -c -p` and bundled
species JSON. The source inspection is a reproducible API/design basis, not native acceptance.

## Versioned predicate

Coordinates are relative to the native root. A central stem must be contiguous at `(0,y,0)`,
starting at `y=1`, every node with native `ORIGIN=DOWN`. For each of E/W/N/S, at **some**
connected stem height there must be an adjacent elbow whose ORIGIN points to that stem and
an immediately higher arm node with ORIGIN=DOWN. The four elbows need not share a height.

```text
one cardinal section (required independently E, W, N, S):
    |       u       u: upward arm, ORIGIN=DOWN
    s---e           e: elbow, ORIGIN points to s
    |               s: contiguous native-origin central stem
   root
```

Mega additionally requires exact existing persisted conversion provenance through RootLedger
and a contiguous central stem above the native `stop_branching_height` (entry to the upper
upright-only tier). An unmarked historical mega is never accepted by shape alone. Fresh,
trunk-only, straight-arm, disconnected, wrong-origin, below-upper-tier and unproven-mega
negative controls establish the distinctions. Cacti can keep developing afterward.

The fixture traverses already loaded connected branch blocks only, bounded at 4096 visited
positions / 128 Manhattan distance; crossing a bound fails, never declares adulthood. The
pure predicate consumes scalar origin topology. It does not alter native RNG, growth, energy,
geometry, fertility, production state or player policy.

## Pinned optional native registry preparation defect

The first optional diagnostic also emitted two real DT registry errors before the coconut
failure: `dtnatures_spirit:redwood` and `frosty_redwood` resources requested growth logic kit
`dtnatures_spirit:redwood`, but DTNS 1.3 `systems/ModGrowthLogicKits` constructs its public
`systems/growthlogic/RedwoodLogic` under `dtnatures_spirit:hemlock` only and registers no
`redwood` ID. Evidence: `build/logging-calibration-20261020T214820Z.log:505–506`, and the
pinned JAR's exact class/resource entries. These errors are not waived or a passing run.

Lead-authorized `compat.OptionalGrowthKitRepair` is a **production optional integration**,
not a fixture-only alias. At the supported DT GrowthLogicKit registry event (LOWEST), it
registers the actual public native `RedwoodLogic` only when DTNS is present and `redwood` is
missing. It never replaces a present ID, the existing hemlock binding, any species resource,
configuration, energy, taper, growth rate or selector. It restores the native logic explicitly
requested by ordinary redwood/frosty-redwood species, so those ordinary trees now resolve their
intended kit instead of the prior broken lookup. Optional linkage lives in a nested class reached
only after presence/ID admission; the absent profile must class-load without that addon.

The existing optional-stock native test now checks real restored class/presence, unchanged
hemlock binding, absent factory noninvocation and alias-already-present identity/no override in
a separate scratch native registry. The main required count stays 26. These added controls and
optional-present/absent full gates remain **unrun**, blocked by calibration and admission; no
native acceptance is implied by source inspection.

## Coconut woody-reference blocker (retained historical failure)

The same optional diagnostic exposed a separate **coconut** requirement mismatch, retained
rather than waived: a fresh source-pinned plot reached 21 branch nodes / height 21 / trunk
radius 6 after the 4096-pulse failure deadline, short of its family cap 8. That observation
alone does not prove impossibility. The relevant causal source facts are:

- DT Nature's Spirit 1.3, `xueluoanping/dtnatures_spirit/systems/growthlogic/CrookedPalmLogic.java`,
  `populateDirectionProbabilityMap`, zeroes every direction and chooses exactly one nonzero
  direction: a root/coordinate-determined horizontal bend at consumed-energy step 4, possibly
  one continuation, then UP. Despite declaring `chance_to_split`, this implementation never
  adds a simultaneous second route. A fixed root/month/energy regime produces one bent path,
  not woody side-branch area that can accumulate a thick trunk.
- DT `growthlogic/PalmGrowthLogic.java`, `getEnergy`, uses base energy × biome suitability plus
  `coordHash % 3`. `Species.biomeSuitability` normally clamps its final environmental result
  to [0,1], or returns exactly 1 for perfect biomes. **Handled BiomeSuitabilityEvent bypasses
  that clamp**, so an arbitrary provider can exceed 1; the bound is not universal. The fixed
  diagnostic now asserts the actual post-hook suitability is finite, positive and ≤1, actual
  energy ≤27 and native secondary thickness 2 before relying on this proof. Those new native
  preconditions have not yet been run. Under that verified regime, unchanged protected energy
  25 gives at most 27. Each native GrowSignal step spends one.
- DT `block/branch/BasicBranchBlock.java`, `growSignal`, returns
  `sqrt(childRadius² + otherBranchRadii²) + species.getTapering()`, clamped against existing
  radius and cap; native block radius uses **floor**, not ceil. On the single path there are
  no off-path branch terms. `DynamicLeavesBlock.branchOut` uses native secondary thickness;
  the family's normal secondary thickness is 2. Leaves aging sustains foliage and does not
  insert another CrookedPalm route. The protected growth-only taper stays `.55 / 2.5 = .22`.
- Thus a conservative fixed-regime radius envelope is `2 + 27 × .22 = 7.94`, floored at most
  7, below family cap 8. The fixed reference plot's smaller biome-adjusted energy explains
  radius 6; this is not a recommendation to change energy, taper, biome policy or assertion.
  Native monthly coordinate-energy changes can alter a future route; this argument is scoped
  to the controlled fresh reference, not a universal all-calendar world-size theorem.
- Fresh fixture construction uses native family primary-thickness branch, native leaves/root
  and species entity where required, followed by actual growSignal/leaf aging. Native
  `BasicBranchBlock.growIntoAir` / `DynamicLeavesBlock.branchOut` remain the geometry authority.
  The failure must not be repaired by worldgen inflation, arbitrary branch insertion or a
  radius/height plateau used as maturity.

A species-native palm structural reference required a **new requirement decision**, not silent
reinterpretation of this failed woody max-radius contract. Subsequent explicit review approved
[native Coconut fruiting-crown structure](coconut-architecture.md), not full-cap/full-canopy or
actual-fruit maturity. No measured reference or historical first-hit is inferred from this failure. Further native inspection must validate the regime,
initialization and leaf/radius facts when resolving that decision.

## Measurement and acceptance

Eight fixed seeded fresh native samples per form record the **first** pulse matching this
predeclared predicate. Ancient retains native maximum trunk radius; Coconut uses only the
specifically reviewed [native fruiting-crown predicate](coconut-architecture.md).
The 4096-pulse diagnostic bound is a failure deadline, never a substitute endpoint. Failure to
reach the predicate preserves observations for diagnosis; it does not justify reducing the
predicate or filling a guessed reference. Only reviewed actual measurements can fill the four
missing source references. Main acceptance repeats the same endpoints under its existing
26-test native matrix, with the separate documented absent-addon profile. Diagnostic success
alone is not full source, pack generation, elapsed gameplay or client acceptance.

### Independent finite diagnostic profiles

The default `-PoptionalGrowthCalibration runGameTestServer` still requires one native diagnostic
case, 2000 ticks, and all 32 samples. SOURCE also implements two explicitly allowlisted diagnostic
selectors; they require their own future Native admission, not merely the property:

- `-PoptionalGrowthCalibration -PoptionalGrowthCalibrationForms=ancient runGameTestServer`:
  eight fresh Ancient samples at the unchanged native maximum trunk radius.
- `-PoptionalGrowthCalibration -PoptionalGrowthCalibrationForms=saguaro,saguaro_mega runGameTestServer`:
  eight Saguaro plus eight proven same-root Mega samples, with separate actual means and the
  unchanged versioned topology/provenance/upper-tier predicates.

`CalibrationPlan` retains the canonical Saguaro/Coconut/Ancient/Mega ordinals 0/1/2/3 and sample
indices 0–7. Subsets never allocate seeds from their selected cursor: the native growth seed stays
`12345 + 104729L * sample`, level seed `54321 + 104729L * sample`, independently for every form.
The native root/soil, fixed season, initial geometry, conversion, growth and leaf-aging calls remain
the same. The main reference contract explicitly selects **all** forms regardless of diagnostics.
Invalid, empty, duplicated, unknown or reordered selectors fail; selection without calibration or
with absent wild addons is rejected. Selector membership and ordinals stay unchanged; the all
profile's Coconut endpoint is now the separately reviewed native fruiting-crown predicate, never
an inferred historical reference.

The diagnostic checks all original native registry prerequisites and the existing missing-only
Redwood controls before growth. Its finalizer requires both nonempty latest and DEBUG logs,
exact namespace/profile header, exactly one completed case, every initial/sample record in
canonical order, independent means equal to each form's own eight pulses, and no actual failed
native operation at any severity. Missing, duplicate, extra, mislabelled, changed-endpoint or
zero-case evidence fails. Synthetic oracle controls and pure JDK seed-plan checks are not native
measurements. Manual causal review of both actual logs is still required. This facility supplies
no references, does not bypass staging's four-reference guard, and does not reduce either required
26-test present/absent acceptance profile.
