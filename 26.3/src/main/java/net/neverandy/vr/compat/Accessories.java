package net.neverandy.vr.compat;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neverandy.vr.VisualRedstone;

/** Whether a player is using the Visualizer: holding it in either hand, or wearing it in a Curios slot. */
public final class Accessories {

    private static final boolean CURIOS = ModList.get().isLoaded("curios");

    private Accessories() {
    }

    public static boolean isUsingVisualizer(Player player) {
        return player.getMainHandItem().is(VisualRedstone.REDSTONE_VISUALIZER.get())
            || player.getOffhandItem().is(VisualRedstone.REDSTONE_VISUALIZER.get())
            || CURIOS && CuriosCompat.isWearingVisualizer(player);
    }
}
