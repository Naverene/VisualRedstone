package net.neverandy.vr;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(VisualRedstone.MOD_ID)
public class VisualRedstone {

    public static final String MOD_ID = "vr";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> REDSTONE_VISUALIZER = ITEMS.register("redstone_visualizer",
        () -> new Item(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_REDSTONE)));

    public VisualRedstone() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
