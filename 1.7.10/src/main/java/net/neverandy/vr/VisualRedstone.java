package net.neverandy.vr;

import java.io.File;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.neverandy.vr.item.RedstoneVisualizer;
import net.neverandy.vr.proxy.CommonProxy;

@Mod(modid = VisualRedstone.MOD_ID, name = VisualRedstone.MOD_NAME, version = Tags.VERSION)
public class VisualRedstone {

    public static final String MOD_ID = "vr";
    public static final String MOD_NAME = "Visual Redstone";
    public static final Logger LOG = LogManager.getLogger(MOD_NAME);

    @SidedProxy(clientSide = "net.neverandy.vr.proxy.ClientProxy", serverSide = "net.neverandy.vr.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Item redstoneVisualizer;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ClientConfig.load(new File(event.getModConfigurationDirectory(), MOD_ID + "-client.cfg"));
        redstoneVisualizer = new RedstoneVisualizer();
        GameRegistry.registerItem(redstoneVisualizer, "redstoneVisualizer");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.addRecipe(
            new ItemStack(redstoneVisualizer),
            " T ",
            "ORC",
            " S ",
            'T',
            Blocks.redstone_torch,
            'O',
            Items.compass,
            'R',
            Items.redstone,
            'C',
            Items.clock,
            'S',
            Items.stick);
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("Post Initialization Complete");
    }
}
