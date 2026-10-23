# Better Wild Fruit Trees

Forge 1.20.1 / Java 17; mod ID `better_wild_fruit_trees`.

- Full source gate: `./gradlew --no-daemon clean verifyFull stageRuntimeJar`.
- Fast gate: `./gradlew --no-daemon verifyFast` (not native acceptance).
- Runtime artifact: `build/libs/better-wild-fruit-trees-<version>.jar`.
- GameTest classes and templates must not ship in the runtime JAR.
- Keep JoCodes, global tree worldgen density, global DT growth settings, ordinary non-target trees,
  non-bush fruit plants and Bumblezone cultivars unchanged except the three selected optional
  resources and mining-only lemon rule below. Explicit managed berry scope: vanilla sweet
  berry, Fruits Delight blueberry/cranberry, and optional Ars Nouveau sourceberry. Their native
  per-pick yields, reset ages and bonemeal remain native. Keep broad 30–42-plant fields over
  a 47×47 footprint, centered on the decoration chunk to keep generation writes local. Natural
  renewal targets 40 berries per mean field and 160 fruit per productive protected tree per
  50,000 active game ticks; preserve Forge growth vetoes/boosts and native geometry/season checks.
  New fields pass an additional 1-in-128 gate and protected weighted tree choices a 1-in-256 gate;
  preserve native biome preferences and unhandled ordinary-tree fallback. Do not cull existing resources.
  Legacy thirteen-species production credit belongs to native species-root data, spends only on
  successful placements, banks at most two replacements and must not accrue unloaded/restart time.
- Source-only optional full-policy scope is exactly `dynamictreesplus:saguaro_cactus`,
  `dtnatures_spirit:coconut`, and `dtquark:ancient`; it is not ready for activation. Preserve ripe
  native food/product routes, including placeable whole-coconut processing; prohibit germination,
  native coconut sprouts, dedicated propagation stock and protected planting/recreation/transform
  routes, not blanket food deletion. Saguaro/coconut/ancient energy is 10/25/60; woody growth taper
  is /2.5, cacti retain native thickness. Threefold soil longevity and the existing hardness/axe,
  eightfold ripe product and 2,500-tick/two-replacement allowance apply. The new roots use schema-1
  dimension SavedData with runtime-only clocks, not a species-block-entity requirement. Only proven
  same-root saguaro→mega conversion inherits energy 45/protection/growth/soil, never invented fruit
  production. Preserve unknown schema bytes and reject use; synchronize exact loaded provenance
  without inferring historical mega origins. Saguaro rarity rejection is native pillar:pipe 6:1;
  other rejected protected selections are unhandled. Keep ordinary selector RNG unchanged.
  Cocoa, archwood pods, Aether trees, ordinary cacti/mega cacti and the old thirteen stay native to
  their existing contracts. Four measured references now have separate explicit lead Source admission
  after complete retained V9 review: `saguaro` (native rate `1F`, mean 26.75 pulses), `coconut`
  (`.8F`, 14.0), `ancient` (`.5F`, 89.125), `saguaro_mega` (`.6F`, 67.25), from diagnostic receipt
  `5c099226-736b-4bf2-8bcd-3c70f889673d` (eight per form, 32 total). Keep required-reference guards,
  unit/release contracts and staging checks unchanged. This Source-data admission is not activation,
  present26/absent26, full-gate, shipping/clean45, staging or pack Debug acceptance; those remain pending.
  Legacy thirteen references use native maximum trunk radius; cactus maturity cannot use that endpoint. The predeclared versioned endpoint is documented in
  [native cactus architecture](docs/cactus-architecture.md); preserve its native-origin topology
  and provenance requirements. Coconut uses only the explicitly reviewed
  [native fruiting-crown structure](docs/coconut-architecture.md), not full-cap wood/full canopy/
  actual fruit; preserve live typed authority, derived single-path envelope and hydrated native crown.
  V9 measures `native_upturned_four_arm_v1` for cactus, `native_coconut_fruiting_crown_v1` for coconut
  and native maximum trunk radius for ancient; separate admission permits only these four Source entries.
  Measurements never automatically admit references; never use a plateau/cap or substitute native rates
  or invented pulses. No builds/tests/native
  diagnostics, deployment, refresh, packaging or cleanup are authorized by this source-only work.
- Fruits Delight `fruitsdelight:lemon_tree` has mining-only protection: minimum hardness 80
  and the existing diamond-or-better axe gate, at every age and on both halves. Keep it out
  of the managed berry selectors; native planting, geometry, harvest/loot, growth/bonemeal,
  worldgen and support/companion-half survival remain unchanged. Preserve existing fake-player
  exemptions and shared Creative behavior.
- Protected wood growth uses constant per-species growth-rate multipliers calibrated to roughly
  30 extended days (1,500,000 ticks) from fresh native geometry to native full trunk radius under
  favorable conditions, at randomTickSpeed 3/global growthMultiplier 0.5. No age ledger, size ramp,
  day-thirty event or automatic fertility supply. Keep full-size energy/taper from the first pulse,
  JoCode/InflatorNode getters and generated size/soil native, ordinary rates untouched, and native
  seasonal/explicit boosts proportional. Native canopy can continue filling after the adult-trunk
  reference; no guaranteed physical maximum/deadline. Verify real native pulse frequency and seeded
  fresh growth, not just rate arithmetic. Only fruit credit is persisted through explicit owned-key
  hooks because the pinned species entity skips Forge's superclass serializer; test fresh entities.
- Native API/mixin targets are pinned to DT 1.4.10, DT Fruits Delight 1.0.0 and Fruits Delight 1.1.3.
  Optional TConstruct public APIs are pinned to 3.11.2.166 / Mantle 1.11.104. Optional tree
  adapters are pinned to DT Plus 1.2.2, DT Nature's Spirit 1.3/Nature's Spirit 2.2.5 and DT Quark
  2.5.3/Quark 4.0-462, with fixture dependencies declared by immutable Curse Maven file IDs.
  Changing a pin requires native tests; do not relax required injections to accommodate drift.
- Once separately authorized and calibration-complete, default native verification includes
  TConstruct, Ars and the three optional tree addons. The main gate requires exactly 26 tests,
  including the lemon pair and seven authored optional contracts. The isolated
  `-PoptionalGrowthCalibration runGameTestServer` diagnostic is not that gate. Additionally run
  the full gate with `-PwithoutTConFixture -PwithoutArsFixture -PwithoutWildAddonsFixture` for
  absent-mod class-loading safety; native fixtures assert requested presence, not silent skips.

Read the shared [workspace](../../better-content-modpack/docs/policies/workspace.md),
[testing](../../better-content-modpack/docs/testing.md) and
[disposal](../../better-content-modpack/docs/policies/generated-data.md) policies.
Use `/home/dev/better-content-modpack/.worklane/forge-cache.lock`, held with whole-lifetime POSIX
`fcntl`, for local Gradle verification. The source controller must resolve that repository-root
lock, never the distinct sibling `/home/dev/.worklane/forge-cache.lock`.
Register fixture/evidence producer paths and retain outputs across handoff;
BSD `flock` is not interoperable with the canonical Java `FileChannel.lock`.
Publication does not authorize modpack activation, deployment, refresh or packaging.
