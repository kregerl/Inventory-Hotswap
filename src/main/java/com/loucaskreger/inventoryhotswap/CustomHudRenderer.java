package com.loucaskreger.inventoryhotswap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;

public class CustomHudRenderer extends InGameHud {

    public static boolean renderHotbar = true;
    public static boolean renderExp = true;

    public CustomHudRenderer(MinecraftClient client) {
        super(client, client.getItemRenderer());
    }
}
