package com.loucaskreger.ezinventory.config;

import com.loucaskreger.ezinventory.EZInventory;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;

public class ClientConfig {

	public static EnumValue<EZInventory.GuiRenderType> guiRenderType;

	public ClientConfig(ForgeConfigSpec.Builder builder) {
		guiRenderType = builder.comment(
				"PUSHED will push up the players survival gui(hearts, xp bar, hunger, armor, air) above the vertical selection slots.\n"
						+ "If set to INVISIBLE, then the survival gui will not be rendered when keybind it pressed.\n"
						+ "If set to OVERLAY, then the survival gui will not be moved and the vertical selection bar will render over it")
				.defineEnum("guiRenderType", EZInventory.GuiRenderType.PUSHED);
	}

}
