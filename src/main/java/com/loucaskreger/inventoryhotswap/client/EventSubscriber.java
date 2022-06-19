package com.loucaskreger.inventoryhotswap.client;

import com.loucaskreger.inventoryhotswap.InventoryHotswap;
import com.loucaskreger.inventoryhotswap.config.ClientConfig;
import com.loucaskreger.inventoryhotswap.config.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;

@Mod.EventBusSubscriber(modid = InventoryHotswap.MOD_ID, value = Dist.CLIENT)
public class EventSubscriber {

    public static final KeyMapping vertScroll = new KeyMapping(InventoryHotswap.MOD_ID + ".key.verticalscroll",
            GLFW_KEY_LEFT_ALT, InventoryHotswap.MOD_ID + ".key.categories");

    private static final int[] slotsScrollDown = {0, 9, 18, 27};
    private static final int[] slotsScrollUp = {0, 27, 18, 9};

    private static final int[] selectedScrollPositions = {65, 43, 21, -1};
//	private static final int[] largeSelectedScrollPositions = { 65, 43, 21, -1 };

    /**
     * The value of the left_height and right_height in {@link ForgeIngameGui}
     */
    private static final int DEFAULT_GUI_HEIGHT = 39;
    private static final int WIDTH = 22;
    private static final int HEIGHT = 66;

    private static final ResourceLocation VERT_TEXTURE = new ResourceLocation(InventoryHotswap.MOD_ID,
            "textures/gui/verticalbar.png");
    private static final ResourceLocation TEXTURE = new ResourceLocation(InventoryHotswap.MOD_ID,
            "textures/gui/largebarselection.png");

    private static ResourceLocation WIDGETS_TEXTURE_PATH;


    static {

        WIDGETS_TEXTURE_PATH = ObfuscationReflectionHelper.getPrivateValue(Gui.class, null,
                /* WIDGETS_TEXTURE_PATH */ "f_92982_");
    }

    private static int accumulatedScrollDelta = 0;
    private static int textOffset = 0;
    private static int remainingHighlightTicks = 0;
    private static int currentIndex = -1;

    private static boolean wasKeyDown = false;
    private static boolean isGuiInvisible = false;
    private static boolean isGuiPushed = false;
    private static boolean moveToCorrectSlot = false;
    private static boolean renderEntireBar = false;

    private static ItemStack highlightingItemStack = ItemStack.EMPTY;

    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent configEvent) {
        if (configEvent.getConfig().getSpec() == Config.CLIENT_SPEC) {
            Config.bakeConfig();
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(final InputEvent.MouseScrollEvent event) {
        if (wasKeyDown) {
            event.setCanceled(true);
            accumulatedScrollDelta += event.getScrollDelta();
            accumulatedScrollDelta %= 4;
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(final InputEvent.KeyInputEvent event) {
        if (wasKeyDown && event.getAction() == GLFW.GLFW_PRESS) {
            int inverted = ClientConfig.inverted.get() ? -1 : 1;
            switch (event.getKey()) {
                case GLFW_KEY_1:
                    accumulatedScrollDelta = inverted;
                    vertScroll.setDown(false);
                    moveToCorrectSlot = true;
                    break;
                case GLFW_KEY_2:
                    accumulatedScrollDelta = inverted * 2;
                    vertScroll.setDown(false);
                    moveToCorrectSlot = true;
                    break;
                case GLFW_KEY_3:
                    accumulatedScrollDelta = inverted * 3;
                    vertScroll.setDown(false);
                    moveToCorrectSlot = true;
                    break;
            }

        }

    }

    @SubscribeEvent
    public static void onGUIRender(final RenderGameOverlayEvent.Pre event) {
        if (event.getType().equals(RenderGameOverlayEvent.ElementType.ALL)) {
            if (wasKeyDown) {
                if (isGuiInvisible) {
                    OverlayRegistry.enableOverlay(ForgeIngameGui.FOOD_LEVEL_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.PLAYER_HEALTH_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.ARMOR_LEVEL_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.AIR_LEVEL_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.MOUNT_HEALTH_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.JUMP_BAR_ELEMENT, false);
                } else if (isGuiPushed) {
                    ObfuscationReflectionHelper.setPrivateValue(ForgeIngameGui.class, (ForgeIngameGui) mc.gui, DEFAULT_GUI_HEIGHT + HEIGHT, "left_height");
                    ObfuscationReflectionHelper.setPrivateValue(ForgeIngameGui.class, (ForgeIngameGui) mc.gui, DEFAULT_GUI_HEIGHT + HEIGHT, "right_height");
                    OverlayRegistry.enableOverlay(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.MOUNT_HEALTH_ELEMENT, false);
                    OverlayRegistry.enableOverlay(ForgeIngameGui.JUMP_BAR_ELEMENT, false);
                    textOffset = 29;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onGUIRender(final RenderGameOverlayEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        NonNullList<ItemStack> inventory = mc.player.getInventory().items;

        int currentIndex = mc.player.getInventory().selected;
        if (wasKeyDown) {

            mc.options.heldItemTooltips = false;

            int scaledHeight = mc.getWindow().getGuiScaledHeight();
            int scaledWidth = mc.getWindow().getGuiScaledWidth();

            int width = (scaledWidth / 2);

            Gui gui = mc.gui;

            ItemRenderer itemRenderer = mc.getItemRenderer();
            Font fontRenderer = mc.font;
            PoseStack matrixStack = event.getPoseStack();

            if (event.getType().equals(RenderGameOverlayEvent.ElementType.ALL)) {
                if (renderEntireBar) {
                    matrixStack.pushPose();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE_PATH);
                    for (int i = 0; i < 4; i++) {
                        gui.blit(matrixStack, width - 91, scaledHeight - WIDTH - (i * 22), 0, 0, 182, 22);
                    }

                    RenderSystem.disableBlend();
                    matrixStack.popPose();

                    for (int k = 3; k > 0; k--) {
                        int l = ClientConfig.inverted.get() ? Math.abs(k - 3) + 1 : k;
                        fontRenderer.draw(matrixStack, String.valueOf(k), width - 98,
                                scaledHeight - 13 - (l * 22), 0xFFFFFF);
                    }

                    for (int i1 = 0; i1 < 9; ++i1) {
                        // Render all item sprites in the multi bar display
                        int j1 = width - 90 + i1 * 20 + 2;
                        int k1 = scaledHeight - 16 - 3;

                        renderHotbarItem(matrixStack, j1, k1, event.getPartialTick(), inventory.get(i1), itemRenderer,
                                fontRenderer);
                        renderHotbarItem(matrixStack, j1, k1 - 22, event.getPartialTick(), inventory.get(i1 + 27),
                                itemRenderer, fontRenderer);
                        renderHotbarItem(matrixStack, j1, k1 - 44, event.getPartialTick(), inventory.get(i1 + 18),
                                itemRenderer, fontRenderer);
                        renderHotbarItem(matrixStack, j1, k1 - 66, event.getPartialTick(), inventory.get(i1 + 9),
                                itemRenderer, fontRenderer);
                    }
                    matrixStack.pushPose();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();

                    RenderSystem.setShaderTexture(0, TEXTURE);
                    // Render the selection square
                    gui.blit(matrixStack, width - 92, scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0, 0, 184, 24);

                    RenderSystem.setShaderTexture(0, Gui.GUI_ICONS_LOCATION);
                    int x = scaledWidth / 2 - 91;

                    renderVehicleHealth(matrixStack, scaledHeight, scaledWidth);
                    if (mc.player.isRidingJumpable()) {
                        renderHorseJumpBar(matrixStack, x, scaledHeight);
                    } else {
                        renderExpBar(matrixStack, x);
                    }
                    RenderSystem.disableBlend();
                    matrixStack.popPose();

                } else {

                    matrixStack.pushPose();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE_PATH);
                    // Re-render hotbar without selection
                    gui.blit(matrixStack, width - 91, scaledHeight - WIDTH, 0, 0, 182, 22);

                    RenderSystem.disableBlend();
                    matrixStack.popPose();
                    // Render items in re-rendered hotbar
                    for (int i1 = 0; i1 < 9; ++i1) {
                        int j1 = width - 90 + i1 * 20 + 2;
                        int k1 = scaledHeight - 16 - 3;
                        renderHotbarItem(matrixStack, j1, k1, event.getPartialTick(), inventory.get(i1), itemRenderer,
                                fontRenderer);
                    }

                    matrixStack.pushPose();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1.0F);
                    RenderSystem.enableBlend();

                    RenderSystem.setShaderTexture(0, VERT_TEXTURE);
                    // Render the verticalbar
                    gui.blit(matrixStack, width - 91 + (currentIndex * (WIDTH - 2)), scaledHeight - WIDTH - HEIGHT, 0,
                            0, WIDTH, HEIGHT);

                    for (int k = 3; k > 0; k--) {
                        int l = ClientConfig.inverted.get() ? Math.abs(k - 3) + 1 : k;
                        fontRenderer.draw(matrixStack, String.valueOf(k),
                                width - 98 + (currentIndex * (WIDTH - 2)), scaledHeight - 13 - (l * 22), 0xFFFFFF);
                    }
                    // Render items in the verticalbar
                    for (int i = 27, j = 22; i > 0; i -= 9, j += 22) {
                        int j1 = width - 88 + (currentIndex * (WIDTH - 2));
                        int k1 = scaledHeight - 16 - 3;
                        ItemStack stack = inventory.get(currentIndex + i);

                        renderHotbarItem(matrixStack, j1, k1 - j, event.getPartialTick(), stack, itemRenderer,
                                fontRenderer);
                    }

                    RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE_PATH);
                    // Render the selection square
                    gui.blit(matrixStack, width - 92 + (currentIndex * (WIDTH - 2)),
                            scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0, 22, 24, 24);

                    renderSelectedItem(matrixStack, mc, scaledWidth, scaledHeight,
                            fontRenderer);

                    // Reset the icon texture to stop hearts and hunger from being screwed up.
                    RenderSystem.setShaderTexture(0, Gui.GUI_ICONS_LOCATION);

                    int x = scaledWidth / 2 - 91;

                    renderVehicleHealth(matrixStack, scaledHeight, scaledWidth);
                    if (mc.player.isRidingJumpable()) {
                        renderHorseJumpBar(matrixStack, x, scaledHeight);
                    } else {
                        renderExpBar(matrixStack, x);
                    }
                    RenderSystem.disableBlend();
                    matrixStack.popPose();
                }

            }
        }
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


    /**
     * Stolen straight from the IngameGUI hotbar rendering
     *
     * @param x
     * @param y
     * @param partialTicks
     * @param stack
     * @param itemRenderer
     * @param fontRenderer
     */
    private static void renderHotbarItem(PoseStack matrixStack, int x, int y, float partialTicks, ItemStack stack,
                                         ItemRenderer itemRenderer, Font fontRenderer) {
        if (!stack.isEmpty()) {
            float f = (float) stack.getPopTime() - partialTicks;
            if (f > 0.0F) {
//                RenderSystem.pushMatrix();
                matrixStack.pushPose();
                float f1 = 1.0F + f / 5.0F;
                matrixStack.translate((float) (x + 8), (float) (y + 12), 0.0F);
                matrixStack.scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
                matrixStack.translate((float) (-(x + 8)), (float) (-(y + 12)), 0.0F);
            }

            itemRenderer.renderAndDecorateItem(stack, x, y);
            if (f > 0.0F) {
                matrixStack.popPose();
            }

            itemRenderer.renderGuiItemDecorations(fontRenderer, stack, x, y);
        }
    }

    public static void renderHorseJumpBar(PoseStack matrixStack, int x, int scaledHeight) {
        mc.getProfiler().push("jumpBar");
        RenderSystem.setShaderTexture(0, Gui.GUI_ICONS_LOCATION);
        float f = mc.player.getJumpRidingScale();
//		int i = 182;
        int j = (int) (f * 183.0F);
        int k = scaledHeight - 32 + 3 - HEIGHT;
        mc.gui.blit(matrixStack, x, k, 0, 84, 182, 5);
        if (j > 0) {
            mc.gui.blit(matrixStack, x, k, 0, 89, j, 5);
        }

        mc.getProfiler().pop();
    }

    private static void renderVehicleHealth(PoseStack matrixStack, int scaledHeight, int scaledWidth) {
        LivingEntity livingentity = getMountEntity();
        if (livingentity != null) {
            int i = getRenderMountHealth(livingentity);
            if (i != 0) {
                int j = (int) Math.ceil((double) livingentity.getHealth());
                mc.getProfiler().popPush("mountHealth");
                int k = scaledHeight - 39 - HEIGHT;
                int l = scaledWidth / 2 + 91;
                int i1 = k;
                int j1 = 0;

                for (boolean flag = false; i > 0; j1 += 20) {
                    int k1 = Math.min(i, 10);
                    i -= k1;

                    for (int l1 = 0; l1 < k1; ++l1) {
                        int i2 = 52;
                        int j2 = 0;
                        int k2 = l - l1 * 8 - 9;
                        mc.gui.blit(matrixStack, k2, i1, 52 + j2 * 9, 9, 9, 9);
                        if (l1 * 2 + 1 + j1 < j) {
                            mc.gui.blit(matrixStack, k2, i1, 88, 9, 9, 9);
                        }

                        if (l1 * 2 + 1 + j1 == j) {
                            mc.gui.blit(matrixStack, k2, i1, 97, 9, 9, 9);
                        }
                    }

                    i1 -= 10;
                }

            }
        }
    }

    private static int getRenderMountHealth(LivingEntity p_212306_1_) {
        if (p_212306_1_ != null && p_212306_1_.isAlive()) {
            float f = p_212306_1_.getMaxHealth();
            int i = (int) (f + 0.5F) / 2;
            if (i > 30) {
                i = 30;
            }

            return i;
        } else {
            return 0;
        }
    }

    private static Player getRenderViewPlayer() {
        return !(mc.getCameraEntity() instanceof LocalPlayer) ? null : (Player) mc.getCameraEntity();
    }

    private static LivingEntity getMountEntity() {
        Player playerentity = getRenderViewPlayer();
        if (playerentity != null) {
            Entity entity = playerentity.getVehicle();
            if (entity == null) {
                return null;
            }

            if (entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }

        return null;
    }

    public static void renderExpBar(PoseStack matrixStack, int x) {
        if (isGuiPushed) {

            RenderSystem.setShaderTexture(0, Gui.GUI_ICONS_LOCATION);
            int i = mc.player.getXpNeededForNextLevel();
            if (i > 0) {
                int j = 182;
                int k = (int) (mc.player.experienceLevel * 183.0F);
                // -32 + 3
                int l = mc.getWindow().getGuiScaledHeight() - 29 - HEIGHT;
                mc.gui.blit(matrixStack, x, l, 0, 64, j, 5);
                if (k > 0) {
                    mc.gui.blit(matrixStack, x, l, 0, 69, k, 5);
                }
            }

            if (mc.player.experienceLevel > 0) {
                String s = "" + mc.player.experienceLevel;
                int i1 = (mc.getWindow().getGuiScaledWidth() - mc.font.width(s)) / 2;
                int j1 = mc.getWindow().getGuiScaledHeight() - 31 - 4 - HEIGHT;
                mc.font.draw(matrixStack, s, (float) (i1 + 1), (float) j1, 0);
                mc.font.draw(matrixStack, s, (float) (i1 - 1), (float) j1, 0);
                mc.font.draw(matrixStack, s, (float) i1, (float) (j1 + 1), 0);
                mc.font.draw(matrixStack, s, (float) i1, (float) (j1 - 1), 0);
                mc.font.draw(matrixStack, s, (float) i1, (float) j1, 8453920);
            }
        }
    }

    private static void renderSelectedItem(PoseStack matrixStack, Minecraft mc, int scaledWidth, int scaledHeight,
                                           Font fontRenderer) {
        mc.getProfiler().push("selectedItemName");
        if (remainingHighlightTicks > 0 && !highlightingItemStack.isEmpty()) {

            MutableComponent mutablecomponent = Component.empty().append(highlightingItemStack.getHoverName()).withStyle(highlightingItemStack.getRarity().getStyleModifier());

            if (highlightingItemStack.hasCustomHoverName()) {
                mutablecomponent.withStyle(ChatFormatting.ITALIC);
            }

            Component highlightTip = highlightingItemStack.getHighlightTip(mutablecomponent);
            int i = fontRenderer.width(highlightTip);
            int j = (scaledWidth - i) / 2;
            int k = mc.gameMode.getPlayerMode().isSurvival() ? scaledHeight - HEIGHT - 31 - textOffset
                    : scaledHeight - HEIGHT - 46;
            if (!mc.gameMode.canHurtPlayer()) {
                k += 14;
            }

            int l = (int) ((float) remainingHighlightTicks * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
//                RenderSystem.pushMatrix();
                matrixStack.pushPose();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                Gui.fill(matrixStack, j - 2, k - 2, j + i + 2, k + 9 + 2,
                        mc.options.getBackgroundColor(0));
                Font font = net.minecraftforge.client.RenderProperties.get(highlightingItemStack).getFont(highlightingItemStack);
                if (font == null) {
                    fontRenderer.drawShadow(matrixStack, highlightTip, (float) j, (float) k,
                            16777215 + (l << 24));
                } else {
                    j = (scaledWidth - font.width(highlightTip)) / 2;
                    j = (scaledWidth - font.width(highlightTip)) / 2;
                    font.drawShadow(matrixStack, highlightTip, (float) j, (float) k, 16777215 + (l << 24));
                }
                RenderSystem.disableBlend();
                matrixStack.popPose();
            }
        }

        mc.getProfiler().endTick();
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        MultiPlayerGameMode pc = mc.gameMode;
        if (mc.player != null) {
            ItemStack itemstack = mc.player.getInventory().items.get(getIndex(mc.player.getInventory().selected));
            if (itemstack.isEmpty()) {
                remainingHighlightTicks = 0;
            } else if (!highlightingItemStack.isEmpty() && itemstack.getItem() == highlightingItemStack.getItem()
                    && (itemstack.getDisplayName().equals(highlightingItemStack.getDisplayName())
                    && itemstack.getHighlightTip(itemstack.getDisplayName()).equals(
                    highlightingItemStack.getHighlightTip(highlightingItemStack.getDisplayName())))) {
                if (remainingHighlightTicks > 0) {
                    --remainingHighlightTicks;
                }
            } else {
                remainingHighlightTicks = 40;
            }

            highlightingItemStack = itemstack;
        }


        if (vertScroll.isDown() && mc.screen == null) {
            OverlayRegistry.enableOverlay(ForgeIngameGui.HOTBAR_ELEMENT, false);

            if (mc.player.isShiftKeyDown()) {
                if (ClientConfig.guiRenderType.get() == GuiRenderType.OVERLAY)
                    isGuiInvisible = true;
                renderEntireBar = true;
            } else {
                renderEntireBar = false;
                // determines whether of not user wants survival gui parts rendered
                isGuiInvisible = ClientConfig.guiRenderType.get() == GuiRenderType.INVISIBLE
                        && pc.getPlayerMode().isSurvival();

                isGuiPushed = ClientConfig.guiRenderType.get() == GuiRenderType.PUSHED
                        && pc.getPlayerMode().isSurvival();
            }
            wasKeyDown = true;
        } else if (wasKeyDown) {
            if (accumulatedScrollDelta != 0) {
                currentIndex = mc.player.getInventory().selected;
                if (renderEntireBar) {
                    for (int i = 0; i < 9; i++) {
                        pc.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, getIndex(i), i, ClickType.SWAP, mc.player);
                    }
                } else {

                    pc.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, getIndex(currentIndex), currentIndex, ClickType.SWAP,
                            mc.player);
                }

            }
            clear();

        } else if (moveToCorrectSlot && currentIndex != -1) {
            mc.player.getInventory().selected = currentIndex;
            moveToCorrectSlot = false;
        }
    }

    private static void clear() {
        wasKeyDown = false;
        accumulatedScrollDelta = 0;
        OverlayRegistry.enableOverlay(ForgeIngameGui.HOTBAR_ELEMENT, true);
        OverlayRegistry.enableOverlay(ForgeIngameGui.PLAYER_HEALTH_ELEMENT, true);
        OverlayRegistry.enableOverlay(ForgeIngameGui.ARMOR_LEVEL_ELEMENT, true);
        OverlayRegistry.enableOverlay(ForgeIngameGui.AIR_LEVEL_ELEMENT, true);
        OverlayRegistry.enableOverlay(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, true);
        OverlayRegistry.enableOverlay(ForgeIngameGui.FOOD_LEVEL_ELEMENT, true);
        OverlayRegistry.enableOverlay(ForgeIngameGui.MOUNT_HEALTH_ELEMENT, true);
        OverlayRegistry.enableOverlay(ForgeIngameGui.JUMP_BAR_ELEMENT, true);

        mc.options.heldItemTooltips = true;
        textOffset = 0;
        remainingHighlightTicks = 0;
        highlightingItemStack = ItemStack.EMPTY;
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
