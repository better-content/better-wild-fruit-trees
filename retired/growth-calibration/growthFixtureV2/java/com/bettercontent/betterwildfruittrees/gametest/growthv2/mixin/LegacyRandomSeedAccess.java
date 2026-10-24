package com.bettercontent.betterwildfruittrees.gametest.growthv2.mixin;

import com.bettercontent.betterwildfruittrees.gametest.GrowthFixtureV2;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.atomic.AtomicLong;

/** Native-fixture-only read access. Never sets, restores, replaces or consumes random state. */
@Mixin(LegacyRandomSource.class)
public interface LegacyRandomSeedAccess extends GrowthFixtureV2.RandomStateAccess {
    @Accessor("seed") AtomicLong growthV2$seed();
}
