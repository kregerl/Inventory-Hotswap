package com.loucaskreger.inventoryhotswap;

import com.loucaskreger.inventoryhotswap.config.Config;
import com.loucaskreger.inventoryhotswap.config.HudTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

public class InventoryHotswap implements ModInitializer {

    public static final String MOD_ID = "inventoryhotswap";

    private static final int[] slotsScrollDown = {0, 9, 18, 27};
    private static final int[] slotsScrollUp = {0, 27, 18, 9};
    private static final int[] selectedScrollPositions = {65, 43, 21, -1};

    public static int accumulatedScrollDelta = 0;
    private static int remainingHighlightTicks = 0;
    private static int textOffset = 0;
    private static int currentIndex;

    public static boolean moveToCorrectSlot = false;
    public static boolean renderStatusBars = true;
    public static boolean renderPushedStatusBars = false;
    public static boolean renderMountInfo = true;
    public static boolean heldItemTooltips = true;
    private static boolean renderCustomExpBar = false;
    private static boolean renderEntireBar = false;
    private static boolean wasKeyDown = false;
    private static boolean renderCustomMountInfo = false;


    private static final int WIDTH = 22;
    public static final int HEIGHT = 66;

    private static MinecraftClient mc = MinecraftClient.getInstance();

    private static ItemStack highlightingItemStack = ItemStack.EMPTY;

    private static final Identifier VERT_TEXTURE = new Identifier(MOD_ID, "textures/gui/verticalbar.png");
    private static final Identifier TEXTURE = new Identifier(InventoryHotswap.MOD_ID,
            "textures/gui/largebarselection.png");
    public static final Identifier WIDGETS_TEXTURE_PATH = new Identifier("textures/gui/widgets.png");

    public static final KeyBinding verticalScroll = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            MOD_ID + ".key.verticalscroll", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, MOD_ID + ".key.categories"));

    private static final KeyBinding fullBarVerticalScroll = KeyBindingHelper.registerKeyBinding(new KeyBinding(MOD_ID + ".key.fullBarVerticalScroll", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z,
            MOD_ID + ".key.categories"));
    
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");

    @Override
    public void onInitialize() {
        Config.init();
        onClientTick();
        onHudRender();
    }

    public static boolean onMouseScroll(double scrollDelta) {
        if (wasKeyDown) {
            accumulatedScrollDelta += scrollDelta;
            accumulatedScrollDelta %= 4;
            return true;
        }
        return false;

    }

    public static void onHudRender() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            DefaultedList<ItemStack> inventory = mc.player.getInventory().main;
            int currentIndex = mc.player.getInventory().selectedSlot;
            if (wasKeyDown) {
                heldItemTooltips = false;

                int scaledWidth = mc.getWindow().getScaledWidth();
                int scaledHeight = mc.getWindow().getScaledHeight();

                int width = (scaledWidth / 2);

                MatrixStack matrixStack = drawContext.getMatrices();

                ItemRenderer itemRenderer = mc.getItemRenderer();
                TextRenderer fontRenderer = mc.textRenderer;
                if (renderEntireBar) {

                    matrixStack.push();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();

                    // Replace textureManager calls with the below call.
                    for (int i = 0; i < 4; i++) {
                        drawContext.drawTexture(WIDGETS_TEXTURE_PATH, width - 91, scaledHeight - WIDTH - (i * 22), 0, 0, 182, 22);
                    }

                    RenderSystem.disableBlend();
                    matrixStack.pop();
                    for (int k = 3; k > 0; k--) {
                        int l = Config.INSTANCE.inverted ? Math.abs(k - 3) + 1 : k;
                        drawContext.drawText(fontRenderer, String.valueOf(k), width - 98, scaledHeight - 13 - (l * 22),
                                0xFFFFFF, false);
                    }

                    for (int i1 = 0; i1 < 9; ++i1) {
                        // Render all item sprites in the multi bar display
                        int j1 = width - 90 + i1 * 20 + 2;
                        int k1 = scaledHeight - 16 - 3;
                        renderHotbarItem(drawContext, j1, k1, tickDelta, mc.player, inventory.get(i1), itemRenderer, fontRenderer);
                        renderHotbarItem(drawContext, j1, k1 - 22, tickDelta, mc.player, inventory.get(i1 + 27), itemRenderer,
                                fontRenderer);
                        renderHotbarItem(drawContext, j1, k1 - 44, tickDelta, mc.player, inventory.get(i1 + 18), itemRenderer,
                                fontRenderer);
                        renderHotbarItem(drawContext, j1, k1 - 66, tickDelta, mc.player, inventory.get(i1 + 9), itemRenderer,
                                fontRenderer);

                    }
                    matrixStack.push();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();

                    drawContext.drawTexture(TEXTURE, width - 92, scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0, 0, 184,
                            24);

                    renderMountHealth(drawContext);
                    if (mc.player.getJumpingMount() != null) {
                        renderMountJumpBar(drawContext, mc.getWindow().getScaledWidth() / 2 - 91);
                    } else {
                        renderExperienceBar(drawContext, mc.getWindow().getScaledWidth() / 2 - 91);
                    }
                    RenderSystem.disableBlend();
                    matrixStack.pop();
                } else {

                    matrixStack.push();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();

                    drawContext.drawTexture(WIDGETS_TEXTURE_PATH, width - 91, scaledHeight - WIDTH, 0, 0, 182, 22);

                    RenderSystem.disableBlend();
                    matrixStack.pop();
                    // Render items in re-rendered hotbar
                    for (int i1 = 0; i1 < 9; ++i1) {
                        int j1 = width - 90 + i1 * 20 + 2;
                        int k1 = scaledHeight - 16 - 3;
                        renderHotbarItem(drawContext, j1, k1, tickDelta, mc.player, inventory.get(i1), itemRenderer, fontRenderer);
                    }

                    matrixStack.push();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();

                    // Render the verticalbar
                    drawContext.drawTexture(VERT_TEXTURE, width - 91 + (currentIndex * (WIDTH - 2)),
                            scaledHeight - WIDTH - HEIGHT, 0, 0, WIDTH, HEIGHT);

                    for (int k = 3; k > 0; k--) {
                        int l = Config.INSTANCE.inverted ? Math.abs(k - 3) + 1 : k;
                        drawContext.drawText(fontRenderer, String.valueOf(k), width - 98 + (currentIndex * (WIDTH - 2)), scaledHeight - 13 - (l * 22),
                                0xFFFFFF, false);
                    }

                    // Render items in the verticalbar
                    for (int i = 27, j = 22; i > 0; i -= 9, j += 22) {
                        int j1 = width - 88 + (currentIndex * (WIDTH - 2));
                        int k1 = scaledHeight - 16 - 3;
                        ItemStack stack = inventory.get(currentIndex + i);

                        renderHotbarItem(drawContext, j1, k1 - j, tickDelta, mc.player, stack, itemRenderer, fontRenderer);
                    }

                    // Render the selection square
                    drawContext.drawTexture(WIDGETS_TEXTURE_PATH, width - 92 + (currentIndex * (WIDTH - 2)),
                            scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0, 22, 24, 24);

                    renderHeldItemTooltip(drawContext, scaledWidth, scaledHeight, fontRenderer);

                    // Reset the icon texture to stop hearts and hunger from being screwed up.
                    RenderSystem.setShaderTexture(0, ICONS);

                    renderMountHealth(drawContext);
                    if (mc.player.getJumpingMount() != null) {
                        renderMountJumpBar(drawContext, mc.getWindow().getScaledWidth() / 2 - 91);
                    } else {
                        renderExperienceBar(drawContext, mc.getWindow().getScaledWidth() / 2 - 91);
                    }
                    RenderSystem.disableBlend();
                    matrixStack.pop();

                }
            }
        });
    }

    /**
     * Determines the height for the selection box
     *
     * @return height offset of selection box
     */
    private static int scrollFunc() {
        if (Math.signum(accumulatedScrollDelta) == 0) {
            return selectedScrollPositions[0];
        } else if (Math.signum(accumulatedScrollDelta) > 0) {
            return selectedScrollPositions[accumulatedScrollDelta];
        }
        return selectedScrollPositions[Math.abs(Math.abs(accumulatedScrollDelta) - selectedScrollPositions.length)];
    }

    public static void renderMountJumpBar(DrawContext drawContext, int x) {
        if (renderCustomMountInfo) {
            mc.getProfiler().push("jumpBar");
            RenderSystem.setShaderTexture(0, ICONS);
            float f = mc.player.getMountJumpStrength();
            int j = (int) (f * 183.0F);
            int k = /* mc.getWindow().getScaledHeight() - 32 + 3 */ mc.getWindow().getScaledHeight() - 29 - HEIGHT;
            drawContext.drawTexture(ICONS, x, k, 0, 84, 182, 5);
            if (j > 0) {
                drawContext.drawTexture(ICONS, x, k, 0, 89, j, 5);
            }

            mc.getProfiler().pop();
        }
    }

    private static void renderMountHealth(DrawContext drawContext) {
        if (renderCustomMountInfo) {
            LivingEntity livingEntity = getRiddenEntity();
            if (livingEntity != null) {
                int i = getHeartCount(livingEntity);
                if (i != 0) {
                    int j = (int) Math.ceil((double) livingEntity.getHealth());
                    mc.getProfiler().swap("mountHealth");
                    int k = mc.getWindow().getScaledHeight() - 39 - HEIGHT;
                    int l = mc.getWindow().getScaledWidth() / 2 + 91;
                    int m = k;
                    int n = 0;

                    for (boolean var9 = false; i > 0; n += 20) {
                        int o = Math.min(i, 10);
                        i -= o;

                        for (int p = 0; p < o; ++p) {
                            int r = 0;
                            int s = l - p * 8 - 9;
                            drawContext.drawTexture(ICONS, s, m, 52 + r * 9, 9, 9, 9);
                            if (p * 2 + 1 + n < j) {
                                drawContext.drawTexture(ICONS, s, m, 88, 9, 9, 9);
                            }

                            if (p * 2 + 1 + n == j) {
                                drawContext.drawTexture(ICONS, s, m, 97, 9, 9, 9);
                            }
                        }

                        m -= 10;
                    }
                }
            }
        }
    }

    private static PlayerEntity getCameraPlayer() {
        return !(mc.getCameraEntity() instanceof PlayerEntity) ? null : (PlayerEntity) mc.getCameraEntity();
    }

    private static LivingEntity getRiddenEntity() {
        PlayerEntity playerEntity = getCameraPlayer();
        if (playerEntity != null) {
            Entity entity = playerEntity.getVehicle();
            if (entity == null) {
                return null;
            }

            if (entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }

        return null;
    }

    private static int getHeartCount(LivingEntity entity) {
        if (entity != null && entity.isLiving()) {
            float f = entity.getMaxHealth();
            int i = (int) (f + 0.5F) / 2;
            if (i > 30) {
                i = 30;
            }

            return i;
        } else {
            return 0;
        }
    }

    public static void renderExperienceBar(DrawContext drawContext, int x) {
        if (renderCustomExpBar && isSurvivalorAdventure()) {
            mc.getProfiler().push("expBar");
            RenderSystem.setShaderTexture(0, ICONS);
            int i = mc.player.getNextLevelExperience();
            int m;
            int n;
            if (i > 0) {
                m = (int) (mc.player.experienceProgress * 183.0F);
                n = mc.getWindow().getScaledHeight() - 29 - HEIGHT;
                drawContext.drawTexture(ICONS, x, n, 0, 64, 182, 5);
                if (m > 0) {
                    drawContext.drawTexture(ICONS, x, n, 0, 69, m, 5);
                }
            }

            mc.getProfiler().pop();
            if (mc.player.experienceLevel > 0) {
                mc.getProfiler().push("expLevel");
                String string = "" + mc.player.experienceLevel;
                m = (mc.getWindow().getScaledWidth() - mc.textRenderer.getWidth(string)) / 2;
                n = mc.getWindow().getScaledHeight() - 31 - 4 - HEIGHT;
                drawContext.drawText(mc.textRenderer, string, (m + 1), n, 0, false);
                drawContext.drawText(mc.textRenderer, string, (m - 1), n, 0, false);
                drawContext.drawText(mc.textRenderer, string, m, (n + 1), 0, false);
                drawContext.drawText(mc.textRenderer, string, m, (n - 1), 0, false);
                drawContext.drawText(mc.textRenderer, string, m, n, 8453920, false);
                mc.getProfiler().pop();
            }
        }

    }

    public static void renderHeldItemTooltip(DrawContext drawContext, int scaledWidth, int scaledHeight,
                                             TextRenderer textRenderer) {
        mc.getProfiler().push("selectedItemName");
        if (remainingHighlightTicks > 0 && !highlightingItemStack.isEmpty()) {
            MutableText mutableText = (Text.empty()).append(highlightingItemStack.getName())
                    .formatted(highlightingItemStack.getRarity().formatting);
            if (highlightingItemStack.hasCustomName()) {
                mutableText.formatted(Formatting.ITALIC);
            }

            int i = textRenderer.getWidth(mutableText);
            int j = (scaledWidth - i) / 2;
            int k = isSurvivalorAdventure() ? scaledHeight - HEIGHT - 31 - textOffset : scaledHeight - HEIGHT - 46;
            if (!mc.interactionManager.hasStatusBars()) {
                k += 14;
            }

            int l = (int) ((float) remainingHighlightTicks * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
                MatrixStack matrices = drawContext.getMatrices();
                matrices.push();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                int var10001 = j - 2;
                int var10002 = k - 2;
                int var10003 = j + i + 2;
                textRenderer.getClass();
                drawContext.fill(var10001, var10002, var10003, k + 9 + 2,
                        mc.options.getTextBackgroundColor(0));
                drawContext.drawTextWithShadow(textRenderer, (Text) mutableText, j, k, 16777215 + (l << 24));
                RenderSystem.disableBlend();
                matrices.pop();
            }
        }

        mc.getProfiler().pop();
    }

    private static void renderHotbarItem(DrawContext drawContext, int x, int y, float tickDelta, PlayerEntity player, ItemStack stack,
                                         ItemRenderer itemRenderer, TextRenderer textRenderer) {
        if (!stack.isEmpty()) {
            float f = (float) stack.getBobbingAnimationTime() - tickDelta;
            MatrixStack matrixStack = drawContext.getMatrices();
            if (f > 0.0F) {
                matrixStack.push();
                float g = 1.0F + f / 5.0F;
                matrixStack.translate((float) (x + 8), (float) (y + 12), 0.0F);
                matrixStack.scale(1.0F / g, (g + 1.0F) / 2.0F, 1.0F);
                matrixStack.translate((float) (-(x + 8)), (float) (-(y + 12)), 0.0F);
            }

            drawContext.drawItem(stack, x, y);
            if (f > 0.0F) {
                matrixStack.pop();
            }

            drawContext.drawItemInSlot(textRenderer, stack, x, y);
        }
    }

    public static void onClientTick() {
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            ClientPlayerInteractionManager interactionManager = client.interactionManager;
            PlayerEntity player = client.player;
            if (player != null) {
                ItemStack stack = client.player.getInventory().main.get(getIndex(client.player.getInventory().selectedSlot));
                if (stack.isEmpty()) {
                    remainingHighlightTicks = 0;
                } else if (!highlightingItemStack.isEmpty() && stack.getItem() == highlightingItemStack.getItem()
                        && stack.getName().equals(highlightingItemStack.getName())) {
                    if (remainingHighlightTicks > 0) {
                        --remainingHighlightTicks;
                    }
                } else {
                    remainingHighlightTicks = 40;
                }

                highlightingItemStack = stack;
            }

            if (verticalScroll.isPressed() && mc.currentScreen == null) {
                HudTypes config = Config.INSTANCE.type;
                CustomHudRenderer.renderHotbar = false;
                if ((Config.INSTANCE.sneakToSwapRows && player.isSneaking()) || fullBarVerticalScroll.isPressed()) {
                    renderEntireBar = true;
                    if (config == HudTypes.OVERLAY) {
                        config = HudTypes.INVISIBLE;
                    }
                }

                switch (config) {

                    case PUSHED:
                        renderPushedStatusBars = true;
                        CustomHudRenderer.renderExp = false;
                        renderCustomExpBar = true;
                        textOffset = 29;
                        renderMountInfo = false;
                        renderCustomMountInfo = true;
                        break;

                    case INVISIBLE:
                        renderStatusBars = false;
                        CustomHudRenderer.renderExp = false;
                        renderMountInfo = false;
                        renderCustomMountInfo = false;
                        break;

                    case OVERLAY:
                        break;

                }
                wasKeyDown = true;
            } else if (wasKeyDown) {
                if (accumulatedScrollDelta != 0) {
                    currentIndex = mc.player.getInventory().selectedSlot;
                    if (renderEntireBar) {
                        for (int i = 0; i < 9; i++) {
                            interactionManager.clickSlot(player.playerScreenHandler.syncId, getIndex(i), i,
                                    SlotActionType.SWAP, mc.player);
                        }
                    } else {
                        interactionManager.clickSlot(player.playerScreenHandler.syncId, getIndex(currentIndex),
                                currentIndex, SlotActionType.SWAP, player);
                    }

                }
                clear();
            }
        });

    }

    private static boolean isSurvivalorAdventure() {
        ClientPlayerInteractionManager interactionManager = mc.interactionManager;
        return interactionManager.getCurrentGameMode() == GameMode.SURVIVAL
                || interactionManager.getCurrentGameMode() == GameMode.ADVENTURE;
    }

    private static void clear() {
        wasKeyDown = false;
        accumulatedScrollDelta = 0;
        CustomHudRenderer.renderHotbar = true;
        CustomHudRenderer.renderExp = true;
        renderStatusBars = true;
        renderMountInfo = true;
        renderPushedStatusBars = false;
        renderCustomMountInfo = false;
        renderCustomExpBar = false;
        remainingHighlightTicks = 0;
        heldItemTooltips = true;
        highlightingItemStack = ItemStack.EMPTY;
        textOffset = 0;
        renderEntireBar = false;
    }

    private static int getIndex(int currentIndex) {
        int result = currentIndex;
        if (Math.signum(accumulatedScrollDelta) < 0) {
            result += slotsScrollDown[Math.abs(accumulatedScrollDelta)];
        } else {
            result += slotsScrollUp[Math.abs(accumulatedScrollDelta)];
        }
        return result;
    }
}
