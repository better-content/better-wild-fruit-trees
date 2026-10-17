# Better Wild Fruit Trees

A standalone Forge 1.20.1 integration that makes Fruits Delight's dynamic fruit trees
large, bountiful, fixed-location food resources rather than portable orchard stock.

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

| Fruit tree | Growth-signal energy |
| --- | ---: |
| Mangosteen, hawberry | 20 |
| Apple, orange, pear, lychee | 25 |
| Persimmon, peach | 30 |
| Bayberry, fig | 35 |
| Kiwi | 37.5 |
| Durian, mango | 40 |

These are growth budgets, **not heights in blocks or guaranteed mature dimensions**.
Protected species use 1/2.5 of their original tapering and three times their native
biome-adjusted soil longevity to support the longer growth paths. Native family trunk
thickness caps, space, light, soil fertility, seasonal behavior, and growth logic still
apply. The mod does not make trees grow indefinitely or change their growth-rate setting.

Existing JoCodes and worldgen species selection/density are not replaced. Trees retain
their authored generation layouts and can grow toward the increased potential afterward.
There is no giant-tree worldgen rewrite, forced backfill, or chunk-load growth scan.

### Bountiful fruit

- Mature dynamic fruit yields **8 times its native product count**.
- Natural fruit replenishment uses **at least 12 placement attempts**, rather than the
  native default of four. Initial worldgen fruit placement is not increased.
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

This makes early chopping impractical and accidental felling unlikely; it is **not a
hard tool-tier prohibition or indestructibility**. Fire, explosions, root destruction,
and third-party machine removal retain their native behavior.

## Explicitly unchanged

- Ordinary oak, birch, spruce, jungle, and other non-target species.
- Fruits Delight's bushes, pineapple, melons, and other non-tree plants.
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

The reobfuscated runtime JAR is `build/libs/better-wild-fruit-trees-0.1.0.jar`; GameTest
classes and structures are excluded. Development tests do not constitute full-modpack,
client-rendering, landscape-size, or gameplay-performance acceptance.

## License

New integration code is MIT licensed. Upstream mods remain separately licensed and are
not bundled. See [LICENSE](LICENSE) and [third-party notices](LICENSE-NOTICE.md).
