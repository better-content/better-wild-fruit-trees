package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import com.bettercontent.betterwildfruittrees.WildFruitTrees;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Retain incomplete evidence if any required test aborts before all six observations. */
@Mod.EventBusSubscriber(modid=WildFruitTrees.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class QueueProofFailureFlush {
 @SubscribeEvent public static void stopping(ServerStoppingEvent event){try{QueueProofLedger.flush(true);}finally{PreparationAudit.flush();}}
}
