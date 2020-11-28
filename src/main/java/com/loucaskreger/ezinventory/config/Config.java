package com.loucaskreger.ezinventory.config;

import org.apache.commons.lang3.tuple.Pair;

import com.loucaskreger.ezinventory.EZInventory;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

	public static final ClientConfig CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;

	public static EZInventory.GuiRenderType guiRenderType;

	static {
		final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
		CLIENT_SPEC = specPair.getRight();
		CLIENT = specPair.getLeft();
	}

	public static void bakeConfig() {
		guiRenderType = ClientConfig.guiRenderType.get();
	}
}
