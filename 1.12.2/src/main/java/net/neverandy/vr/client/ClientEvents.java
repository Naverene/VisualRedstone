package net.neverandy.vr.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.neverandy.vr.VisualRedstone;

@Mod.EventBusSubscriber(modid = VisualRedstone.MOD_ID, value = Side.CLIENT)
public class ClientEvents
{
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event)
	{
		ModelLoader.setCustomModelResourceLocation(VisualRedstone.redstoneVisualizer, 0,
				new ModelResourceLocation(VisualRedstone.redstoneVisualizer.getRegistryName(), "inventory"));
	}
}
