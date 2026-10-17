package com.bettercontent.betterwildfruittrees;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

public final class PropagationLootModifier extends LootModifier {
    public static final Codec<PropagationLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, PropagationLootModifier::new));
    public PropagationLootModifier(LootItemCondition[] conditions) { super(conditions); }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        loot.removeIf(stack -> WildFruitPolicy.blockedPlantItem(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()));
        return loot;
    }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
