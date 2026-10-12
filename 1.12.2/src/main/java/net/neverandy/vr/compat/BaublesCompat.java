package net.neverandy.vr.compat;

import baubles.api.BaublesApi;
import net.minecraft.entity.player.EntityPlayer;
import net.neverandy.vr.VisualRedstone;

/** Talks to Baubles. Only loaded when Baubles is installed. */
final class BaublesCompat
{
	private BaublesCompat()
	{
	}

	static boolean isWearingVisualizer(EntityPlayer player)
	{
		return BaublesApi.isBaubleEquipped(player, VisualRedstone.redstoneVisualizer) >= 0;
	}
}
