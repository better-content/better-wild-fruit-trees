package com.bettercontent.betterwildfruittrees.compat;

import com.bettercontent.betterwildfruittrees.WildFruitPolicy;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.SpeciesSelection;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public final class RareResources {
    private RareResources() {}
    public static SpeciesSelection filter(SpeciesSelection selected, RandomSource random, boolean direct) {
        if (!selected.isHandled()) return selected;
        String id = selected.getSpecies().getRegistryName().toString();
        // Legacy direct selectors remain exactly native. Both pinned selector implementations return
        // decisions directly, not by invoking each other, so each new selection consumes one gate.
        if (!(direct ? WildFruitPolicy.savedRoot(id) : WildFruitPolicy.protectsSpecies(id))) return selected;
        if (random.nextInt(WildFruitPolicy.TREE_RARITY) == 0) return selected;
        if (id.equals(WildFruitPolicy.SAGUARO)) {
            String ordinary = random.nextInt(7) < 6 ? "pillar_cactus" : "pipe_cactus";
            return new SpeciesSelection(Species.REGISTRY.get(new ResourceLocation("dynamictreesplus", ordinary)));
        }
        return new SpeciesSelection();
    }
}
