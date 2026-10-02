package net.neverandy.vr.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import net.neverandy.vr.VisualRedstone;

/** Held in the hand, it highlights the block you look at when that block carries a redstone signal. */
public class RedstoneVisualizer extends Item {

    public RedstoneVisualizer() {
        setMaxStackSize(1);
        setUnlocalizedName(VisualRedstone.MOD_ID + ".redstoneVisualizer");
        setTextureName(VisualRedstone.MOD_ID + ":redstone_visualizer");
        setCreativeTab(CreativeTabs.tabRedstone);
    }
}
