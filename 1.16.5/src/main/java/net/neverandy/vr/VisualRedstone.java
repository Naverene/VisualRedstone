package net.neverandy.vr;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.neverandy.vr.compat.CuriosCompat;

@Mod(VisualRedstone.MOD_ID)
public class VisualRedstone {

    public static final String MOD_ID = "vr";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> REDSTONE_VISUALIZER = ITEMS.register("redstone_visualizer",
        () -> new Item(new Item.Properties().maxStackSize(1).group(ItemGroup.REDSTONE)));

    public VisualRedstone() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(this::sendIMC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private void sendIMC(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded(CuriosCompat.MOD_ID)) {
            CuriosCompat.registerSlot();
        }
    }
}
