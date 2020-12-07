package com.loucaskreger.inventoryhotswap.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.loucaskreger.inventoryhotswap.CustomHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;

@Mixin(MinecraftClient.class)
public class HideHudMixin {

	@Shadow
	private static MinecraftClient instance;

	@Shadow
	@Final
	private InGameHud inGameHud;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onHudRender(final CallbackInfo ci) {
		inGameHud = new CustomHudRenderer(instance);
	}
	

}
