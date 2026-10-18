# Better Wild Fruit Trees

Forge 1.20.1 / Java 17; mod ID `better_wild_fruit_trees`.

- Full source gate: `./gradlew --no-daemon clean verifyFull stageRuntimeJar`.
- Fast gate: `./gradlew --no-daemon verifyFast` (not native acceptance).
- Runtime artifact: `build/libs/better-wild-fruit-trees-<version>.jar`.
- GameTest classes and templates must not ship in the runtime JAR.
- Keep JoCodes, tree worldgen selection/density, global DT growth settings, ordinary trees,
  non-bush fruit plants and Bumblezone cultivars unchanged. Explicit bush scope: vanilla sweet
  berry, Fruits Delight blueberry/cranberry, and optional Ars Nouveau sourceberry. Their native
  per-plant fruit yields/regrowth remain; abundance comes from broad 10–14-plant fields.
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
