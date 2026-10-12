package net.neverandy.vr.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import cpw.mods.fml.common.Optional;
import net.neverandy.vr.VisualRedstone;

/**
 * Held in the hand, it highlights the block you look at when that block carries a redstone signal. When Baubles is
 * installed it also fits an amulet slot, and wearing it there works the same as holding it.
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "Baubles")
public class RedstoneVisualizer extends Item implements IBauble {

    public RedstoneVisualizer() {
        setMaxStackSize(1);
        setUnlocalizedName(VisualRedstone.MOD_ID + ".redstoneVisualizer");
        setTextureName(VisualRedstone.MOD_ID + ":redstone_visualizer");
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    @Override
    @Optional.Method(modid = "Baubles")
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.AMULET;
    }

    @Override
    @Optional.Method(modid = "Baubles")
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    @Optional.Method(modid = "Baubles")
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    @Optional.Method(modid = "Baubles")
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    @Optional.Method(modid = "Baubles")
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    @Optional.Method(modid = "Baubles")
    public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }
}
