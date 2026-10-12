package net.neverandy.vr.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.InterModComms;
import net.neverandy.vr.VisualRedstone;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

/**
 * Talks to Curios. Only loaded when Curios is installed. The Visualizer goes in the charm slot: the
 * curios:charm item tag lets it in, and {@link #registerSlot()} makes sure players have that slot.
 */
public final class CuriosCompat {

    public static final String MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static void registerSlot() {
        InterModComms.sendTo(MOD_ID, SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.CHARM.getMessageBuilder().build());
    }

    static boolean isWearingVisualizer(PlayerEntity player) {
        return CuriosApi.getCuriosHelper().findEquippedCurio(VisualRedstone.REDSTONE_VISUALIZER.get(), player).isPresent();
    }
}
