package com.loucaskreger.inventoryhotswap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;

public class CustomHudRenderer extends InGameHud {

	public static boolean renderHotbar = true;
	public static boolean renderExp = true;

	public CustomHudRenderer(MinecraftClient client) {
		super(client);
	}

	@Override
	public void renderHotbar(float tickDelta, MatrixStack matrices) {
		if (renderHotbar)
			super.renderHotbar(tickDelta, matrices);
	}

	@Override
	public void renderExperienceBar(MatrixStack matrices, int x) {
		if (renderExp)
			super.renderExperienceBar(matrices, x);
	}

}
