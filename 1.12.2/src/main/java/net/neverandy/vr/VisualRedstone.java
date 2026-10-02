package net.neverandy.vr;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = VisualRedstone.MOD_ID, name = VisualRedstone.MOD_NAME, version = VisualRedstone.MOD_VERSION, acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber(modid = VisualRedstone.MOD_ID)
public class VisualRedstone
{
	public static final String MOD_ID = "vr";
	public static final String MOD_NAME = "Visual Redstone";
	public static final String MOD_VERSION = "@VERSION@";

	@GameRegistry.ObjectHolder(MOD_ID + ":redstone_visualizer")
	public static Item redstoneVisualizer;

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event)
	{
		event.getRegistry().register(new Item()
				.setMaxStackSize(1)
				.setCreativeTab(CreativeTabs.REDSTONE)
				.setUnlocalizedName(MOD_ID + ".redstone_visualizer")
				.setRegistryName(MOD_ID, "redstone_visualizer"));
	}
}
