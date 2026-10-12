package net.neverandy.vr.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import baubles.api.BaublesApi;
import net.neverandy.vr.VisualRedstone;

/** Talks to Baubles. Only loaded when Baubles is installed. */
final class BaublesCompat {

    private BaublesCompat() {}

    static boolean isWearingVisualizer(EntityPlayer player) {
        IInventory baubles = BaublesApi.getBaubles(player);
        if (baubles == null) {
            return false;
        }
        for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
            ItemStack stack = baubles.getStackInSlot(slot);
            if (stack != null && stack.getItem() == VisualRedstone.redstoneVisualizer) {
                return true;
            }
        }
        return false;
    }
}
