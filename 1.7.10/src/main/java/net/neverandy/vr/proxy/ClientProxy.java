package net.neverandy.vr.proxy;

import net.minecraftforge.common.MinecraftForge;

import net.neverandy.vr.client.HighlightRenderer;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(new HighlightRenderer());
    }
}
