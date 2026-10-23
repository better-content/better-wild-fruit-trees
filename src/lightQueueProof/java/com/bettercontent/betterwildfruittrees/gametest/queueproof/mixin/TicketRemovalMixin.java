package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import com.bettercontent.betterwildfruittrees.gametest.queueproof.QueueProofLedger;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(DistanceManager.class)
public abstract class TicketRemovalMixin {
 @Redirect(method="removeTicket(JLnet/minecraft/server/level/Ticket;)V",at=@At(value="INVOKE",target="Lnet/minecraft/util/SortedArraySet;remove(Ljava/lang/Object;)Z"),require=2,expect=2,allow=2)
 private boolean proof$removed(SortedArraySet<?> set,Object original,long key,Ticket<?> ticket) throws Throwable {boolean removed;try{removed=set.remove(original);}catch(Throwable failure){QueueProofLedger.originalThrowable(5,System.identityHashCode(failure),System.identityHashCode(failure.getClass()));throw failure;}if(QueueProofLedger.active())QueueProofLedger.ticket(key,removed,((TicketAccess)(Object)ticket).proof$type()==TicketType.LIGHT);return removed;}
}
