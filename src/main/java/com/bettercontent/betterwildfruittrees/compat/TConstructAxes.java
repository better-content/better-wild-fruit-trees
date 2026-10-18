package com.bettercontent.betterwildfruittrees.compat;

import com.bettercontent.betterwildfruittrees.WildBushes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/** Loaded only after the optional pinned TConstruct dependency is present. */
public final class TConstructAxes {
    private TConstructAxes() {}
    public static boolean handles(ItemStack stack) {
        return stack.getItem() instanceof IModifiable;
    }
    public static boolean appropriateAxe(ItemStack stack) {
        // Do not initialize/rebuild a malformed or blank tool during a mining permission query.
        if (stack.getCount() != 1 || !ToolStack.isInitialized(stack)) return false;
        var tool = ToolStack.copyFrom(stack);
        var stats = tool.getStats();
        return !tool.isBroken() && stats.hasStat(ToolStats.HARVEST_TIER)
            && stack.canPerformAction(ToolActions.AXE_DIG)
            && WildBushes.atLeastDiamond(stats.get(ToolStats.HARVEST_TIER));
    }
}
