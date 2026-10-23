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
        com.bettercontent.betterwildfruittrees.diagnostics.RuntimeDiagnostics.initialize();
        com.bettercontent.betterwildfruittrees.compat.LedgerNetwork.initialize();
        MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        WildBushField.FEATURES.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent.RightClickBlock event) {
        String held = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString();
        if (java.util.Set.of("minecraft:sweet_berries", "fruitsdelight:blueberry", "fruitsdelight:cranberry", "ars_nouveau:sourceberry_bush", "ars_nouveau:sourceberry").contains(held)
            && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        // Dual-purpose foods remain edible/usable in cooking. Only their pot interaction is rejected here;
        // native seed/species and block-placement hooks reject their actual planting routes.
        if (WildFruitPolicy.blockedPlantItem(held) || WildFruitPolicy.ediblePropagule(held)
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (WildBushes.isBush(event.getPlacedBlock()) || WildFruitPolicy.blockedPlantBlock(ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock()).toString()))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSaplingGrowth(SaplingGrowTreeEvent event) {
        if (WildFruitPolicy.blockedPlantBlock(ForgeRegistries.BLOCKS.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).toString()))
            event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (WildBushes.restrictPlayer(event.getPlayer()) && WildBushes.protectedResource(event.getState(), event.getLevel(), event.getPos()) &&
            !WildBushes.appropriateAxe(event.getPlayer().getMainHandItem())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBushGrowth(net.minecraftforge.event.level.BlockEvent.CropGrowEvent.Pre event) {
        if (event.getResult() != Event.Result.DEFAULT || !WildBushes.isBush(event.getState())) return;
        String id = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()).toString();
        boolean grow = event.getLevel().getRandom().nextDouble() < ProductionRates.bushGrowthChance(id);
        event.setResult(grow ? Event.Result.ALLOW : Event.Result.DENY);
    }
    @SubscribeEvent
    public void onChunkLoad(net.minecraftforge.event.level.ChunkEvent.Load event) {
        // Also cover reattaching an existing chunk instance without deserializing its roots again.
        if (!event.getLevel().isClientSide() && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)
            chunk.getBlockEntities().values().forEach(TreeFruitBudget::resume);
    }
    @SubscribeEvent
    public void onTrade(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getTarget() instanceof AbstractVillager villager)
            villager.getOffers().removeIf(offer -> WildFruitPolicy.blockedPlantItem(ForgeRegistries.ITEMS.getKey(offer.getResult().getItem()).toString()));
    }
}
