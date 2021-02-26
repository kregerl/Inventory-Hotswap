package com.loucaskreger.inventoryhotswap.config;

import com.loucaskreger.inventoryhotswap.InventoryHotswap;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;

public class ClientConfig {

	public static EnumValue<InventoryHotswap.GuiRenderType> guiRenderType;
	public static BooleanValue inverted;
	public static BooleanValue sneakSwapsEntireRows;

	public ClientConfig(ForgeConfigSpec.Builder builder) {
		guiRenderType = builder.comment(
				"PUSHED will push up the players survival gui(hearts, xp bar, hunger, armor, air) above the vertical selection slots.\n"
						+ "If set to INVISIBLE, then the survival gui will not be rendered when keybind it pressed.\n"
						+ "If set to OVERLAY, then the survival gui will not be moved and the vertical selection bar will render over it")
				.defineEnum("guiRenderType", InventoryHotswap.GuiRenderType.PUSHED);
		inverted = builder.comment("Set to true if you want the quick selection key presses to be inverted.").define("invertQuickSelection", false);
		sneakSwapsEntireRows = builder
				.comment("(Default: true) Turning this to false will disable the sneak to swap entire bar feature.")
				.define("sneakSwapsEntireRows", true);
	}

}
