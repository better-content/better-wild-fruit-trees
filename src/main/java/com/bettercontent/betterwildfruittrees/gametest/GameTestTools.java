package com.bettercontent.betterwildfruittrees.gametest;

import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Nonshipping native tool whose Forge tier sorts above netherite. */
@Mod.EventBusSubscriber(modid = WildFruitTrees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GameTestTools {
    public static final ResourceLocation AXE = new ResourceLocation(WildFruitTrees.MOD_ID, "test_high_tier_axe");
    private static final Tier TIER = TierSortingRegistry.registerTier(
        new ForgeTier(5, 2500, 12, 6, 20, BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(Items.DIAMOND)),
        new ResourceLocation(WildFruitTrees.MOD_ID, "test_high_tier"), java.util.List.of(Tiers.NETHERITE), java.util.List.of());
    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper -> helper.register(AXE, new AxeItem(TIER, 5, -3, new Item.Properties())));
    }
}
