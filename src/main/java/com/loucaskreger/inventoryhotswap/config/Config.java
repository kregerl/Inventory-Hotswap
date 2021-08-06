package com.loucaskreger.inventoryhotswap.config;

import com.loucaskreger.inventoryhotswap.client.GuiRenderType;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static GuiRenderType guiRenderType;
    public static boolean inverted;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static void bakeConfig() {
        guiRenderType = ClientConfig.guiRenderType.get();
        inverted = ClientConfig.inverted.get();
    }
}
