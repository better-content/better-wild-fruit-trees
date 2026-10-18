package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.WildBushes;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Blocks;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerTools;

/** Typed optional fixture, never resolved by the absent-TCon GameTest entry point. */
public final class TConstructAxeProof {
    private record Case(ItemStack stack, boolean allowed, String label) {}
    private static ToolStack make(ModifiableItem item, String head, boolean broad) {
        var materials = MaterialNBT.builder().add(new MaterialId("tconstruct", head))
            .add(new MaterialId("tconstruct", "wood"));
        if (broad) materials.add(new MaterialId("tconstruct", head));
        materials.add(new MaterialId("tconstruct", "wood"));
        return ToolStack.createTool(item, item.getToolDefinition(), materials.build());
    }
    private static void branch(GameTestHelper h, BlockPos pos, String speciesId) {
        var level = h.getLevel();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
        var species = Species.REGISTRY.get(new ResourceLocation(speciesId));
        species.placeRootyDirtBlock(level, pos.below(), 15);
        species.getFamily().getBranch().orElseThrow().setRadius(level, pos, 8, Direction.DOWN, 2);
    }
    public static void run(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TConAxeGate"));
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var cases = new java.util.ArrayList<Case>();
        var controls = new java.util.ArrayList<ItemStack>();
        for (boolean broad : new boolean[]{false, true}) {
            var item = broad ? TinkerTools.broadAxe.get() : TinkerTools.handAxe.get();
            String name = broad ? "broad axe" : "hand axe";
            for (String head : new String[]{"iron", "cobalt", "manyullyn"}) {
                var tool = make(item, head, broad);
                var tier = tool.getStats().get(ToolStats.HARVEST_TIER);
                h.assertTrue(tier == (head.equals("iron") ? Tiers.IRON : head.equals("cobalt") ? Tiers.DIAMOND : Tiers.NETHERITE), "Native material mining tier " + name + " " + head);
                cases.add(new Case(tool.createStack(), !head.equals("iron"), name + " " + head));
            }
            var upgraded = make(item, "iron", broad);
            h.assertTrue(!WildBushes.appropriateAxe(upgraded.createStack()), "Iron initially exceeds threshold");
            upgraded.addModifier(new ModifierId("tconstruct", "diamond"), 1);
            h.assertTrue(upgraded.getStats().get(ToolStats.HARVEST_TIER) == Tiers.DIAMOND, "Native diamond modifier did not raise harvest tier");
            cases.add(new Case(upgraded.createStack().copy(), true, name + " native diamond modifier"));
            upgraded.removeModifier(new ModifierId("tconstruct", "diamond"), 1);
            h.assertTrue(upgraded.getStats().get(ToolStats.HARVEST_TIER) == Tiers.IRON, "Native modifier removal did not lower tier");
            cases.add(new Case(upgraded.createStack().copy(), false, name + " modifier removed"));
            var broken = make(item, "manyullyn", broad);
            broken.setDamage(broken.getStats().getInt(ToolStats.DURABILITY));
            h.assertTrue(broken.isBroken(), "Native broken fixture was not broken");
            cases.add(new Case(broken.createStack(), false, name + " broken"));
            var blank = new ItemStack(item);
            h.assertTrue(!ToolStack.isInitialized(blank), "Expected native uninitialized stack");
            cases.add(new Case(blank, false, name + " uninitialized"));
            controls.add(make(item, "iron", broad).createStack());
        }
        var pickaxe = make(TinkerTools.pickaxe.get(), "manyullyn", false);
        h.assertTrue(pickaxe.getStats().get(ToolStats.HARVEST_TIER) == Tiers.NETHERITE, "Pickaxe tier fixture");
        cases.add(new Case(pickaxe.createStack(), false, "native netherite-tier pickaxe"));
        var sequence = h.startSequence();
        for (var test : cases) {
            sequence.thenExecute(() -> {
                var before = test.stack().copy();
                h.assertTrue(WildBushes.appropriateAxe(test.stack()) == test.allowed(), "Native stack tier/action permission " + test.label());
                h.assertTrue(ItemStack.matches(before, test.stack()), "Permission query mutated tool " + test.label());
                player.setItemInHand(InteractionHand.MAIN_HAND, test.stack());
                level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                var bush = WildBushes.mature(Blocks.SWEET_BERRY_BUSH.defaultBlockState());
                level.setBlock(pos, bush, 2);
                h.assertTrue((bush.getDestroyProgress(player, level, pos) > 0) == test.allowed(), "Bush mining progress " + test.label());
                // TCon consumes vanilla's call and may return false even when it removed the block.
                player.gameMode.destroyBlock(pos);
                h.assertTrue(test.allowed() ? level.isEmptyBlock(pos) : level.getBlockState(pos).is(Blocks.SWEET_BERRY_BUSH), "Actual bush removal state " + test.label());
                branch(h, pos, "dynamictrees:apple_oak");
                h.assertTrue((level.getBlockState(pos).getDestroyProgress(player, level, pos) > 0) == test.allowed(), "Tree mining progress " + test.label());
                player.gameMode.destroyBlock(pos);
            }).thenIdle(2).thenExecute(() -> {
                // DT queues FutureBreak; observe real server ticks, never invoke the felling helper.
                h.assertTrue(test.allowed() ? level.isEmptyBlock(pos) : level.getBlockState(pos).getBlock() instanceof com.ferreusveritas.dynamictrees.block.branch.BranchBlock, "Actual deferred tree removal state " + test.label());
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            });
        }
        for (var iron : controls) {
            sequence.thenExecute(() -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, iron);
                branch(h, pos, "dynamictrees:oak");
                h.assertTrue(level.getBlockState(pos).getDestroyProgress(player, level, pos) > 0, "Ordinary tree progress restricted");
                player.gameMode.destroyBlock(pos);
            }).thenIdle(2).thenExecute(() -> {
                h.assertTrue(level.isEmptyBlock(pos), "Ordinary native tree felling restricted");
                level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                level.setBlock(pos, WildBushes.mature(Blocks.SWEET_BERRY_BUSH.defaultBlockState()), 2);
                var machine = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
                machine.setItemInHand(InteractionHand.MAIN_HAND, iron);
                h.assertTrue(level.getBlockState(pos).getDestroyProgress(machine, level, pos) > 0, "Native TCon machine progress restricted");
                machine.gameMode.destroyBlock(pos);
                h.assertTrue(level.isEmptyBlock(pos), "Native TCon machine removal restricted");
            });
        }
        sequence.thenSucceed();
    }
}
