# Better Wild Fruit Trees

Forge 1.20.1 / Java 17; mod ID `better_wild_fruit_trees`.

- Full source gate: `./gradlew --no-daemon clean verifyFull stageRuntimeJar`.
- Fast gate: `./gradlew --no-daemon verifyFast` (not native acceptance).
- Runtime artifact: `build/libs/better-wild-fruit-trees-<version>.jar`.
- GameTest classes and templates must not ship in the runtime JAR.
- Keep JoCodes, worldgen selection/density, global DT growth settings, ordinary trees,
  non-tree fruit plants and Bumblezone cultivars unchanged.
- Native API/mixin targets are pinned to DT 1.4.10, DT Fruits Delight 1.0.0 and Fruits Delight 1.1.3.
  Changing a pin requires native tests; do not relax required injections to accommodate drift.

Read the shared [workspace](../../better-content-modpack/docs/policies/workspace.md),
[testing](../../better-content-modpack/docs/testing.md) and
[disposal](../../better-content-modpack/docs/policies/generated-data.md) policies.
Use the workspace full-lifetime Forge cache lock for local Gradle verification.
Publication does not authorize modpack activation, deployment, refresh or packaging.
