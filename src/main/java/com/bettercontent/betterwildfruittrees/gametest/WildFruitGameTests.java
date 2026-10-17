package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock;
import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import com.ferreusveritas.dynamictrees.systems.substance.TransformSubstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;

@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildFruitGameTests {
    private static String speciesId(String name) { return name.equals("apple") ? "dynamictrees:apple_oak" : "dtfruitsdelight:" + name; }
    private static Species species(String name) { return Species.REGISTRY.get(new ResourceLocation(speciesId(name))); }
    private static Fruit fruit(String name) { return Fruit.REGISTRY.get(new ResourceLocation(name.equals("apple") ? "dynamictrees:apple" : "dtfruitsdelight:" + name)); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allSpeciesHaveLargerPotentialAndCannotPropagate(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
        for (var entry : WildFruitPolicy.ENERGY.entrySet()) {
            Species tree = species(entry.getKey());
            h.assertTrue(tree.isValid(), "Missing species " + entry.getKey());
            h.assertTrue(tree.getSignalEnergy() == entry.getValue(), "Wrong energy: " + entry.getKey());
            h.assertTrue(tree.getGrowthLogicKit().getEnergy(new com.ferreusveritas.dynamictrees.growthlogic.context.PositionalSpeciesContext(level, pos, tree)) == entry.getValue(), "Native growth logic did not consume larger energy");
            h.assertTrue(!tree.shouldDropSeeds() && tree.getVoluntaryDrops(level, pos, 15).isEmpty(), "Seed drop route survived");
            h.assertTrue(tree.getPrimitiveSaplingRecipes().isEmpty(), "Conversion survived");
            h.assertTrue(!tree.plantSapling(level, pos, false), "Direct planting survived");
            h.assertTrue(!tree.isTransformable(), "Can transform a fruit tree");
            h.assertTrue(!new TransformSubstance(tree).apply(level, pos), "Can transform into fruit tree");
            var seed = tree.getSeed().orElseThrow();
            ItemStack stack = new ItemStack(seed);
            stack.getOrCreateTag().putBoolean("ForcePlant", true);
            h.assertTrue(!seed.shouldPlant(level, pos, stack), "Forced self planting survived");
            h.assertTrue(!seed.doPlanting(level, pos, null, stack), "Code-bearing seed planting survived");
            h.assertTrue(seed.getPlant(level, pos).isAir(), "Machine plant interface exposed sapling");
            level.setBlock(pos, tree.getSapling().orElseThrow().defaultBlockState(), 2);
            h.assertTrue(!tree.transitionToTree(level, pos), "Pre-existing dynamic sapling grew into a new tree");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        h.assertTrue(Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak")).getSignalEnergy() == 12F, "Ordinary oak changed");
        h.assertTrue(Species.REGISTRY.get(new ResourceLocation("dynamictrees:birch")).getSignalEnergy() == 14F, "Ordinary birch changed");
        Species oak = Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak"));
        h.assertTrue(oak.plantSapling(level, pos, false), "Ordinary oak planting was blocked");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void rootIdentityProtectsSharedAndStrippedBranchesButNotOrdinaryTrees(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos root = h.absolutePos(new BlockPos(4, 1, 4));
        BlockPos trunk = root.above();
        for (String name : WildFruitPolicy.ENERGY.keySet()) {
            Species tree = species(name);
            level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
            h.assertTrue(tree.placeRootyDirtBlock(level, root, 15), "Root placement failed " + name);
            for (var branch : new com.ferreusveritas.dynamictrees.block.branch.BranchBlock[]{tree.getFamily().getBranch().orElseThrow(), tree.getFamily().getStrippedBranch().orElseThrow()}) {
                branch.setRadius(level, trunk, 1, Direction.DOWN, 2);
                var state = level.getBlockState(trunk);
                h.assertTrue(LoadedTreeLookup.isProtected(level, trunk), "Lost exact species " + name);
                h.assertTrue(((BasicBranchBlock)branch).getHardness(state, level, trunk) >= 80F, "Twig/stripped hardness bypass " + name);
            }
            level.setBlock(trunk, Blocks.AIR.defaultBlockState(), 2);
        }
        for (String name : new String[]{"oak", "birch", "spruce", "jungle"}) {
            Species tree = Species.REGISTRY.get(new ResourceLocation("dynamictrees:" + name));
            level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
            tree.placeRootyDirtBlock(level, root, 15);
            var branch = tree.getFamily().getBranch().orElseThrow();
            branch.setRadius(level, trunk, 1, Direction.DOWN, 2);
            h.assertTrue(!LoadedTreeLookup.isProtected(level, trunk), "Ordinary tree protected " + name);
            h.assertTrue(((BasicBranchBlock)branch).getHardness(level.getBlockState(trunk), level, trunk) < 80F, "Ordinary tree hardness changed " + name);
            level.setBlock(trunk, Blocks.AIR.defaultBlockState(), 2);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void matureFruitYieldsEightfoldAndImmatureFruitDoesNot(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 4, 4));
        level.setBlock(pos.above(), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        var box = new net.minecraft.world.phys.AABB(pos).inflate(2);
        for (String name : WildFruitPolicy.ENERGY.keySet()) {
            Fruit fruit = fruit(name);
            var block = fruit.getBlock();
            var mature = block.defaultBlockState().setValue(fruit.getAgeProperty(), fruit.getMaxAge());
            level.setBlock(pos, mature, 2);
            var drops = Block.getDrops(mature, level, pos, null);
            int count = drops.stream().filter(stack -> stack.is(fruit.getItemStack().getItem())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(count >= 8 && count % 8 == 0, "Mature bounty missing " + name + ": " + count);
            h.assertTrue(drops.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()), "Oversized stack");
            var young = mature.setValue(fruit.getAgeProperty(), 0);
            h.assertTrue(Block.getDrops(young, level, pos, null).isEmpty(), "Immature harvest " + name);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
            block.use(mature, level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false));
            int spawned = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).stream()
                .filter(entity -> entity.getItem().is(fruit.getItemStack().getItem())).mapToInt(entity -> entity.getItem().getCount()).sum();
            h.assertTrue(spawned == count, "Real picking did not spawn the previewed bounty " + name);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fallingDurianIsAmplifiedExactlyOnce(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 4, 4));
        Fruit durian = fruit("durian");
        var block = (com.dtteam.dtfruitsdelight.blocks.FallingFruitBlock)durian.getBlock();
        var state = block.defaultBlockState().setValue(durian.getAgeProperty(), durian.getMaxAge());
        level.setBlock(pos, state, 2);
        var entity = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, pos, state);
        h.assertTrue(block.getDropOnFallItems(entity).getCount() == 8, "Falling durian bounty missing or multiplied twice");
        entity.discard();
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allLootSourcesStripPropagulesAndRetainFoodAndOrdinarySeeds(GameTestHelper h) {
        var modifier = new PropagationLootModifier(new LootItemCondition[0]);
        for (String name : WildFruitPolicy.ENERGY.keySet()) {
            var loot = new ObjectArrayList<ItemStack>();
            loot.add(new ItemStack(species(name).getSeed().orElseThrow()));
            loot.add(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("fruitsdelight:" + name + "_sapling"))));
            loot.add(new ItemStack(Items.OAK_SAPLING));
            loot.add(new ItemStack(Items.WHEAT_SEEDS));
            loot.add(fruit(name).getItemStack());
            var result = modifier.apply(loot, null);
            h.assertTrue(result.size() == 3, "Propagation loot survived or ordinary loot lost " + name);
        }
        h.succeed();
    }

    private static final class CountingFruitFeature extends com.ferreusveritas.dynamictrees.systems.genfeature.FruitGenFeature {
        int attempts;
        CountingFruitFeature() { super(new ResourceLocation(WildFruitTrees.MOD_ID, "counting_fruit")); }
        @Override protected void place(com.ferreusveritas.dynamictrees.systems.genfeature.GenFeatureConfiguration configuration,
            Species species, net.minecraft.world.level.LevelAccessor level, BlockPos treePos, BlockPos branchPos, Float season) { attempts++; }
        void run(com.ferreusveritas.dynamictrees.systems.genfeature.GenFeatureConfiguration configuration,
            com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext context) { super.postGrow(configuration, context); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeReplenishmentUsesTwelveAttemptsWithoutMutatingWorldgenConfig(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos root = h.absolutePos(new BlockPos(4, 1, 4));
        for (String id : new String[]{"dtfruitsdelight:pear", "dynamictrees:oak"}) {
            Species tree = Species.REGISTRY.get(new ResourceLocation(id));
            level.setBlock(root, Blocks.DIRT.defaultBlockState(), 2);
            tree.placeRootyDirtBlock(level, root, 15);
            var branch = tree.getFamily().getBranch().orElseThrow();
            branch.setRadius(level, root.above(), 8, Direction.DOWN, 2);
            branch.setRadius(level, root.above(2), 1, Direction.DOWN, 2);
            CountingFruitFeature counter = new CountingFruitFeature();
            var config = counter.createDefaultConfiguration().with(com.ferreusveritas.dynamictrees.systems.genfeature.FruitGenFeature.FRUIT, fruit("pear"));
            counter.run(config, new com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext(level, root, tree, root.above(), 15, true));
            h.assertTrue(counter.attempts == (id.endsWith(":pear") ? 12 : 4), "Wrong native replenishment attempts: " + counter.attempts);
            h.assertTrue(config.get(com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature.QUANTITY) == 4, "Worldgen/shared feature configuration mutated");
            counter.attempts = 0;
            counter.run(config, new com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext(level, root, tree, root.above(), 15, false));
            h.assertTrue(counter.attempts == 0, "Non-natural growth unexpectedly replenished fruit");
            level.setBlock(root.above(2), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 2);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void realForgeInteractionEventsRejectStockButNotFoodOrOrdinarySaplings(GameTestHelper h) {
        var level = h.getLevel();
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        BlockPos pos = h.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (String name : WildFruitPolicy.ENERGY.keySet()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(species(name).getSeed().orElseThrow()));
            var event = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            h.assertTrue(event.isCanceled(), "Native event bus did not reject seed " + name);
        }
        for (ItemStack stack : new ItemStack[]{new ItemStack(Items.OAK_SAPLING), fruit("pear").getItemStack()}) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            var event = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            h.assertTrue(!event.isCanceled(), "Ordinary sapling or fruit food was blocked");
        }
        var staff = com.ferreusveritas.dynamictrees.init.DTRegistries.STAFF.get();
        ItemStack staffStack = new ItemStack(staff);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, staffStack);
        staff.setSpecies(staffStack, species("pear"));
        h.assertTrue(staff.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit)) == net.minecraft.world.InteractionResult.FAIL, "Staff recreated protected tree");
        Species oak = Species.REGISTRY.get(new ResourceLocation("dynamictrees:oak"));
        oak.placeRootyDirtBlock(level, pos, 15);
        oak.getFamily().getBranch().orElseThrow().setRadius(level, pos.above(), 8, Direction.DOWN, 2);
        h.assertTrue(!new TransformSubstance(species("pear")).apply(level, pos), "Ordinary tree transformed into protected tree");
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
        Species pear = species("pear");
        pear.placeRootyDirtBlock(level, pos, 15);
        pear.getFamily().getBranch().orElseThrow().setRadius(level, pos.above(), 8, Direction.DOWN, 2);
        staffStack = new ItemStack(staff);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, staffStack);
        for (BlockPos clicked : new BlockPos[]{pos.above(), pos.above(2)}) {
            if (clicked.equals(pos.above(2))) level.setBlock(clicked, Blocks.GRASS.defaultBlockState(), 2);
            var sourceHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(clicked), Direction.UP, clicked, false);
            h.assertTrue(staff.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, sourceHit)) == net.minecraft.world.InteractionResult.FAIL, "Staff copied protected tree through branch/replaceable targeting");
        }
        BlockPos distant = new BlockPos(10000000, 80, 10000000);
        h.assertTrue(!level.hasChunkAt(distant), "Distant negative-control chunk already loaded");
        h.assertTrue(!LoadedTreeLookup.isProtected(level, distant) && !level.hasChunkAt(distant), "Mining lookup loaded a distant chunk");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "zz_reload", timeoutTicks = 1200)
    public static void resourceReloadPreservesPolicyAndLeavesNoStockRecipes(GameTestHelper h) {
        var level = h.getLevel();
        var reload = level.getServer().reloadResources(level.getServer().getPackRepository().getSelectedIds());
        h.succeedWhen(() -> {
            h.assertTrue(reload.isDone(), "Waiting for resource reload");
            reload.join();
            for (var entry : WildFruitPolicy.ENERGY.entrySet()) {
                h.assertTrue(species(entry.getKey()).getSignalEnergy() == entry.getValue(), "Reload lost energy");
                h.assertTrue(species(entry.getKey()).getPrimitiveSaplingRecipes().isEmpty(), "Reload restored conversions");
            }
            for (var recipe : level.getRecipeManager().getRecipes()) {
                var stack = recipe.getResultItem(level.registryAccess());
                h.assertTrue(!WildFruitPolicy.blockedPlantItem(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()), "Planting recipe survived reload: " + recipe.getId());
            }
        });
    }
}
