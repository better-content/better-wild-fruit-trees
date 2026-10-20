package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.WildBushes;
import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Lemon must share mining protection, not managed berry planting/geometry/production. */
@GameTestHolder(WildFruitTrees.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LemonMiningGameTests {
    private static Block lemon() {
        return java.util.Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("fruitsdelight", "lemon_tree")));
    }
    private static IntegerProperty age(Block block) {
        return (IntegerProperty)block.getStateDefinition().getProperty("age");
    }
    private static ServerPlayer player(GameTestHelper h) {
        var level = h.getLevel();
        var player = new ServerPlayer(level.getServer(), level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "LemonGate"));
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }
    private static void plant(GameTestHelper h, BlockPos pos, int value) {
        var level = h.getLevel();
        var block = lemon();
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        var lower = block.defaultBlockState().setValue(age(block), value)
            .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(pos, lower, 2);
        if (value >= 2) level.setBlock(pos.above(), lower.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
    }
    private static void gate(GameTestHelper h, ServerPlayer player, BlockPos pos, ItemStack tool, boolean allowed) {
        var level = h.getLevel();
        var state = level.getBlockState(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        float progress = state.getDestroyProgress(player, level, pos);
        h.assertTrue(allowed ? progress > 0 && progress < 1 : progress == 0,
            "Lemon mining progress gate " + tool + " " + state);
        h.assertTrue(MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)) != allowed,
            "Lemon server break gate " + tool + " " + state);
        if (!allowed) h.assertTrue(!player.gameMode.destroyBlock(pos) && level.getBlockState(pos).equals(state),
            "Disallowed lemon removal " + tool);
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void lemonMiningAllNativeAgesAndHalves(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var player = player(h);
        var block = lemon();
        h.assertTrue(block != Blocks.AIR, "Missing pinned lemon block");
        h.assertTrue(!WildBushes.isBush(block.defaultBlockState()), "Lemon entered managed berry selectors");
        for (int value : age(block).getPossibleValues()) {
            plant(h, pos, value);
            for (int half = 0; half < (value >= 2 ? 2 : 1); half++) {
                var target = pos.above(half);
                var state = level.getBlockState(target);
                h.assertTrue(WildBushes.protectedResource(state, level, target), "Unprotected lemon state " + state);
                h.assertTrue(state.getDestroySpeed(level, target) >= 80, "Instant-break lemon state " + state);
                h.assertTrue(state.getShape(level, target).bounds().equals(
                    block.getShape(state, level, target, CollisionContext.empty()).bounds()), "Lemon shape overridden " + state);
                for (var item : new net.minecraft.world.item.Item[]{Items.AIR, Items.WOODEN_AXE, Items.STONE_AXE,
                    Items.IRON_AXE, Items.GOLDEN_AXE, Items.SHEARS, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE})
                    gate(h, player, target, new ItemStack(item), item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE);
                var growth = new BlockEvent.CropGrowEvent.Pre(level, target, state);
                MinecraftForge.EVENT_BUS.post(growth);
                h.assertTrue(growth.getResult() == net.minecraftforge.eventbus.api.Event.Result.DEFAULT,
                    "Lemon entered custom berry growth handler");
            }
        }
        for (var control : new Block[]{Blocks.AZALEA, Blocks.WHEAT}) {
            level.setBlock(pos, control.defaultBlockState(), 2);
            h.assertTrue(!WildBushes.protectedResource(control.defaultBlockState(), level, pos), "Unrelated plant protected");
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")) TConstructCases.check(h, player, pos);
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void lemonPairedRemovalHarvestPlantingAndSurvival(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var block = lemon();
        var player = player(h);
        for (int half = 0; half < 2; half++) {
            plant(h, pos, 4);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
            h.assertTrue(player.gameMode.destroyBlock(pos.above(half)), "Diamond lemon removal denied");
            h.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()), "Native companion cleanup failed");
            plant(h, pos, 4);
            var box = new net.minecraft.world.phys.AABB(pos).inflate(2);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var target = pos.above(half);
            level.getBlockState(target).use(level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
            for (int i = 0; i < 2; i++) h.assertTrue(level.getBlockState(pos.above(i)).is(block)
                && level.getBlockState(pos.above(i)).getValue(age(block)) == 2, "Native lemon harvest reset changed");
            var fruit = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fruitsdelight", "lemon"));
            int yield = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).stream()
                .filter(e -> e.getItem().is(fruit)).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(yield >= 1 && yield <= 2, "Native lemon harvest yield changed: " + yield);
        }
        plant(h, pos, 4);
        var machine = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        machine.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        h.assertTrue(!MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), machine))
            && level.getBlockState(pos).getDestroyProgress(machine, level, pos) > 0, "Machine exemption changed");
        h.assertTrue(machine.gameMode.destroyBlock(pos) && level.isEmptyBlock(pos), "Machine removal denied");
        plant(h, pos, 4);
        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()), "Native support survival changed");
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        var seeds = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fruitsdelight", "lemon_seeds"));
        h.assertTrue(seeds instanceof BlockItem, "Missing native lemon seed BlockItem");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(seeds));
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
        h.assertTrue(((BlockItem)seeds).place(new BlockPlaceContext(context)).consumesAction()
            && level.getBlockState(pos).is(block), "Native lemon planting denied");
        var snapshot = net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(), level, pos);
        h.assertTrue(!MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot, Blocks.GRASS_BLOCK.defaultBlockState(), player)),
            "Lemon entered managed berry placement gate");
        for (int i = 0; i < 4; i++) ((net.minecraft.world.level.block.BonemealableBlock)block)
            .performBonemeal(level, level.random, pos, level.getBlockState(pos));
        h.assertTrue(level.getBlockState(pos).getValue(age(block)) == 4 && level.getBlockState(pos.above()).is(block),
            "Native lemon bonemeal growth changed");
        h.succeed();
    }
    /** Only resolved when the optional dependency is present. No optional types in outer signatures. */
    private static final class TConstructCases {
        private static void check(GameTestHelper h, ServerPlayer player, BlockPos pos) {
            for (boolean broad : new boolean[]{false, true}) {
                var item = broad ? slimeknights.tconstruct.tools.TinkerTools.broadAxe.get()
                    : slimeknights.tconstruct.tools.TinkerTools.handAxe.get();
                for (String head : new String[]{"iron", "cobalt", "manyullyn"}) {
                    var materials = slimeknights.tconstruct.library.tools.nbt.MaterialNBT.builder()
                        .add(new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct", head))
                        .add(new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct", "wood"));
                    if (broad) materials.add(new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct", head));
                    materials.add(new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct", "wood"));
                    var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.createTool(item, item.getToolDefinition(), materials.build());
                    for (int half = 0; half < 2; half++) {
                        plant(h, pos, 4);
                        gate(h, player, pos.above(half), tool.createStack(), !head.equals("iron"));
                        player.gameMode.destroyBlock(pos.above(half));
                        h.assertTrue(head.equals("iron") ? h.getLevel().getBlockState(pos).is(lemon())
                            : h.getLevel().isEmptyBlock(pos) && h.getLevel().isEmptyBlock(pos.above()), "TCon lemon removal state");
                    }
                    if (head.equals("iron")) {
                        var diamond = new slimeknights.tconstruct.library.modifiers.ModifierId("tconstruct", "diamond");
                        tool.addModifier(diamond, 1);
                        plant(h, pos, 4);
                        gate(h, player, pos, tool.createStack(), true);
                        tool.removeModifier(diamond, 1);
                        gate(h, player, pos, tool.createStack(), false);
                    }
                    tool.setDamage(tool.getStats().getInt(slimeknights.tconstruct.library.tools.stat.ToolStats.DURABILITY));
                    plant(h, pos, 4);
                    gate(h, player, pos, tool.createStack(), false);
                }
                plant(h, pos, 4);
                gate(h, player, pos, new ItemStack(item), false);
            }
        }
    }
}
