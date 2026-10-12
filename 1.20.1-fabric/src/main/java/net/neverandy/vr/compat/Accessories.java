package net.neverandy.vr.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.neverandy.vr.VisualRedstone;

/** Whether a player is using the Visualizer: holding it in either hand, or wearing it in a Trinkets slot. */
public final class Accessories {

    private static final boolean TRINKETS = FabricLoader.getInstance().isModLoaded("trinkets");

    private Accessories() {
    }

    public static boolean isUsingVisualizer(Player player) {
        return player.getMainHandItem().is(VisualRedstone.REDSTONE_VISUALIZER)
            || player.getOffhandItem().is(VisualRedstone.REDSTONE_VISUALIZER)
            || TRINKETS && TrinketsCompat.isWearingVisualizer(player);
    }
}
