# Better Wild Fruit Trees

Forge 1.20.1 / Java 17; mod ID `better_wild_fruit_trees`.

- Full source gate: `./gradlew --no-daemon clean verifyFull stageRuntimeJar`.
- Fast gate: `./gradlew --no-daemon verifyFast` (not native acceptance).
- Runtime artifact: `build/libs/better-wild-fruit-trees-<version>.jar`.
- GameTest classes and templates must not ship in the runtime JAR.
- Keep JoCodes, global tree worldgen density, global DT growth settings, ordinary trees,
  non-bush fruit plants and Bumblezone cultivars unchanged. Explicit bush scope: vanilla sweet
  berry, Fruits Delight blueberry/cranberry, and optional Ars Nouveau sourceberry. Their native
  per-pick yields, reset ages and bonemeal remain native. Keep broad 30–42-plant fields over
  a 47×47 footprint, centered on the decoration chunk to keep generation writes local. Natural
  renewal targets 40 berries per mean field and 160 fruit per productive protected tree per
  50,000 active game ticks; preserve Forge growth vetoes/boosts and native geometry/season checks.
  New fields pass an additional 1-in-128 gate and protected weighted tree choices a 1-in-256 gate;
  preserve native biome preferences and unhandled ordinary-tree fallback. Do not cull existing resources.
  Tree production credit belongs to native species-root data, spends only on successful placements,
  banks at most two replacements and must not accrue unloaded/restart time.
- New protected roots keep native worldgen geometry. Their additional growth-signal energy and
  growth-only tapering ramp from native to enlarged settings over 30 extended days (1,500,000
  active/forced chunk ticks), using native inhabited time and persistent root age. No calendar,
  bonemeal or offline catch-up; keep legacy untracked roots mature, ordinary trees unchanged,
  and JoCode/InflatorNode getters native. New tracked worldgen roots left exhausted receive 15
  fertility once after generation; preserve nonzero authored values and never refuel legacy/ordinary
  roots or repeat on reload. Explicitly serialize only owned maturation/fruit-credit records because
  the pinned native species entity skips Forge's superclass save/load. Verify fresh entities and
  protochunk serialization, not same-instance reload alone. This schedule is potential, not guaranteed geometry.
- Native API/mixin targets are pinned to DT 1.4.10, DT Fruits Delight 1.0.0 and Fruits Delight 1.1.3.
  Optional TConstruct public APIs are pinned to 3.11.2.166 / Mantle 1.11.104.
  Changing a pin requires native tests; do not relax required injections to accommodate drift.
- Default native verification includes TConstruct and Ars; additionally run the clean full gate
  with `-PwithoutTConFixture -PwithoutArsFixture` for absent-mod class-loading safety.

Read the shared [workspace](../../better-content-modpack/docs/policies/workspace.md),
[testing](../../better-content-modpack/docs/testing.md) and
[disposal](../../better-content-modpack/docs/policies/generated-data.md) policies.
Use the workspace full-lifetime POSIX `fcntl` Forge cache lock for local Gradle verification;
BSD `flock` is not interoperable with the canonical Java `FileChannel.lock`.
Publication does not authorize modpack activation, deployment, refresh or packaging.
