package com.loucaskreger.inventoryhotswap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.loucaskreger.inventoryhotswap.InventoryHotswap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public class MouseInputMixin {

	@Inject(method = "onMouseScroll", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 0))
	private void hookOnMouseScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen == null) {
			double amount = yoffset * client.options.getMouseWheelSensitivity().getValue();

			if (InventoryHotswap.onMouseScroll(amount)) {
				ci.cancel();
			}
		}
	}

}
