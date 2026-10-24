package com.bettercontent.betterwildfruittrees.gametest.queueproof.mixin;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Ticket.class)
public interface TicketAccess { @Accessor("type") TicketType<?> proof$type(); }
