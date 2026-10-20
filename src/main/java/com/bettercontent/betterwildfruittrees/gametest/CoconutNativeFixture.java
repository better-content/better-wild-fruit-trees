package com.bettercontent.betterwildfruittrees.gametest;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import xueluoanping.dtnatures_spirit.systems.pods.IFallingFruit;

/** Isolate optional upstream types from the absent-addon GameTest class-loading path. */
final class CoconutNativeFixture {
    static ItemStack fallingDrop(Block block, FallingBlockEntity entity) {
        return ((IFallingFruit)block).getDropOnFallItems(entity);
    }
}
