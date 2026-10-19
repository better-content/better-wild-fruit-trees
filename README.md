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

| Fruit tree | Mature growth-signal energy |
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
apply. The mod does not make trees grow indefinitely or change their growth-rate setting.

Existing JoCodes and global worldgen density are not replaced. Protected fruit-species
selection is deliberately made very rare, with ordinary-tree fallback as described below.
Trees retain their authored generation layouts and can grow toward the increased potential afterward.
There is no giant-tree worldgen rewrite, forced backfill, or chunk-load growth scan.

### Thirty-day additional growth

New protected trees **keep their current native JoCode/worldgen size**. Their additional
post-generation growth potential matures over **30 extended Minecraft days**: **1,500,000
active chunk ticks**, or about **20 hours 50 minutes at 20 TPS**. This is not a seedling-worldgen
rewrite, an instant day-thirty resize, or a guaranteed height.

- Growth signals start at the species' original native energy, then linearly reach the mature
  energy table above at day thirty. Growth-only branch tapering likewise moves from native
  tapering to native/2.5. At day fifteen, half the **additional potential** is available—not
  necessarily half a finished tree's physical dimensions. The native getter used by JoCode's
  InflatorNode is never age-scaled.
- Successful native protected-root placement creates root-owned maturation data. The first
  native growth update anchors a vanilla chunk **inhabited-time** clock; active/forced ticking
  accrues age, while merely loaded but inactive chunks, unloaded terrain and server downtime do
  not. Clock reads are loaded-only; no global tree scan or per-branch persistence writes occur.
- Sleeping/calendar skips and repeated bonemeal/artificial growth pulses cannot award age.
  Native boosts still run growth attempts within the current ceiling. Soil fertility, seasons,
  obstacles and native growth opportunities can delay actual shape development beyond the
  schedule; full enlarged potential becomes available at day thirty, not guaranteed geometry.
- Native JoCodes normally leave generated roots at zero fertility. After generation and initial
  fruit placement finish, a new tracked protected root receives **15 fertility once** if still
  exhausted; existing nonzero authored fertility is preserved. The persisted one-time marker
  prevents repeated notifications/reloads from refueling it. Ordinary and legacy roots are not
  refueled. Subsequent native fertility consumption and replenishment still apply.
- Native root saves preserve earned age. The pinned species entity skips Forge's superclass
  save/load methods, so narrow native-root hooks explicitly serialize only this mod's maturation
  and fruit-credit records. Unrelated Forge data/capabilities and native species are not rewritten.
  Data/level/chunk reattachment rebases the clock on the
  first subsequent growth update, without offline catch-up. Duplicate root initialization does
  not reset progress. **Legacy roots without maturation data retain their full existing growth
  potential**; no existing tree is shrunk, scanned, reset or forcibly resized.
- Fruit ripening, the 160-fruit renewable target, field regrowth and the very-rare discovery
  gates remain independent. A generated tree can already bear fruit before additional growth
  maturation finishes. Ordinary species receive no maturation ledger or growth restriction.

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
The same rule covers the berry bushes below, whose hardness is 80. Picking needs no axe.
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
- Native species-root block entity data persists fractional credit. It starts empty and banks at
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

The wrapper downloads pinned Forge/mod dependencies. `verifyFast` runs Minecraft-free
contracts; `verifyFull` also runs isolated headless Forge GameTests, a native diagnostic
gate, and production archive/refmap checks. Tests cover all thirteen species, native
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
`-PwithoutTConFixture -PwithoutArsFixture` for the clean companion absent-optional-mod gate.
Both flags affect only fixture runtime dependencies; the production integrations stay optional. Prior GameTest worlds are retained
under `run-gametest/retained-worlds/` before each fresh native invocation.
Local Gradle invocations must hold the workspace POSIX `fcntl` Forge cache lock for
the complete child-process lifetime (canonical `test-control.main.kts run source --repo`
is preferred); BSD `flock` is not interoperable with the canonical Java lock.
The source gate also covers normalized native bush growth/harvest statistics, exact rate math,
real native tree placement through exposed versus occluded dynamic foliage, success-only credit,
160-item replacement accounting, all protected species' persistent root ownership and native
save/load/level reattachment without offline catch-up. Controlled elapsed-time fixtures establish
accounting, not observed full-pack fruit per calendar day.
Rarity fixtures enumerate every field gate outcome and all 256 tree gate outcomes against
every protected species' actual pinned upstream weighted rule, proving native ordinary fallback,
unchanged density/chance selectors and ordinary-species RNG behavior. They do not establish
representative-seed landscape density or exploration time.
Maturation fixtures exercise actual native growth-signal emission and real branch tapering
for all thirteen species at birth, halfway, just before day thirty and maturity. They cover
chunk-clock accounting, native bonemeal pulses, calendar skips, actual protochunk birth/NBT
serialization and fresh-entity reconstruction, one-time generated-soil establishment, save/load
and existing-chunk reattachment, fruit-credit independence, legacy roots, ordinary-tree controls and missing-chunk
safety. Controlled elapsed-time fixtures do not establish twenty observed hours of full-pack
physical tree growth.
Development tests do not constitute full-modpack,
client-rendering, landscape-size, or gameplay-performance acceptance.

## License

New integration code is MIT licensed. Upstream mods remain separately licensed and are
not bundled. See [LICENSE](LICENSE) and [third-party notices](LICENSE-NOTICE.md).
