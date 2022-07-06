package com.loucaskreger.inventoryhotswap.config;

public class ConfigInstance {

    public HudTypes type;
    public boolean inverted;
    public boolean sneakToSwapRows;

    public ConfigInstance() {
        type = HudTypes.PUSHED;
        inverted = false;
        sneakToSwapRows = true;
    }

}
