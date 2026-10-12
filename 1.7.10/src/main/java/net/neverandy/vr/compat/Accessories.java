package net.neverandy.vr.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.Loader;
import net.neverandy.vr.VisualRedstone;

/** Whether a player is using the Visualizer: holding it, or wearing it in a bauble slot. */
public final class Accessories {

    private static final boolean BAUBLES = Loader.isModLoaded("Baubles");

    private Accessories() {}

    public static boolean isUsingVisualizer(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        return held != null && held.getItem() == VisualRedstone.redstoneVisualizer
            || BAUBLES && BaublesCompat.isWearingVisualizer(player);
    }
}
