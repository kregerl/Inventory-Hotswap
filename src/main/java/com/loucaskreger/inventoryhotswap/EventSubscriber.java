package com.loucaskreger.inventoryhotswap;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;

import com.google.common.base.Strings;
import com.loucaskreger.inventoryhotswap.config.ClientConfig;
import com.loucaskreger.inventoryhotswap.config.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
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

@Mod.EventBusSubscriber(modid = InventoryHotswap.MOD_ID, value = Dist.CLIENT)
public class EventSubscriber {

	public static final KeyBinding vertScroll = new KeyBinding(InventoryHotswap.MOD_ID + ".key.verticalscroll",
			GLFW_KEY_LEFT_ALT, InventoryHotswap.MOD_ID + ".key.categories");

	private static final int[] slotsScrollDown = { 0, 9, 18, 27 };
	private static final int[] slotsScrollUp = { 0, 27, 18, 9 };

	private static final int[] selectedScrollPositions = { 65, 43, 21, -1 };
//	private static final int[] largeSelectedScrollPositions = { 65, 43, 21, -1 };

	private static final int WIDTH = 22;
	private static final int HEIGHT = 66;

	private static final ResourceLocation VERT_TEXTURE = new ResourceLocation(InventoryHotswap.MOD_ID,
			"textures/gui/verticalbar.png");
	private static final ResourceLocation TEXTURE = new ResourceLocation(InventoryHotswap.MOD_ID,
			"textures/gui/largebarselection.png");

	private static ResourceLocation WIDGETS_TEXTURE_PATH;
	static {
		WIDGETS_TEXTURE_PATH = ObfuscationReflectionHelper.getPrivateValue(IngameGui.class, null,
				/* WIDGETS_TEXTURE_PATH */ "field_110330_c");
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
	public static void onKeyPressed(final InputEvent.KeyInputEvent event) {
		if (wasKeyDown && event.getAction() == GLFW.GLFW_PRESS) {
			int inverted = ClientConfig.inverted.get() ? -1 : 1;
			switch (event.getKey()) {
			case GLFW_KEY_1:
				accumulatedScrollDelta = inverted * 1;
				vertScroll.setPressed(false);
				moveToCorrectSlot = true;
				break;
			case GLFW_KEY_2:
				accumulatedScrollDelta = inverted * 2;
				vertScroll.setPressed(false);
				moveToCorrectSlot = true;
				break;
			case GLFW_KEY_3:
				accumulatedScrollDelta = inverted * 3;
				vertScroll.setPressed(false);
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
					ForgeIngameGui.renderFood = false;
					ForgeIngameGui.renderHealth = false;
					ForgeIngameGui.renderArmor = false;
					ForgeIngameGui.renderAir = false;
					ForgeIngameGui.renderExperiance = false;
					ForgeIngameGui.renderHealthMount = false;
					ForgeIngameGui.renderJumpBar = false;
				} else if (isGuiPushed) {
					ForgeIngameGui.left_height += HEIGHT;
					ForgeIngameGui.right_height += HEIGHT;
					ForgeIngameGui.renderExperiance = false;
					ForgeIngameGui.renderHealthMount = false;
					ForgeIngameGui.renderJumpBar = false;
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
				if (renderEntireBar) {
					RenderSystem.pushMatrix();
					RenderSystem.color4f(1F, 1F, 1F, 1.0F);
					RenderSystem.enableBlend();
					textureManager.bindTexture(WIDGETS_TEXTURE_PATH);
					for (int i = 0; i < 4; i++) {
						gui.blit(width - 91, scaledHeight - WIDTH - (i * 22), 0, 0, 182, 22);
					}

					RenderSystem.disableBlend();
					RenderSystem.popMatrix();

					for (int k = 3; k > 0; k--) {
						int l = ClientConfig.inverted.get() ? Math.abs(k - 3) + 1 : k;
						fontRenderer.drawString(String.valueOf(k), width - 97, scaledHeight - 13 - (l * 22), 0xFFFFFF);
					}

					for (int i1 = 0; i1 < 9; ++i1) {
						// Render all item sprites in the multi bar display
						int j1 = width - 90 + i1 * 20 + 2;
						int k1 = scaledHeight - 16 - 3;
						renderHotbarItem(j1, k1, event.getPartialTicks(), mc.player, inventory.get(i1), itemRenderer,
								fontRenderer);
						renderHotbarItem(j1, k1 - 22, event.getPartialTicks(), mc.player, inventory.get(i1 + 27),
								itemRenderer, fontRenderer);
						renderHotbarItem(j1, k1 - 44, event.getPartialTicks(), mc.player, inventory.get(i1 + 18),
								itemRenderer, fontRenderer);
						renderHotbarItem(j1, k1 - 66, event.getPartialTicks(), mc.player, inventory.get(i1 + 9),
								itemRenderer, fontRenderer);
					}
					RenderSystem.pushMatrix();
					RenderSystem.color4f(1F, 1F, 1F, 1.0F);
					RenderSystem.enableBlend();

					textureManager.bindTexture(TEXTURE);
					// Render the selection square
					gui.blit(width - 92, scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0, 0, 184, 24);

					textureManager.bindTexture(AbstractGui.GUI_ICONS_LOCATION);
					int x = mc.getMainWindow().getScaledWidth() / 2 - 91;

					renderVehicleHealth(scaledHeight, scaledWidth);
					if (mc.player.isRidingHorse()) {
						renderHorseJumpBar(x, scaledHeight);
					} else {
						renderExpBar(x);
					}
					RenderSystem.disableBlend();
					RenderSystem.popMatrix();

				} else {

					RenderSystem.pushMatrix();
					RenderSystem.color4f(1F, 1F, 1F, 1.0F);
					RenderSystem.enableBlend();
					textureManager.bindTexture(WIDGETS_TEXTURE_PATH);
					// Re-render hotbar without selection
					gui.blit(width - 91, scaledHeight - WIDTH, 0, 0, 182, 22);

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

					textureManager.bindTexture(VERT_TEXTURE);
					// Render the verticalbar
					gui.blit(width - 91 + (currentIndex * (WIDTH - 2)), scaledHeight - WIDTH - HEIGHT, 0, 0, WIDTH,
							HEIGHT);

					for (int k = 3; k > 0; k--) {
						int l = ClientConfig.inverted.get() ? Math.abs(k - 3) + 1 : k;
						fontRenderer.drawString(String.valueOf(k), width - 97, scaledHeight - 13 - (l * 22), 0xFFFFFF);
					}
					// Render items in the verticalbar
					for (int i = 27, j = 22; i > 0; i -= 9, j += 22) {
						int j1 = width - 88 + (currentIndex * (WIDTH - 2));
						int k1 = scaledHeight - 16 - 3;
						ItemStack stack = inventory.get(currentIndex + i);

						renderHotbarItem(j1, k1 - j, event.getPartialTicks(), mc.player, stack, itemRenderer,
								fontRenderer);
					}

					textureManager.bindTexture(WIDGETS_TEXTURE_PATH);
					// Render the selection square
					gui.blit(width - 92 + (currentIndex * (WIDTH - 2)), scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0,
							22, 24, 24);

					renderSelectedItem(inventory.get(getIndex(currentIndex)), mc, scaledWidth, scaledHeight,
							fontRenderer);

					// Reset the icon texture to stop hearts and hunger from being screwed up.
					textureManager.bindTexture(AbstractGui.GUI_ICONS_LOCATION);

					int x = mc.getMainWindow().getScaledWidth() / 2 - 91;

					renderVehicleHealth(scaledHeight, scaledWidth);
					if (mc.player.isRidingHorse()) {
						renderHorseJumpBar(x, scaledHeight);
					} else {
						renderExpBar(x);
					}
					RenderSystem.disableBlend();
					RenderSystem.popMatrix();
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

//	private static int largeScrollFunc() {
//		if (Math.signum(accumulatedScrollDelta) == 0) {
//			return largeSelectedScrollPositions[0];
//		} else if (Math.signum(accumulatedScrollDelta) > 0) {
//			return largeSelectedScrollPositions[accumulatedScrollDelta];
//		}
//		return largeSelectedScrollPositions[Math
//				.abs(Math.abs(accumulatedScrollDelta) - largeSelectedScrollPositions.length)];
//	}

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

	public static void renderHorseJumpBar(int x, int scaledHeight) {
		mc.getProfiler().startSection("jumpBar");
		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
		float f = mc.player.getHorseJumpPower();
//		int i = 182;
		int j = (int) (f * 183.0F);
		int k = scaledHeight - 32 + 3 - HEIGHT;
		mc.ingameGUI.blit(x, k, 0, 84, 182, 5);
		if (j > 0) {
			mc.ingameGUI.blit(x, k, 0, 89, j, 5);
		}

		mc.getProfiler().endSection();
	}

	private static void renderVehicleHealth(int scaledHeight, int scaledWidth) {
		LivingEntity livingentity = getMountEntity();
		if (livingentity != null) {
			int i = getRenderMountHealth(livingentity);
			if (i != 0) {
				int j = (int) Math.ceil((double) livingentity.getHealth());
				mc.getProfiler().endStartSection("mountHealth");
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
						mc.ingameGUI.blit(k2, i1, 52 + j2 * 9, 9, 9, 9);
						if (l1 * 2 + 1 + j1 < j) {
							mc.ingameGUI.blit(k2, i1, 88, 9, 9, 9);
						}

						if (l1 * 2 + 1 + j1 == j) {
							mc.ingameGUI.blit(k2, i1, 97, 9, 9, 9);
						}
					}

					i1 -= 10;
				}

			}
		}
	}

	private static int getRenderMountHealth(LivingEntity p_212306_1_) {
		if (p_212306_1_ != null && p_212306_1_.isLiving()) {
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

	private static PlayerEntity getRenderViewPlayer() {
		return !(mc.getRenderViewEntity() instanceof PlayerEntity) ? null : (PlayerEntity) mc.getRenderViewEntity();
	}

	private static LivingEntity getMountEntity() {
		PlayerEntity playerentity = getRenderViewPlayer();
		if (playerentity != null) {
			Entity entity = playerentity.getRidingEntity();
			if (entity == null) {
				return null;
			}

			if (entity instanceof LivingEntity) {
				return (LivingEntity) entity;
			}
		}

		return null;
	}

	public static void renderExpBar(int x) {
		if (isGuiPushed) {

			mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
			int i = mc.player.xpBarCap();
			if (i > 0) {
				int j = 182;
				int k = (int) (mc.player.experience * 183.0F);
				// -32 + 3
				int l = mc.getMainWindow().getScaledHeight() - 29 - HEIGHT;
				mc.ingameGUI.blit(x, l, 0, 64, j, 5);
				if (k > 0) {
					mc.ingameGUI.blit(x, l, 0, 69, k, 5);
				}
			}

			if (mc.player.experienceLevel > 0) {
				String s = "" + mc.player.experienceLevel;
				int i1 = (mc.getMainWindow().getScaledWidth() - mc.fontRenderer.getStringWidth(s)) / 2;
				int j1 = mc.getMainWindow().getScaledHeight() - 31 - 4 - HEIGHT;
				mc.fontRenderer.drawString(s, (float) (i1 + 1), (float) j1, 0);
				mc.fontRenderer.drawString(s, (float) (i1 - 1), (float) j1, 0);
				mc.fontRenderer.drawString(s, (float) i1, (float) (j1 + 1), 0);
				mc.fontRenderer.drawString(s, (float) i1, (float) (j1 - 1), 0);
				mc.fontRenderer.drawString(s, (float) i1, (float) j1, 8453920);
			}
		}
	}

	private static void renderSelectedItem(ItemStack stack, Minecraft mc, int scaledWidth, int scaledHeight,
			FontRenderer fontRenderer) {
		mc.getProfiler().startSection("selectedItemName");
		if (remainingHighlightTicks > 0 && !highlightingItemStack.isEmpty()) {
			ITextComponent itextcomponent = (new StringTextComponent(""))
					.appendSibling(highlightingItemStack.getDisplayName())
					.applyTextStyle(highlightingItemStack.getRarity().color);
			if (highlightingItemStack.hasDisplayName()) {
				itextcomponent.applyTextStyle(TextFormatting.ITALIC);
			}

			String s = itextcomponent.getFormattedText();
			s = highlightingItemStack.getHighlightTip(s);
			int i = (scaledWidth - fontRenderer.getStringWidth(s)) / 2;
			int j = mc.playerController.gameIsSurvivalOrAdventure() ? scaledHeight - HEIGHT - 31 - textOffset
					: scaledHeight - HEIGHT - 46;
			if (!mc.playerController.shouldDrawHUD()) {
				j += 14;
			}

			int k = (int) ((float) remainingHighlightTicks * 256.0F / 10.0F);
			if (k > 255) {
				k = 255;
			}

			if (k > 0) {
				RenderSystem.pushMatrix();
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				AbstractGui.fill(i - 2, j - 2, i + fontRenderer.getStringWidth(s) + 2, j + 9 + 2,
						mc.gameSettings.getChatBackgroundColor(0));
				FontRenderer font = highlightingItemStack.getItem().getFontRenderer(highlightingItemStack);
				if (font == null) {
					fontRenderer.drawStringWithShadow(s, (float) i, (float) j, 16777215 + (k << 24));
				} else {
					i = (scaledWidth - font.getStringWidth(s)) / 2;
					font.drawStringWithShadow(s, (float) i, (float) j, 16777215 + (k << 24));
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
							&& itemstack.getHighlightTip(itemstack.getDisplayName().getUnformattedComponentText())
									.equals(highlightingItemStack.getHighlightTip(
											highlightingItemStack.getDisplayName().getUnformattedComponentText())))) {
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

			if (mc.player.isSneaking()) {
				if (ClientConfig.guiRenderType.get() == InventoryHotswap.GuiRenderType.OVERLAY)
					isGuiInvisible = true;
				renderEntireBar = true;
			} else {
				renderEntireBar = false;
				// determines whether of not user wants survival gui parts rendered
				isGuiInvisible = ClientConfig.guiRenderType.get() == InventoryHotswap.GuiRenderType.INVISIBLE
						&& pc.gameIsSurvivalOrAdventure();
				isGuiPushed = ClientConfig.guiRenderType.get() == InventoryHotswap.GuiRenderType.PUSHED
						&& pc.gameIsSurvivalOrAdventure();
			}
			wasKeyDown = true;
		} else if (wasKeyDown) {
			if (accumulatedScrollDelta != 0) {
				currentIndex = mc.player.inventory.currentItem;
				if (renderEntireBar) {
					for (int i = 0; i < 9; i++) {
						pc.windowClick(mc.player.container.windowId, getIndex(i), i, ClickType.SWAP, mc.player);
					}
				} else {

					pc.windowClick(mc.player.container.windowId, getIndex(currentIndex), currentIndex, ClickType.SWAP,
							mc.player);
				}

			}
			clear();

		} else if (moveToCorrectSlot && currentIndex != -1) {
			mc.player.inventory.currentItem = currentIndex;
			moveToCorrectSlot = false;
		}
	}

	private static void clear() {
		wasKeyDown = false;
		accumulatedScrollDelta = 0;
		ForgeIngameGui.renderHotbar = true;
		ForgeIngameGui.renderHealth = true;
		ForgeIngameGui.renderArmor = true;
		ForgeIngameGui.renderAir = true;
		ForgeIngameGui.renderExperiance = true;
		ForgeIngameGui.renderFood = true;
		ForgeIngameGui.renderHealthMount = true;
		ForgeIngameGui.renderJumpBar = true;
		mc.gameSettings.heldItemTooltips = true;
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
