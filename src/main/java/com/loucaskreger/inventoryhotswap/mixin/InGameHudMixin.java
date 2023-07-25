package com.loucaskreger.inventoryhotswap.mixin;


import com.loucaskreger.inventoryhotswap.CustomHudRenderer;
import com.loucaskreger.inventoryhotswap.InventoryHotswap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
        if (!CustomHudRenderer.renderHotbar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void onExpBarRender(DrawContext context, int x, CallbackInfo ci) {
        if (!CustomHudRenderer.renderExp) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void renderHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        if (!InventoryHotswap.heldItemTooltips) {
            ci.cancel();
        }
    }

}
