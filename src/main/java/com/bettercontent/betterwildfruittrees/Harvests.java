package com.bettercontent.betterwildfruittrees;

import com.ferreusveritas.dynamictrees.systems.fruit.Fruit;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class Harvests {
    private Harvests() {}
    /** Copy and split; do not mutate upstream stacks or create oversized item entities. */
    public static List<ItemStack> amplify(Fruit fruit, List<ItemStack> original) {
        return amplify(fruit.getRegistryName().toString(), fruit.getItemStack(), original);
    }
    public static List<ItemStack> amplify(String harvestId, ItemStack product, List<ItemStack> original) {
        if (!WildFruitPolicy.protectsFruit(harvestId)) return original;
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : original) {
            int remaining = stack.getCount() * (ItemStack.isSameItemSameTags(stack, product) ? WildFruitPolicy.HARVEST_MULTIPLIER : 1);
            while (remaining > 0) {
                int count = Math.min(remaining, stack.getMaxStackSize());
                result.add(stack.copyWithCount(count));
                remaining -= count;
            }
        }
        return result;
    }
}
