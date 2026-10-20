package com.bettercontent.betterwildfruittrees;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;

/** Explicit bush inventory: foods are not propagule loot and must remain edible/craftable. */
public final class WildBushes {
    public static final Set<String> BLOCKS = Set.of("minecraft:sweet_berry_bush",
        "fruitsdelight:blueberry_bush", "fruitsdelight:cranberry_bush", "ars_nouveau:sourceberry_bush");
    private static final Set<net.minecraft.resources.ResourceLocation> IDS = BLOCKS.stream()
        .map(net.minecraft.resources.ResourceLocation::new).collect(java.util.stream.Collectors.toUnmodifiableSet());
    // Mining-only: never include lemon in the berry geometry/planting/production selectors.
    private static final net.minecraft.resources.ResourceLocation LEMON_TREE =
        new net.minecraft.resources.ResourceLocation("fruitsdelight", "lemon_tree");
    private WildBushes() {}
    public static boolean isBush(BlockState state) {
        return IDS.contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }
    public static BlockState mature(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age"))
                return state.setValue(age, age.getPossibleValues().stream().max(Integer::compareTo).orElseThrow());
        }
        return state;
    }
    public static boolean protectedResource(BlockState state, BlockGetter level, BlockPos pos) {
        return isBush(state) || LEMON_TREE.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()))
            || LoadedTreeLookup.isProtected(level, pos);
    }
    public static boolean restrictPlayer(net.minecraft.world.entity.player.Player player) {
        return !(player instanceof net.minecraftforge.common.util.FakePlayer);
    }
    public static boolean appropriateAxe(ItemStack tool) {
        // TCon extends TieredItem, but its superclass tier is a dummy: the actual tier is per-stack.
        if (net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")
            && com.bettercontent.betterwildfruittrees.compat.TConstructAxes.handles(tool))
            return com.bettercontent.betterwildfruittrees.compat.TConstructAxes.appropriateAxe(tool);
        if (!tool.canPerformAction(net.minecraftforge.common.ToolActions.AXE_DIG) || !(tool.getItem() instanceof TieredItem tiered)) return false;
        return atLeastDiamond(tiered.getTier());
    }
    public static boolean atLeastDiamond(net.minecraft.world.item.Tier tier) {
        return tier != null && (tier == Tiers.DIAMOND || TierSortingRegistry.getTiersLowerThan(tier).contains(Tiers.DIAMOND));
    }
}
