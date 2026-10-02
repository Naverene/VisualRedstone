package net.neverandy.vr;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(VisualRedstone.MOD_ID)
public class VisualRedstone {

    public static final String MOD_ID = "vr";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<Item> REDSTONE_VISUALIZER = ITEMS.registerSimpleItem("redstone_visualizer",
        p -> p.stacksTo(1));

    public VisualRedstone(IEventBus modBus, ModContainer container) {
        ITEMS.register(modBus);
        modBus.addListener(VisualRedstone::addToCreativeTab);
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(REDSTONE_VISUALIZER);
        }
    }
}
