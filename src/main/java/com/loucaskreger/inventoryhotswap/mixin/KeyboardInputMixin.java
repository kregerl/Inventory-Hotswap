package com.loucaskreger.inventoryhotswap.mixin;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.loucaskreger.inventoryhotswap.InventoryHotswap;
import com.loucaskreger.inventoryhotswap.config.Config;

import net.minecraft.client.Keyboard;

@Mixin(Keyboard.class)
public class KeyboardInputMixin {

	@Inject(method = "onKey", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/client/Keyboard;debugCrashStartTime:J", ordinal = 0))
	private void onKeyboardInput(long windowPointer, int key, int scanCode, int action, int modifiers,
			CallbackInfo ci) {
		if (InventoryHotswap.verticalScroll.isPressed()) {
			int inverted = Config.INSTANCE.inverted ? -1 : 1;
			switch (key) {
			case GLFW_KEY_1:
				InventoryHotswap.accumulatedScrollDelta = inverted * 1;
				InventoryHotswap.verticalScroll.setPressed(false);
				ci.cancel();
//				InventoryHotswap.moveToCorrectSlot = true;
				break;
			case GLFW_KEY_2:
				InventoryHotswap.accumulatedScrollDelta = inverted * 2;
				InventoryHotswap.verticalScroll.setPressed(false);
				ci.cancel();
//				InventoryHotswap.moveToCorrectSlot = true;
				break;
			case GLFW_KEY_3:
				InventoryHotswap.accumulatedScrollDelta = inverted * 3;
				InventoryHotswap.verticalScroll.setPressed(false);
				ci.cancel();
//				InventoryHotswap.moveToCorrectSlot = true;
				break;
			}

		}
	}

}
