package com.loucaskreger.inventoryhotswap.mixin;

import com.loucaskreger.inventoryhotswap.InventoryHotswap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.JumpingMount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MoveHudMixin {

	@Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
	private void moveStatusBars(DrawContext context, CallbackInfo ci) {
		if (!InventoryHotswap.renderStatusBars) {
			ci.cancel();
		} else if (InventoryHotswap.renderPushedStatusBars) {
			MatrixStack matrices = context.getMatrices();
			matrices.translate(0, -InventoryHotswap.HEIGHT, 0);
		}
	}

	@Inject(method = "renderStatusBars", at = @At("TAIL"))
	private void fixStatusBar(DrawContext context, CallbackInfo ci) {
		if (InventoryHotswap.renderPushedStatusBars) {
			MatrixStack matrices = context.getMatrices();
			matrices.translate(0, InventoryHotswap.HEIGHT, 0);
		}
	}

	@Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
	private void shiftExpBar(DrawContext context, CallbackInfo ci) {
		if (!InventoryHotswap.renderMountInfo) {
			ci.cancel();
		}

	}

	@Inject(method = "renderMountJumpBar", at = @At("HEAD"), cancellable = true)
	private void fixExpBar(JumpingMount mount, DrawContext context, int x, CallbackInfo ci) {
		if (!InventoryHotswap.renderMountInfo) {
			ci.cancel();
		}
	}

}
