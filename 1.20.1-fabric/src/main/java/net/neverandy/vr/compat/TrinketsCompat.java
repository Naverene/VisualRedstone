package net.neverandy.vr.compat;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;
import net.neverandy.vr.VisualRedstone;

/**
 * Talks to Trinkets. Only loaded when Trinkets is installed. The Visualizer goes in the necklace slot:
 * the trinkets:chest/necklace item tag lets it in, and data/trinkets/entities gives players that slot.
 */
final class TrinketsCompat {

    private TrinketsCompat() {
    }

    static boolean isWearingVisualizer(Player player) {
        return TrinketsApi.getTrinketComponent(player)
            .map(trinkets -> trinkets.isEquipped(VisualRedstone.REDSTONE_VISUALIZER))
            .orElse(false);
    }
}
