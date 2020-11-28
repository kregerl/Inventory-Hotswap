package com.loucaskreger.ezinventory;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.loucaskreger.ezinventory.config.Config;

@Mod(EZInventory.MOD_ID)
public class EZInventory {

	public static final String MOD_ID = "ezinventory";
	public static final Logger LOGGER = LogManager.getLogger();

	public EZInventory() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
	}

	private void clientSetup(final FMLClientSetupEvent event) {
		ClientRegistry.registerKeyBinding(EventSubscriber.vertScroll);
	}

	public enum GuiRenderType {
		INVISIBLE(), PUSHED(), OVERLAY();
	}
}
