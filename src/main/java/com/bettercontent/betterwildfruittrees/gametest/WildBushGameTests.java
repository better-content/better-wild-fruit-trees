package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildBushGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bushHarvestRegrowthAndFixedGeometry(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        level.setBlock(pos.above(2), Blocks.LIGHT.defaultBlockState(), 3);
        // Native light propagation is asynchronous; do not confuse an unlit fixture with growth failure.
        h.runAfterDelay(10, () -> {
        for (String id : WildBushes.BLOCKS) {
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
            if (block == null || block == Blocks.AIR) { h.assertTrue(id.startsWith("ars_nouveau:"), "Missing mandatory bush"); continue; }
            var mature = WildBushes.mature(block.defaultBlockState());
            IntegerProperty age = (IntegerProperty)mature.getProperties().stream().filter(p -> p.getName().equals("age")).findFirst().orElseThrow();
            var shape = mature.getShape(level, pos).bounds();
            for (int value : age.getPossibleValues()) {
                var state = mature.setValue(age, value);
                h.assertTrue(state.getShape(level, pos, CollisionContext.empty()).bounds().equals(shape), "Changing physical size " + id);
                h.assertTrue(state.getDestroySpeed(level, pos) >= 80, "Bush hardness " + id);
            }
            level.setBlock(pos, mature, 2);
            var box = new net.minecraft.world.phys.AABB(pos).inflate(2);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
            mature.use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            var picked = level.getBlockState(pos);
            h.assertTrue(picked.is(block) && picked.getValue(age) < mature.getValue(age), "Native picking/reset failed " + id);
            int yield = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).stream().mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(yield >= 1 && yield <= 3, "Native aggregate balancing changed per-bush yield " + id + " count=" + yield);
            for (int i = 0; i < 300 && level.getBlockState(pos).getValue(age) < mature.getValue(age); i++)
                block.randomTick(level.getBlockState(pos), level, pos, net.minecraft.util.RandomSource.create(i));
            h.assertTrue(level.getBlockState(pos).equals(mature), "Native random fruit ripening failed " + id);
            level.setBlock(pos, picked, 2);
            var grower = (net.minecraft.world.level.block.BonemealableBlock)block;
            for (int i = 0; i < 5 && level.getBlockState(pos).getValue(age) < mature.getValue(age); i++)
                grower.performBonemeal(level, level.random, pos, level.getBlockState(pos));
            h.assertTrue(level.getBlockState(pos).equals(mature), "Native fruit regrowth failed " + id);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bushAndTreeActualToolGateAndPlanting(GameTestHelper h) {
        var level = h.getLevel();
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BushGate"));
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var machine = FakePlayerFactory.getMinecraft(level);
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (String id : WildBushes.BLOCKS) {
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
            if (block == null || block == Blocks.AIR) continue;
            var state = WildBushes.mature(block.defaultBlockState());
            level.setBlock(pos, state, 2);
            var moddedAxe = ForgeRegistries.ITEMS.getValue(GameTestTools.AXE);
            h.assertTrue(moddedAxe != Items.AIR, "Missing custom tier fixture");
            for (var item : new net.minecraft.world.item.Item[]{Items.AIR, Items.WOODEN_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.SHEARS, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE, moddedAxe}) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                boolean allowed = item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE || item == moddedAxe;
                h.assertTrue(MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)) != allowed, "Actual break gate " + id + " " + item);
                h.assertTrue((state.getDestroyProgress(player, level, pos) > 0) == allowed, "Client/server mining progress gate " + id);
                if (!allowed) h.assertTrue(!player.gameMode.destroyBlock(pos), "Native player removal bypass " + id);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
            h.assertTrue(player.gameMode.destroyBlock(pos) && level.isEmptyBlock(pos), "Diamond bush removal denied " + id);
            level.setBlock(pos, state, 2);
            machine.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            h.assertTrue(!MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, machine)), "Machine break event altered");
            h.assertTrue(state.getDestroyProgress(machine, level, pos) > 0 && machine.gameMode.destroyBlock(pos), "Machine native removal denied");
            // Existing food/stock remains in inventories, but BlockItem's direct machine-facing route fails.
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(switch(id) {
                case "minecraft:sweet_berry_bush" -> "minecraft:sweet_berries";
                case "fruitsdelight:blueberry_bush" -> "fruitsdelight:blueberry";
                case "fruitsdelight:cranberry_bush" -> "fruitsdelight:cranberry";
                default -> "ars_nouveau:sourceberry_bush";
            }));
            h.assertTrue(item instanceof BlockItem, "Expected pinned native bush item " + id);
            h.assertTrue(item.isEdible(), "Berry food was removed " + id);
            var pot = pos.east();
            level.setBlock(pot, Blocks.FLOWER_POT.defaultBlockState(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            h.assertTrue(MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pot, new BlockHitResult(Vec3.atCenterOf(pot), Direction.UP, pot, false))), "Flower-pot propagation bypass " + id);
            machine.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
            h.assertTrue(((BlockItem)item).place(new BlockPlaceContext(context)) == net.minecraft.world.InteractionResult.FAIL && level.isEmptyBlock(pos), "Native planting bypass " + id);
            var machineContext = new UseOnContext(machine, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
            h.assertTrue(((BlockItem)item).place(new BlockPlaceContext(machineContext)) == net.minecraft.world.InteractionResult.FAIL, "Machine planting bypass");
            level.setBlock(pos, state, 2);
            h.assertTrue(MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(), level, pos), Blocks.AIR.defaultBlockState(), player)), "Forge placement bypass");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        // Connected fruit branch uses the same progress and server-side removal policy.
        var tree = com.ferreusveritas.dynamictrees.tree.species.Species.REGISTRY.get(new ResourceLocation("dynamictrees:apple_oak"));
        tree.placeRootyDirtBlock(level, pos.below(), 15);
        tree.getFamily().getBranch().orElseThrow().setRadius(level, pos, 1, Direction.DOWN, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        h.assertTrue(!player.gameMode.destroyBlock(pos) && level.getBlockState(pos).getDestroyProgress(player, level, pos) == 0, "Protected tree actual removal bypass");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
        h.assertTrue(player.gameMode.destroyBlock(pos), "Diamond tree felling denied");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void optionalTConstructAxesUseNativePerStackTier(GameTestHelper h) {
        for (String mod : new String[]{"tconstruct", "ars_nouveau"}) {
            String expected = System.getProperty("better_wild_fruit_trees.fixture." + mod);
            h.assertTrue(expected != null && Boolean.parseBoolean(expected) == net.minecraftforge.fml.ModList.get().isLoaded(mod), "Wrong optional fixture dependency presence " + mod);
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")) TConstructAxeProof.run(h);
        else {
            h.assertTrue(WildBushes.appropriateAxe(new ItemStack(Items.DIAMOND_AXE)), "Absent optional integration broke vanilla axes");
            h.succeed();
        }
    }

    @GameTest(template = "field", timeoutTicks = 100)
    public static void configuredWorldgenMakesSparseIrregularFields(GameTestHelper h) {
        var level = h.getLevel();
        var requested = h.absolutePos(new BlockPos(32, 2, 32));
        var chunk = new net.minecraft.world.level.ChunkPos(requested);
        var origin = new BlockPos(chunk.getMiddleBlockX(), requested.getY(), chunk.getMiddleBlockZ());
        for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
            level.setBlock(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = 0; y <= 5; y++) level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
        var chunks = new java.util.ArrayList<net.minecraft.world.level.chunk.ChunkAccess>();
        for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) chunks.add(level.getChunk(chunk.x + x, chunk.z + z));
        // Exercise the native decoration write/read boundary, not just unrestricted ServerLevel placement.
        var region = new net.minecraft.server.level.WorldGenRegion(level, chunks, net.minecraft.world.level.chunk.ChunkStatus.FEATURES, 1);
        for (String id : new String[]{"minecraft:patch_berry_bush", "fruitsdelight:blueberry_bush", "fruitsdelight:cranberry_bush", "ars_nouveau:patch_berry"}) {
            var feature = registry.get(new ResourceLocation(id));
            h.assertTrue(feature.feature() instanceof WildBushField, "Configured feature not overridden " + id);
            boolean present = !id.startsWith("ars_nouveau:") || net.minecraftforge.fml.ModList.get().isLoaded("ars_nouveau");
            boolean widened = false;
            for (int[] corner : new int[][]{{0, 0}, {15, 0}, {0, 15}, {15, 15}}) {
                for (int x=-24;x<=24;x++) for(int z=-24;z<=24;z++) level.setBlock(origin.offset(x,0,z), Blocks.AIR.defaultBlockState(), 2);
                var edgeOrigin = new BlockPos(chunk.getMinBlockX() + corner[0], origin.getY(), chunk.getMinBlockZ() + corner[1]);
                var random = net.minecraft.util.RandomSource.create(112 + corner[0] + 16 * corner[1]);
                h.assertTrue(feature.place(region, level.getChunkSource().getGenerator(), random, edgeOrigin) == present, "Native/optional feature placement " + id);
                int count = 0, minX = 99, maxX = -99, minZ = 99, maxZ = -99;
                for (int x=-24;x<=24;x++) for(int z=-24;z<=24;z++) if (WildBushes.isBush(level.getBlockState(origin.offset(x,0,z)))) {
                    count++; minX=Math.min(minX,x); maxX=Math.max(maxX,x); minZ=Math.min(minZ,z); maxZ=Math.max(maxZ,z);
                    h.assertTrue(Math.abs(x) <= 23 && Math.abs(z) <= 23, "Field escaped its 47x47 footprint " + id);
                    var state = level.getBlockState(origin.offset(x,0,z));
                    h.assertTrue(state.equals(WildBushes.mature(state)), "Field bush was not ripe " + id);
                    for (int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) if(dx!=0 || dz!=0)
                        h.assertTrue(!WildBushes.isBush(level.getBlockState(origin.offset(x+dx,0,z+dz))), "Field became a dense hedge " + id);
                }
                h.assertTrue(present ? count >= 30 && count <= 42 : count == 0, "Wrong field aggregate count " + id + " " + count);
                if (present) h.assertTrue(maxX-minX >= 16 && maxZ-minZ >= 16, "Field too compact " + id);
                widened |= maxX-minX > 28 && maxZ-minZ > 28;
            }
            if (present) h.assertTrue(widened, "Fields never exceeded their old 29x29 footprint " + id);
        }
        var absent = new WildBushField().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(), level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(1), origin, new WildBushField.Config("missing_mod:absent_bush")));
        h.assertTrue(!absent, "Optional absent bush was not safe");
        h.succeed();
    }
}
