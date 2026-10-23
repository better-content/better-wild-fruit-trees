# Better Wild Fruit Trees

A standalone Forge 1.20.1 integration that makes Fruits Delight's dynamic fruit trees
large, bountiful, fixed-location food resources rather than portable orchard stock.
It also protects wild berry bushes and generates broad, irregular berry fields.

## Scope and authority

This repository owns the standalone mod's behavior, tests, and authoring inputs.
Contributor instructions are in [AGENTS.md](AGENTS.md). The workspace authority is
`better-content-modpack/docs/README.md`. Better Content workspace conventions are
maintained in the [shared policy index](https://github.com/better-content/better-content-modpack/blob/main/docs/README.md);
that development-policy link does not introduce a runtime modpack dependency.

## Optional food-tree extension — source-only, not ready

The working source extends the complete fixed-location resource policy to exactly
`dynamictreesplus:saguaro_cactus`, `dtnatures_spirit:coconut` and `dtquark:ancient`.
**The extension remains source-only, not ready for activation: four measured V9 references are
separately Source-admitted, but full native verification and deployment remain pending.
No matching validated runtime JAR has been installed.** The established thirteen-species behavior described below remains
the compatibility contract, not evidence that these additions have passed it.

| Resource | Native ripe product | Full-size energy | Native adapter |
| --- | --- | ---: | --- |
| Saguaro | `dynamictreesplus:saguaro_cactus_seed` | 10 | Cactus fruit |
| Coconut | `natures_spirit:coconut` | 25 | Attached/falling pod |
| Ancient | `quark:ancient_fruit` | 60 | Dynamic fruit |
| Proven saguaro-origin mega cactus | No invented fruit production | 45 | Persisted conversion origin |

The draft applies eightfold mature product, success-only 2,500-active-tick replacement credit
with a two-replacement bank, minimum hardness 80/native ×16, functional diamond-or-better axes,
threefold native soil longevity and the additional 1-in-256 native-selector gate. Woody growth
uses the established growth-only taper divisor 2.5; cacti keep native discrete thickness.
Coconut's root allowance replaces its native extra 1/16 production throttle while preserving the
random draw and native seasonal/size/geometry checks. Cactus hardness has a separate base hook:
its branch does not inherit the established hardwood hook on `BasicBranchBlock`.
Rejected saguaro selects native non-fruiting pillar:pipe cacti in a 6:1 ratio; other rejections
remain unhandled for native ordinary-tree fallback. Ordinary selections spend no extra RNG.
No global density, biome preference, JoCode, generated-size/fertility, scan/backfill or culling
change is intended. Cocoa, archwood pods, Aether trees, ordinary cacti and unmarked historical
mega cacti remain outside this policy.

Food/product identity is separate from propagation stock. Saguaro's edible seed and ancient
fruit retain native food behavior. Whole coconuts remain placeable native food/processing blocks:
DT must not destroy them as rejected saplings, but native coconut germination and
`natures_spirit:coconut_sprout` propagation are closed. Dedicated seeds/saplings/leaves, native
sprouts, recipe/trade/loot stock, staff and transformation routes are covered by the draft;
food, native cooking and unrelated stock are not blanket-filtered.

These three roots use a dimension `SavedData` ledger, schema 1, rather than requiring a
species block entity. Credit is persisted; runtime clocks are not. Native saguaro→mega conversion
records exact origin at the same root, inherits protection/growth/soil identity and synchronizes
loaded chunk provenance to clients. Unmarked mega roots are never guessed to be saguaro.
Root removal/lazy loaded validation clears stale records; chunk suspension prevents offline
catch-up. Unknown ledger schemas retain their bytes and reject use rather than becoming empty
ledgers. The old thirteen species retain their species-root block-entity storage.

**Measured-reference Source admission:** retained V9 diagnostic receipt
`5c099226-736b-4bf2-8bcd-3c70f889673d` supplies eight native samples per form (32 total).
After complete retained review and separate explicit lead admission, `TreeGrowthRates` records
`saguaro` (native rate `1F`, mean 26.75 pulses), `coconut` (`.8F`, 14.0), `ancient` (`.5F`,
89.125), and `saguaro_mega` (`.6F`, 67.25). This removes the missing-four Source-data blocker,
not the remaining release blockers. Required-reference guards and verification/staging contracts
remain intact; no tests or staging were run for this edit. Native rates alone are not calibration.
Saguaro/mega use fixed discrete thickness, so the ordinary full-trunk-radius endpoint is invalid. The predeclared versioned
[adult-architecture predicate](docs/cactus-architecture.md) uses native-origin connected
four-cardinal upturned arms, plus proven conversion and the native upper tier for mega.
The V9 cactus endpoint is `native_upturned_four_arm_v1`; coconut uses
`native_coconut_fruiting_crown_v1`, and ancient uses native maximum trunk radius. These measured
endpoints do not establish full-gate or production acceptance. No arbitrary pulse count, last observed
change or bounded-probe limit is accepted as maturity.

Seven additional main-namespace GameTests are authored (26 total including the two lemon tests),
not run: stock/player mining, food/ripe/falling harvest, native conversion/provenance/reload,
weighted/direct rarity and 6:1 fallback, saved success-only allowance, measured-reference native
pulse cadence plus eight-seed coconut/ancient growth, and native cactus/palm production hooks.
They also distinguish fixture-present from fixture-absent runs. Cactus endpoint acceptance still
needs authoring once the endpoint is resolved; real client provenance/relog/dimension behavior
and representative native generation remain runtime acceptance work, not inferred from serialization.

The separate `better_wild_fruit_calibration` diagnostic namespace requires all pinned addons.
`-PoptionalGrowthCalibration runGameTestServer` authors eight seeded bounded probes per form;
Ancient reports full-trunk pulses; Coconut reports only the reviewed
[native reproductive-eligible leaf-crowned structure](docs/coconut-architecture.md), not full-cap
wood/full canopy/actual fruit. Cactus probes report first attainment of the
predeclared adult-architecture predicate. A bounded-probe failure never supplies a reference. Its single
successful V9 diagnostic is **not** the 26-test production gate and does not automatically fill
source references; the four entries above have separate explicit measured-reference Source admission.
Main fixture-present 26, fixture-absent 26, full gate, shipping exclusions/clean45, staging and pack
Debug acceptance remain pending; none is supplied by this Source edit.
Future execution requires separate explicit authorization, registered retained fixture/evidence
paths and the whole-lifetime workspace POSIX Forge cache lock. Do not run `clean`, staging,
pack deployment, Packwiz refresh or packaging from this source-work handoff.

## Bounded playtest observations

`bc.playtest.better_wild_fruit_trees.*` observes **one existing outer production callback**
with primitive reserved/placed/refunded/unsettled totals and credit before/after. Native
product-observed debit versus absent-product refund is counted at the existing placement site,
never emitted per fruit cell. Callback normal return and aborted exception are distinct; native
exceptions propagate unchanged. Unloaded-root, insufficient-credit and optional-ledger deferrals
are coalesced primitive totals emitted after600 native END-server-ticks or at server stop, not
per random tick. There is no world reference, per-root/position diagnostic cache or new ledger.
It adds no growth pulse, placement, RNG draw, chunk load or persistent diagnostic state.
Credit spend is not a claim of ripe harvest or disk persistence. After the enabled guard,
the outer batch snapshots immutable dimension, game tick and existing native budget-root
coordinates. That root is the causal account anchor, not a player's location or identity;
no per-fruit-cell position, new tree ID/schema, root cache or native-state probe is added.
Success and aborted callbacks preserve the same original root context, including distinct
worlds at identical coordinates. Global deferral windows intentionally span roots/worlds and
carry no single-root context. Credit/storage-family fields avoid seeds, root-ledger NBT and
player identities. Startup reports actual version and bounded exact
own-artifact source metadata, or UNKNOWN when unavailable. All categories share120 detail/min,
burst20, max256 aggregation keys and max2048-character single-line records. Native server END
tick coalesces monotonic30-second summaries; server stop clears keys without refilling the
allowance or rearming TRACE. Ordinary standalone stays INFO; DEBUG requires an enabled logger
profile. This boundary emits causal DEBUG rather than per-growth-tick detail. Common focused
TRACE support requires a dimension plus exactly one UUID/position and expires once/process at
60seconds/1000records, still subject to the shared rate limit. Source log contracts and the
pure helper tests do not supply growth references or replace native/pack acceptance.

## Requirements

Install this mod on both client and server, alongside:

- Dynamic Trees **1.20.1-1.4.10**
- Dynamic Trees – Fruits Delight **1.20.1-1.0.0**
- Fruits Delight **1.1.3**, with its required Farmer's Delight dependency

The integration uses pinned native APIs and required Mixins. Other upstream versions
are rejected rather than silently running a partial integration. No KubeJS, modpack,
Bumblezone, or other Better Content mod is required. Java 17 is required.

## Behavior

### Larger growth potential

| Fruit tree | Full-size growth-signal energy |
| --- | ---: |
| Mangosteen, hawberry | 20 |
| Apple, orange, pear, lychee | 25 |
| Persimmon, peach | 30 |
| Bayberry, fig | 35 |
| Kiwi | 37.5 |
| Durian, mango | 40 |

These are growth budgets, **not heights in blocks or guaranteed mature dimensions**.
Subsequent branch growth uses 1/2.5 of the protected species' original tapering;
the species tapering used by worldgen branch inflation stays unchanged. Protected
species use three times their native biome-adjusted soil longevity to support the longer growth paths. Native family trunk
thickness caps, space, light, soil fertility, seasonal behavior, and growth logic still
apply. Growth frequency is normalized below; full-size energy/tapering do not depend on age.

Existing JoCodes and global worldgen density are not replaced. Protected fruit-species
selection is deliberately made very rare, with ordinary-tree fallback as described below.
Trees retain their authored generation layouts and can grow toward the increased potential afterward.
There is no giant-tree worldgen rewrite, forced backfill, or chunk-load growth scan.

### Thirty-day expected growth rate

Protected trees **keep their current native generated size**. Ordinary subsequent wood growth
uses a **constant per-species rate**, normalized so a fresh sapling-sized native tree would
reach the adult-size reference in roughly **30 extended Minecraft days** under favorable
conditions: **1,500,000 active game ticks**, about **20h50m at 20 TPS**. Normal player planting
remains blocked; the fresh-tree model is a calibration reference, not new portable orchard stock.

- **No age ledger, maturity timer, changing energy/tapering, size unlock or day-thirty event.**
  Full-size energy and growth-only tapering apply from the first pulse. Native JoCode/InflatorNode
  tapering remains unchanged. Existing and fresh trees use the same rate; old age records are ignored.
- The legacy thirteen `TreeGrowthRates` references use eight seeded native fresh-growth samples
  per species and GameTest world seed `8675309`, with first attainment of native **maximum trunk
  radius**. The four separately Source-admitted V9 references use their retained eight-sample native
  seed schedule and versioned cactus/coconut endpoints described above (ancient: maximum trunk radius).
  Pulse counts remain sampled calibration, not a deterministic per-tree guarantee or guaranteed final
  canopy volume/height. Branches can continue filling out naturally afterward. These controlled
  favorable-condition samples do not establish a universal physical maximum or full-pack growth time.
- For measured mean pulse count `P`, the baseline growth rate is
  `P × 4096 / (1,500,000 × 3 × 0.5)`. Vanilla selects 3 of 4096 section positions per tick;
  DT's root-update folding and per-update pulse folding cancel in expectation. The reference
  assumes randomTickSpeed **3** and the pack's global growthMultiplier **0.5**. The mod does not
  change those settings; doubling either speed halves the nominal time. Standalone DT defaults
  or other packs can therefore differ from this thirty-day reference.
- The native growth-rate result is multiplied, not replaced. Native dormancy, seasons and explicit
  boosts remain proportional. Bonemeal/extra growth calls can accelerate growth normally; sleeping
  does not directly grow wood. Inactive/unloaded chunks have no natural growth pulses or catch-up.
- Fertility, light, clearance, biome and native growth logic still constrain actual development.
  **Generated-root fertility is not changed or replenished.** No root is scanned, resized, reset
  or given an automatic fertility supply. This is growth-frequency tuning, not a guaranteed deadline.
- Fruit renewal remains independent: native `postGrow` still runs once per root update even if
  no wood pulse fires. The 160-fruit target, field renewal, rarity and ordinary-tree rates are unchanged.

### Bountiful fruit

- Mature dynamic fruit yields **8 times its native product count**.
- Natural fruit replenishment keeps **at least 12 bounded placement attempts** for native
  canopy geometry, but successful replacements now spend a per-root production allowance
  described below. Attempts are not guaranteed fruit. Initial fruit placement per accepted worldgen tree is unchanged.
- Fruit picking remains the normal native interaction. Immature fruit is not rewarded.
- Durian falling harvests use the same amplified native loot path, exactly once.
- Drop stacks are copied and split at the native stack limit.

Native ripening, seasonal production, and bonemeal rules remain in force. Twelve attempts
is not a promise of twelve successful placements or a fixed recovery timer. This is an
initial gameplay tuning, not a measured fruit-per-hour guarantee.

### No propagation

For these thirteen tree species the mod blocks native seed/sapling planting, automatic
seed planting (including force-plant/code NBT), the seed's machine planting interface,
flower-pot planting, native sapling conversion recipes, and Dynamic Trees staff copying
or recreation. Native transformation substances cannot convert trees into or out of the
protected species. Pre-existing dynamic fruit saplings cannot transition into trees.

Global loot filtering removes their seeds, native saplings, and movable fruit leaves.
Explicit JSON recipes producing that stock are removed on recipe reload. Native villager
offers selling it are removed on interaction. Player/entity placement and native sapling
growth events also reject protected planting stock. Existing inventory items are **not
silently deleted**; their planting routes become unusable.

This covers the pinned integrations and Forge placement paths, not arbitrary third-party
world-editing code that directly writes blocks or clones structures. Commands and world
generation are not disabled. A pack that replaces Forge's global loot modifier list must
retain `better_wild_fruit_trees:no_propagules` when integrating this mod.

### Resistant wood

Connected protected branches, including twigs and stripped branches, have hardness
`max(80, native hardness × 16)`. A bounded loaded-only root lookup identifies the actual
species instead of applying the policy to its shared oak/birch/spruce/jungle wood family.
No chunk is requested merely to classify mining resistance. Lookup-budget exhaustion or
an unavailable root leaves native hardness unchanged.

Normal player breaking requires an axe supporting Forge's `AXE_DIG` action with a
registered diamond-or-higher tier (Forge tier ordering, not item IDs). A server break-event
restriction prevents removal; client/server mining progress is zero with unsuitable tools.
The same rule covers the berry bushes below and Fruits Delight's `fruitsdelight:lemon_tree`,
with minimum hardness 80. Lemon is a **mining-only** inclusion at every age and on both
halves: native planting/seeds, growth, bonemeal, geometry, worldgen, loot and right-click
harvesting remain unchanged. It is not part of the managed berry-field inventory.
Picking needs no axe.
Fire, explosions, non-player root destruction, and native third-party machine removal remain unchanged;
Forge `FakePlayer` machine miners are explicitly exempt from the breaking/progress gate,
but **not** from planting restrictions. High hardness still applies to them. Machines
masquerading as real players are subject to the normal player rule;
this is **direct** player-breaking protection, not indestructibility. Removing supporting
soil, hoe conversion, fluid changes and other indirect native terrain updates can still
uproot bushes; supporting terrain is not assigned an axe requirement.

Optional Tinkers’ Construct **3.11.2.166** / Mantle **1.11.104** axes use their actual
per-stack `ToolStats.HARVEST_TIER` through the public typed API, not the dummy tier inherited
from `TieredItem`. Material changes and native modifier upgrades therefore affect permission.
The adapter copies the tool view, does not initialize blank tools, rejects broken tools, and
still requires the tool’s dynamic `AXE_DIG` action. TConstruct and Mantle are not required
or bundled. Other unregistered/custom tools without a supported Forge tier identity remain
conservatively rejected.

## Wild berry fields

The explicit bush inventory is `minecraft:sweet_berry_bush`,
`fruitsdelight:blueberry_bush`, `fruitsdelight:cranberry_bush`, and optional
`ars_nouveau:sourceberry_bush`. Sourceberry is optional; no Ars classes are referenced.
Glow berries (cave vines), Twilight Forest torchberries (hanging plants), tree
bayberry/hawberry, pineapple, melons and other non-bush crops are excluded.

All fruit ages use mature physical outline geometry and full-grown foliage models.
Age remains native fruit readiness: picking, fruit reset and bonemeal regrowth continue
without shrinking the plant. Natural random growth is deliberately slowed and normalized
per species as described below. Native per-plant picking yields are unchanged, not multiplied by eight. Abundance comes from **30–42 plants per field**, not
increased fruit per plant; actual harvest totals still vary by species and native yield.

Configured-feature overrides replace only the four bush patch definitions. Existing biome
selection and outer patch frequency remain native, followed by an additional 1-in-128 field
gate. Each accepted field targets 30–42 successful plants
with irregular triangular offsets over a **47×47-block footprint** and sparse spacing; poor
terrain can yield fewer. The target count is tripled from 10–14 and the footprint area is
about 2.6 times the former 29×29 area. Placement gets 288 bounded attempts. Fields are centered
on their decoration chunk so writes stay within its 3×3 chunk neighborhood; spacing checks
skip unavailable neighboring chunks rather than requesting far chunks. There is no chunk
scanning or existing-world backfill. Optional Ars overrides store a string registry ID, safely
doing nothing when the mod is absent.

BlockItem placement (including direct machine/fake-player item use) and Forge entity
placement reject bush creation. Berry-to-flower-pot use is blocked. Edible berries and
recipes remain intact; bush harvest food is not filtered from loot. Existing planted
bushes become protected resources in place. Commands, structure cloning, world-edit code,
and machines that directly write blocks without Forge placement events are outside this
propagation guard; native destruction by machines/fire/explosions is intentionally preserved.

## Very rare new-world resources

The landmarks are large and productive, but new discoveries are deliberately scarce:

- **Fields:** each otherwise eligible native bush-feature attempt passes an additional
  **1-in-128** roll. A native 1-in-32 placed feature becomes **1 attempt per 4,096 eligible
  chunks per berry type** (about one per 1.05 square kilometres of fully eligible terrain).
  Snowy-taiga sweet berries go from 1-in-384 to **1-in-49,152**. Biome preferences stay native;
  overlapping types still add opportunities. Ordinary taiga's sweet/blue/sourceberry rules
  total roughly **2.9 attempts per square kilometre**, before terrain and placement failures.
- **Trees:** a protected species selected by the native weighted selector passes an additional
  **1-in-256** roll. Failure returns native **unhandled**, so the addon's `SPLICE_BEFORE` chain
  continues into its ordinary biome selector instead of requesting a handled null tree.
  All thirteen protected species' pinned upstream rules are covered by the native fixture.
  A native 1-in-41 species rule becomes **1-in-10,496**; the base temperate forest's four
  overlapping fruit rules total approximately **0.026%**, or one in roughly **3,900 tree
  selections**. This is not an observed successful-tree density per square kilometre.
- Native biome eligibility, ordinary species selection, tree density/chance settings and JoCodes
  remain in charge. No fruit species is introduced to a new biome. The rolls do not enforce a
  minimum distance: rare nearby clusters remain possible, and unsuccessful terrain lowers output.
- Rarity applies to **new generation only**. Existing trees/fields are retained, not culled or
  relocated. There is no existing-world scan/backfill. Commands, structure cloning, direct block
  writes and third-party generators bypassing these native feature/selector paths are not covered.

## Renewable production cadence

Size and productivity are separate. Large trees and 30–42-bush fields stay large; natural
renewal is paced around a **50,000-game-tick reference day** (41 minutes 40 seconds at 20 TPS,
matching the pack's extended daylight plus normal night). This standalone mod does not change
the day-length mod, world clock or randomTickSpeed. Sleeping/skipping calendar time earns nothing.

- **Fields:** a mean 36-plant field targets **40 berries per reference day** at randomTickSpeed 3,
  sufficient light and prompt ripe harvesting. At 30–42 plants the expected range is about
  **33.3–46.7**, not an exact daily quota. Sweet/sourceberry ripe picks average 2.5 items;
  blueberry/cranberry picks average 1.5. Both need two native growth stages after ripe picking,
  so their per-random-tick growth probabilities differ (about 2.43% and 4.05%, respectively).
  Existing Forge growth vetoes and explicit ALLOW boosts remain authoritative; ordinary crops
  and non-target plants are unaffected. Bonemeal is not throttled by this natural-growth policy.
- **Trees:** protected fruit roots earn one replacement fruit block per **2,500 game ticks**,
  corresponding to **160 fruit per reference day** at eight items per ripe block. Only a successful
  native placement spends credit: blocked/occupied/non-exposed foliage does not count as capacity.
  The native geometry, fruiting-size checks, seasonal eligibility and ripening remain in charge;
  unproductive trees may fall below this target. The allowance is independent of leaf count.
- Native species-root block entity data persists fractional credit. The pinned native entity skips
  Forge's superclass save/load, so narrow hooks explicitly serialize only this mod's fruit-credit
  record; unrelated Forge data/capabilities and native species are not rewritten. Credit starts empty and banks at
  most **two replacements (16 eventual items)**, not whole days of unattended output. Data/level
  reattachment resets the accrual timestamp, so unload/restart time does not mint catch-up credit.
  Naturally infrequent updates and a full bank can lose potential production; this is a target
  rate for a productive tree, not guaranteed harvest or unlimited compensation for missed ticks.
- Existing ripe fruit, initial stock per accepted worldgen resource and native per-pick yield are unchanged. A first
  discovery/full harvest can exceed these renewable daily targets. Season, loading, harvest
  frequency and explicit growth boosts affect actual output; source fixtures are not full-pack
  seasonal or gameplay-performance acceptance.

## Explicitly unchanged

- Ordinary oak, birch, spruce, jungle, and other non-target species' growth, harvest and protection behavior.
- Pineapple, melons, non-bush vines/crops and other non-target plants.
- Global Dynamic Trees growth, worldgen density, and authored JoCodes.
- Ordinary oak planting stock and the independent Bumblezone cultivar system.

Apple protection targets `dynamictrees:apple_oak`, not ordinary `dynamictrees:oak` or
`minecraft:oak_sapling`. Fruit foods remain foods, not a blanket blocked item namespace.

## Build and verification

```sh
./gradlew --no-daemon clean verifyFull stageRuntimeJar
```

These are documented gates, not authorization to execute them for the incomplete extension.
After authorization, first compile sources, run the isolated optional diagnostic, review/measure
the cactus endpoint and all four references, and implement its missing endpoint acceptance.
Only then can the full gates pass. No gate was run for this extension.

The wrapper downloads pinned Forge/mod dependencies. `verifyFast` runs Minecraft-free
contracts; `verifyFull` also runs isolated headless Forge GameTests, a native diagnostic
gate, and production archive/refmap checks. The main native gate requires exactly 26 tests:
the established nineteen (including lemon) plus seven optional-tree contracts. Tests cover the thirteen established species, native
planting/harvest/mining/staff paths, replenishment attempts, full resource reload, and
ordinary-tree controls. The fixture includes Patchouli 85 so DT's optional guide recipe
has its book item. Fixture-only defaults disable DT chunk decoration to avoid racing
upstream biome-database mutation against reload; neither those defaults nor the test
server properties are shipped. Concurrent exploration during reload is not accepted by
this test suite.

The reobfuscated runtime JAR is `build/libs/better-wild-fruit-trees-0.1.1.jar`; GameTest
classes and structures are excluded. The default native fixture additionally includes pinned
Ars Nouveau 4.12.7, Curios and GeckoLib to exercise optional sourceberry, plus
TConstruct 3.11.2.166 and Mantle 1.11.104 to exercise native hand/broad axes, material tiers,
native diamond modifier addition/removal, broken/blank/pickaxe negatives, real-player
removal/progress, and ordinary-tree/machine controls. Use
`-PwithoutTConFixture -PwithoutArsFixture -PwithoutWildAddonsFixture` for the companion
absent-optional-mod gate. The default fixture also declares DT Plus 1.2.2, DT Nature's Spirit
1.3/Nature's Spirit 2.2.5, TerraBlender 3.0.1.10, DT Quark 2.5.3/Quark 4.0-462 and Zeta
1.0-31 using pinned Curse Maven file IDs in `build.gradle.kts`; do not substitute warmup-tree
JARs for declared fixture resolution. Fixture flags alter runtime dependencies only; the native
coconut target and typed nonshipping cactus/falling-pod fixture APIs remain compile-only;
production integrations stay optional. Fixtures use public typed construction/observer APIs, not
private-method discovery or runtime proxies. Prior GameTest worlds are retained
under `run-gametest/retained-worlds/` before each fresh native invocation.
Local Gradle invocations must hold the workspace POSIX `fcntl` Forge cache lock for
the complete child-process lifetime: `/home/dev/better-content-modpack/.worklane/forge-cache.lock`.
Canonical `test-control.main.kts run source --repo better-wild-fruit-trees` is preferred;
the controller must resolve that repository-root lock, not the distinct sibling
`/home/dev/.worklane/forge-cache.lock`. BSD `flock` is not interoperable with the canonical Java lock.
Ordinary release verification may inherit `BC_CUSTOM_MOD_JAR_DIR`; this provider has no local
custom-provider dependencies and never consumes that binding. The before-prepare receipt records
its actual absence/presence and nonconsumption. Isolated calibration diagnostics still require
the binding to be absent; their profile and native-input isolation guards are unchanged.
The source gate also covers normalized native bush growth/harvest statistics, exact rate math,
real native tree placement through exposed versus occluded dynamic foliage, success-only credit,
160-item replacement accounting, all protected species' persistent root ownership and native
save/load/level reattachment without offline catch-up. Controlled elapsed-time fixtures establish
accounting, not observed full-pack fruit per calendar day.
Rarity fixtures enumerate every field gate outcome and all 256 tree gate outcomes against
every protected species' actual pinned upstream weighted rule, proving native ordinary fallback,
unchanged density/chance selectors and ordinary-species RNG behavior. They do not establish
representative-seed landscape density or exploration time.
Established thirteen-species growth-rate fixtures run eight seeded sunlit native fresh-growth samples per species,
checking the adult-trunk reference against its calibration with a 20% sample-mean tolerance.
A separate stratified 4,096-draw native `Species.grow` test per species checks actual pulse
frequency, native emitted energy, full-size branch taper, obsolete-age/calendar/reload invariance,
unchanged generated-soil fertility and ordinary-tree controls. Fixture growthMultiplier/folding
match the pack's 0.5/2. Rate math preserves seasons/boosts and proves the nominal thirty-day
expectation; it does not establish twenty observed hours of full-pack canopy development.
Development tests do not constitute full-modpack,
client-rendering, landscape-size, or gameplay-performance acceptance.

## License

New integration code is MIT licensed. Upstream mods remain separately licensed and are
not bundled. See [LICENSE](LICENSE) and [third-party notices](LICENSE-NOTICE.md).
