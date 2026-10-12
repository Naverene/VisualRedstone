package net.neverandy.vr.compat;

import net.minecraft.world.entity.player.Player;
import net.neverandy.vr.VisualRedstone;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Talks to Curios. Only loaded when Curios is installed. The Visualizer goes in the charm slot: the
 * curios:charm item tag lets it in, and data/vr/curios/entities gives players that slot.
 */
final class CuriosCompat {

    private CuriosCompat() {
    }

    static boolean isWearingVisualizer(Player player) {
        return CuriosApi.getCuriosInventory(player)
            .map(curios -> curios.isEquipped(VisualRedstone.REDSTONE_VISUALIZER.get()))
            .orElse(false);
    }
}
