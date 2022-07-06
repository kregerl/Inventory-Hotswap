package com.loucaskreger.inventoryhotswap.mixin;


import com.loucaskreger.inventoryhotswap.CustomHudRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
        if (!CustomHudRenderer.renderHotbar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void onExpBarRender(MatrixStack matrices, int x, CallbackInfo ci) {
        if (!CustomHudRenderer.renderExp) {
            ci.cancel();
        }
    }


}
