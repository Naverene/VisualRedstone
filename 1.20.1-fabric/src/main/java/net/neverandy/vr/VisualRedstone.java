package net.neverandy.vr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class VisualRedstone implements ModInitializer {

    public static final String MOD_ID = "vr";

    public static final Item REDSTONE_VISUALIZER = new Item(new Item.Properties().stacksTo(1));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "redstone_visualizer"), REDSTONE_VISUALIZER);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> entries.accept(REDSTONE_VISUALIZER));
    }
}
