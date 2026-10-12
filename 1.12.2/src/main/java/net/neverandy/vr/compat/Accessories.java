package net.neverandy.vr.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;
import net.neverandy.vr.VisualRedstone;

/** Whether a player is using the Visualizer: holding it in either hand, or wearing it in a bauble slot. */
public final class Accessories
{
	private static final boolean BAUBLES = Loader.isModLoaded("baubles");

	private Accessories()
	{
	}

	public static boolean isUsingVisualizer(EntityPlayer player)
	{
		return player.getHeldItemMainhand().getItem() == VisualRedstone.redstoneVisualizer
				|| player.getHeldItemOffhand().getItem() == VisualRedstone.redstoneVisualizer
				|| BAUBLES && BaublesCompat.isWearingVisualizer(player);
	}
}
