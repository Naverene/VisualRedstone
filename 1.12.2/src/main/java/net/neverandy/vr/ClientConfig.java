package net.neverandy.vr;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** The client settings, in config/vr-client.cfg (also editable from the Mods screen). */
@Config(modid = VisualRedstone.MOD_ID, name = VisualRedstone.MOD_ID + "-client")
@Mod.EventBusSubscriber(modid = VisualRedstone.MOD_ID)
public class ClientConfig
{
	@Config.Comment("How many blocks around the player get signal numbers, component info and hints.")
	@Config.RangeInt(min = 1, max = 16)
	public static int range = 8;

	@Config.Comment("Width of the bright box outline. The dark border under it is twice as wide.")
	@Config.RangeDouble(min = 0.5, max = 10.0)
	public static double thickness = 3.5;

	@Config.Comment("Color of the box on the block you look at, as #RRGGBB.")
	public static String highlightColor = "#40FF40";

	@Config.Comment("Color of the boxes on the blocks wired to the one you look at, as #RRGGBB.")
	public static String pathColor = "#40C0FF";

	@Config.Comment("Color of quasi-connectivity hints on pistons, dispensers and droppers, as #RRGGBB.")
	public static String qcColor = "#FFAA00";

	@Config.Comment("Show the signal strength (0 to 15) over redstone dust and powered blocks.")
	public static boolean signalNumbers = true;

	@Config.Comment("Outline every powered block wired to the one you look at.")
	public static boolean powerPath = true;

	@Config.Comment("Show the delay of repeaters and the mode of comparators.")
	public static boolean componentInfo = true;

	@Config.Comment("Mark pistons, dispensers and droppers that are quasi-powered but have not updated yet.")
	public static boolean qcHints = true;

	/** The color as 0xRRGGBB, or the fallback when it is not a valid #RRGGBB value. */
	public static int color(String text, int fallback)
	{
		return text != null && text.matches("#[0-9a-fA-F]{6}") ? Integer.parseInt(text.substring(1), 16) : fallback;
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (VisualRedstone.MOD_ID.equals(event.getModID()))
		{
			ConfigManager.sync(VisualRedstone.MOD_ID, Config.Type.INSTANCE);
		}
	}
}
