package com.bettercontent.betterwildfruittrees;

import com.mojang.serialization.Codec;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(WildFruitTrees.MOD_ID)
public final class WildFruitTrees {
    public static final String MOD_ID = "better_wild_fruit_trees";
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    static { MODIFIERS.register("no_propagules", () -> PropagationLootModifier.CODEC); }
    public WildFruitTrees() {
        MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (WildFruitPolicy.blockedPlantItem(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (WildFruitPolicy.blockedPlantBlock(ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock()).toString()))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSaplingGrowth(SaplingGrowTreeEvent event) {
        if (WildFruitPolicy.blockedPlantBlock(ForgeRegistries.BLOCKS.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).toString()))
            event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent
    public void onTrade(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getTarget() instanceof AbstractVillager villager)
            villager.getOffers().removeIf(offer -> WildFruitPolicy.blockedPlantItem(ForgeRegistries.ITEMS.getKey(offer.getResult().getItem()).toString()));
    }
}
