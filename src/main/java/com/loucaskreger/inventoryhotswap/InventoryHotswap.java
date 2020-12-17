package com.loucaskreger.inventoryhotswap;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.loucaskreger.inventoryhotswap.config.Config;

@Mod(InventoryHotswap.MOD_ID)
public class InventoryHotswap {

	public static final String MOD_ID = "inventoryhotswap";
	public static final Logger LOGGER = LogManager.getLogger();

	public InventoryHotswap() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		// Use a sided proxy instead of this
		// {https://forums.minecraftforge.net/topic/63802-how-to-add-compatibility-with-other-mods/}
		if (ModList.get().isLoaded("armorhotswap"))
			LOGGER.debug("Armor Hotswap is loaded alongside inventoryHotswap");
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
