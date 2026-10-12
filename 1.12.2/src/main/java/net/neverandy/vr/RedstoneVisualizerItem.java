package net.neverandy.vr;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

/**
 * The Redstone Visualizer. When Baubles is installed it also fits a charm slot, and wearing it there
 * works the same as holding it. Without Baubles the interface is stripped and it is a plain item.
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class RedstoneVisualizerItem extends Item implements IBauble
{
	@Override
	@Optional.Method(modid = "baubles")
	public BaubleType getBaubleType(ItemStack stack)
	{
		return BaubleType.CHARM;
	}
}
