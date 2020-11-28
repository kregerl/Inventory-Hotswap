package com.loucaskreger.ezinventory;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

import com.loucaskreger.ezinventory.config.ClientConfig;
import com.loucaskreger.ezinventory.config.Config;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber(modid = EZInventory.MOD_ID, value = Dist.CLIENT)
public class EventSubscriber {

	public static final KeyBinding vertScroll = new KeyBinding(EZInventory.MOD_ID + ".key.verticalscroll",
			GLFW_KEY_LEFT_ALT, EZInventory.MOD_ID + ".key.categories");

	private static final int[] slotsScrollDown = { 0, 9, 18, 27 };
	private static final int[] slotsScrollUp = { 0, 27, 18, 9 };

	private static final int[] selectedScrollPositions = { 60, 39, 19, -1 };

	private static final int WIDTH = 22;
	private static final int HEIGHT = 61;

	private static final ResourceLocation TEXTURE = new ResourceLocation(EZInventory.MOD_ID,
			"textures/gui/verticalbar.png");

	private static ResourceLocation WIDGETS_TEXTURE_PATH;
	static {
		WIDGETS_TEXTURE_PATH = ObfuscationReflectionHelper.getPrivateValue(IngameGui.class, null,
				/* WIDGETS_TEXTURE_PATH */ "field_110330_c");
	}

	private static int accumulatedScrollDelta = 0;
	private static int textOffset = 0;
	private static int remainingHighlightTicks = 0;

	private static boolean wasKeyDown = false;
	private static boolean isGuiInvisible = false;
	private static boolean isGuiPushed = false;

	private static ItemStack highlightingItemStack = ItemStack.EMPTY;

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent) {
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
	public static void onGUIRender(final RenderGameOverlayEvent.Pre event) {
		if (event.getType().equals(RenderGameOverlayEvent.ElementType.ALL)) {
			if (wasKeyDown) {
				if (isGuiInvisible) {
					ForgeIngameGui.renderFood = false;
					ForgeIngameGui.renderHealth = false;
					ForgeIngameGui.renderArmor = false;
					ForgeIngameGui.renderAir = false;
					ForgeIngameGui.renderExperiance = false;
				} else if (isGuiPushed) {
					ForgeIngameGui.left_height += HEIGHT;
					ForgeIngameGui.right_height += HEIGHT;
					ForgeIngameGui.renderExperiance = false;
					textOffset = 29;
				}
			}
		}
	}

	@SubscribeEvent
	public static void onGUIRender(final RenderGameOverlayEvent.Post event) {

		Minecraft mc = Minecraft.getInstance();
		NonNullList<ItemStack> inventory = mc.player.inventory.mainInventory;
		int currentIndex = mc.player.inventory.currentItem;
		if (wasKeyDown) {

			mc.gameSettings.heldItemTooltips = false;

			int scaledHeight = mc.getMainWindow().getScaledHeight();
			int scaledWidth = mc.getMainWindow().getScaledWidth();

			int width = (scaledWidth / 2);

			IngameGui gui = mc.ingameGUI;

			TextureManager textureManager = mc.getTextureManager();
			ItemRenderer itemRenderer = mc.getItemRenderer();
			FontRenderer fontRenderer = mc.fontRenderer;

			if (event.getType().equals(RenderGameOverlayEvent.ElementType.ALL)) {
				RenderSystem.pushMatrix();
				RenderSystem.color4f(1F, 1F, 1F, 1.0F);
				RenderSystem.enableBlend();
				textureManager.bindTexture(WIDGETS_TEXTURE_PATH);
				// Re-render hotbar without selection
				gui.blit(new MatrixStack(), width - 91, scaledHeight - WIDTH, 0, 0, 182, 22);

				RenderSystem.disableBlend();
				RenderSystem.popMatrix();
				// Render items in re-rendered hotbar
				for (int i1 = 0; i1 < 9; ++i1) {
					int j1 = width - 90 + i1 * 20 + 2;
					int k1 = scaledHeight - 16 - 3;
					renderHotbarItem(j1, k1, event.getPartialTicks(), mc.player, inventory.get(i1), itemRenderer,
							fontRenderer);
				}

				RenderSystem.pushMatrix();
				RenderSystem.color4f(1F, 1F, 1F, 1.0F);
				RenderSystem.enableBlend();

				textureManager.bindTexture(TEXTURE);
				// Render the verticalbar
				gui.blit(new MatrixStack(), width - 91 + (currentIndex * (WIDTH - 2)), scaledHeight - WIDTH - HEIGHT, 0,
						0, WIDTH, HEIGHT);

				// Render items in the verticalbar
				for (int i = 9, j = 0; i < 36; i += 9, j += 20) {
					ItemStack stack = inventory.get(currentIndex + i);
					int count = stack.getCount();
					itemRenderer.renderItemIntoGUI(stack, width - 88 + (currentIndex * (WIDTH - 2)),
							scaledHeight - WIDTH - HEIGHT + 3 + j);
					itemRenderer.renderItemOverlayIntoGUI(fontRenderer, stack,
							width - 88 + (currentIndex * (WIDTH - 2)), scaledHeight - WIDTH - HEIGHT + 3 + j,
							count == 1 ? "" : Integer.toString(count));
				}
				textureManager.bindTexture(WIDGETS_TEXTURE_PATH);
				// Render the selection square
				gui.blit(new MatrixStack(), width - 92 + (currentIndex * (WIDTH - 2)),
						scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0, 22, 24, 24);

				renderSelectedItem(inventory.get(getIndex(currentIndex)), mc, scaledWidth, scaledHeight, fontRenderer);
//				highlightingItemStack = inventory.get(getIndex(currentIndex));

				// Reset the icon texture to stop hearts and hunger from being screwed up.
				textureManager.bindTexture(AbstractGui.GUI_ICONS_LOCATION);

				renderExpBar(mc.getMainWindow().getScaledWidth() / 2 - 91, mc);
				RenderSystem.disableBlend();
				RenderSystem.popMatrix();

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
	 * @param player
	 * @param stack
	 * @param itemRenderer
	 * @param fontRenderer
	 */
	private static void renderHotbarItem(int x, int y, float partialTicks, PlayerEntity player, ItemStack stack,
			ItemRenderer itemRenderer, FontRenderer fontRenderer) {
		if (!stack.isEmpty()) {
			float f = (float) stack.getAnimationsToGo() - partialTicks;
			if (f > 0.0F) {
				RenderSystem.pushMatrix();
				float f1 = 1.0F + f / 5.0F;
				RenderSystem.translatef((float) (x + 8), (float) (y + 12), 0.0F);
				RenderSystem.scalef(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
				RenderSystem.translatef((float) (-(x + 8)), (float) (-(y + 12)), 0.0F);
			}

			itemRenderer.renderItemAndEffectIntoGUI(player, stack, x, y);
			if (f > 0.0F) {
				RenderSystem.popMatrix();
			}

			itemRenderer.renderItemOverlays(fontRenderer, stack, x, y);
		}
	}

	public static void renderExpBar(int x, Minecraft mc) {
		if (isGuiPushed) {

			mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
			int i = mc.player.xpBarCap();
			if (i > 0) {
				int j = 182;
				int k = (int) (mc.player.experience * 183.0F);
				// -32 + 3
				int l = mc.getMainWindow().getScaledHeight() - 29 - HEIGHT;
				mc.ingameGUI.blit(new MatrixStack(), x, l, 0, 64, j, 5);
				if (k > 0) {
					mc.ingameGUI.blit(new MatrixStack(), x, l, 0, 69, k, 5);
				}
			}

			if (mc.player.experienceLevel > 0) {
				String s = "" + mc.player.experienceLevel;
				int i1 = (mc.getMainWindow().getScaledWidth() - mc.fontRenderer.getStringWidth(s)) / 2;
				int j1 = mc.getMainWindow().getScaledHeight() - 31 - 4 - HEIGHT;
				mc.fontRenderer.drawString(new MatrixStack(), s, (float) (i1 + 1), (float) j1, 0);
				mc.fontRenderer.drawString(new MatrixStack(), s, (float) (i1 - 1), (float) j1, 0);
				mc.fontRenderer.drawString(new MatrixStack(), s, (float) i1, (float) (j1 + 1), 0);
				mc.fontRenderer.drawString(new MatrixStack(), s, (float) i1, (float) (j1 - 1), 0);
				mc.fontRenderer.drawString(new MatrixStack(), s, (float) i1, (float) j1, 8453920);
			}
		}
	}

	private static void renderSelectedItem(ItemStack stack, Minecraft mc, int scaledWidth, int scaledHeight,
			FontRenderer fontRenderer) {
		mc.getProfiler().startSection("selectedItemName");
		if (remainingHighlightTicks > 0 && !highlightingItemStack.isEmpty()) {
			IFormattableTextComponent iformattabletextcomponent = (new StringTextComponent(""))
					.append(highlightingItemStack.getDisplayName()).mergeStyle(highlightingItemStack.getRarity().color);
			if (highlightingItemStack.hasDisplayName()) {
				iformattabletextcomponent.mergeStyle(TextFormatting.ITALIC);
			}

			ITextComponent highlightTip = highlightingItemStack.getHighlightTip(iformattabletextcomponent);
			int i = fontRenderer.getStringPropertyWidth(highlightTip);
			int j = (scaledWidth - i) / 2;
			int k =  mc.playerController.gameIsSurvivalOrAdventure() ? scaledHeight - HEIGHT - 31 - textOffset
					: scaledHeight - HEIGHT - 46;
			if (!mc.playerController.shouldDrawHUD()) {
				k += 14;
			}

			int l = (int) ((float) remainingHighlightTicks * 256.0F / 10.0F);
			if (l > 255) {
				l = 255;
			}

			if (l > 0) {
				RenderSystem.pushMatrix();
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				AbstractGui.fill(new MatrixStack(), j - 2, k - 2, j + i + 2, k + 9 + 2,
						mc.gameSettings.getChatBackgroundColor(0));
				FontRenderer font = highlightingItemStack.getItem().getFontRenderer(highlightingItemStack);
				if (font == null) {
					fontRenderer.func_243246_a(new MatrixStack(), highlightTip, (float) j, (float) k,
							16777215 + (l << 24));
				} else {
					j = (scaledWidth - font.getStringPropertyWidth(highlightTip)) / 2;
					font.func_243246_a(new MatrixStack(), highlightTip, (float) j, (float) k, 16777215 + (l << 24));
				}
				RenderSystem.disableBlend();
				RenderSystem.popMatrix();
			}
		}

		mc.getProfiler().endSection();
	}

	@SubscribeEvent
	public static void onClientTick(final ClientTickEvent event) {
		Minecraft mc = Minecraft.getInstance();
		PlayerController pc = mc.playerController;
		if (mc.player != null) {
			ItemStack itemstack = mc.player.inventory.mainInventory.get(getIndex(mc.player.inventory.currentItem));
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

		if (vertScroll.isKeyDown() && mc.currentScreen == null) {
			ForgeIngameGui.renderHotbar = false;
			// determines whether of not user wants survival gui parts rendered
			isGuiInvisible = ClientConfig.guiRenderType.get() == EZInventory.GuiRenderType.INVISIBLE
					&& pc.gameIsSurvivalOrAdventure();
			isGuiPushed = ClientConfig.guiRenderType.get() == EZInventory.GuiRenderType.PUSHED
					&& pc.gameIsSurvivalOrAdventure();
			wasKeyDown = true;
		} else if (wasKeyDown) {
			if (accumulatedScrollDelta != 0) {
				int currentIndex = mc.player.inventory.currentItem;

				pc.windowClick(mc.player.container.windowId, getIndex(currentIndex), currentIndex, ClickType.SWAP,
						mc.player);
			}
			clear(mc);
		}
	}

	private static void clear(Minecraft mc) {
		wasKeyDown = false;
		accumulatedScrollDelta = 0;
		ForgeIngameGui.renderHotbar = true;
		ForgeIngameGui.renderHealth = true;
		ForgeIngameGui.renderArmor = true;
		ForgeIngameGui.renderAir = true;
		ForgeIngameGui.renderExperiance = true;
		ForgeIngameGui.renderFood = true;
		mc.gameSettings.heldItemTooltips = true;
		textOffset = 0;
		remainingHighlightTicks = 0;
		highlightingItemStack = ItemStack.EMPTY;
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
