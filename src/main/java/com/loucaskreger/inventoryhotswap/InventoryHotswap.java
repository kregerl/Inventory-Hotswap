package com.loucaskreger.inventoryhotswap;

import org.lwjgl.glfw.GLFW;

import com.loucaskreger.inventoryhotswap.config.Config;
import com.loucaskreger.inventoryhotswap.config.HudTypes;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameMode;

public class InventoryHotswap implements ModInitializer {

	public static final String MOD_ID = "inventoryhotswap";

	private static final int[] slotsScrollDown = { 0, 9, 18, 27 };
	private static final int[] slotsScrollUp = { 0, 27, 18, 9 };
	private static final int[] selectedScrollPositions = { 60, 39, 19, -1 };

	private static int accumulatedScrollDelta = 0;
	private static int remainingHighlightTicks = 0;
	private static int textOffset = 0;

	public static boolean renderStatusBars = true;
	public static boolean renderPushedStatusBars = false;
	public static boolean renderMountInfo = true;
	private static boolean renderCustomExpBar = false;
	private static boolean wasKeyDown = false;
	private static boolean renderCustomMountInfo = false;

	private static final int WIDTH = 22;
	public static final int HEIGHT = 61;

	private static MinecraftClient mc = MinecraftClient.getInstance();

	private static ItemStack highlightingItemStack = ItemStack.EMPTY;

	private static final Identifier TEXTURE = new Identifier(MOD_ID, "textures/gui/verticalbar.png");
	protected static final Identifier WIDGETS_TEXTURE_PATH = new Identifier("textures/gui/widgets.png");

	private static final KeyBinding verticalScroll = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			MOD_ID + ".key.verticalscroll", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, MOD_ID + ".key.categories"));

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
		HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
			DefaultedList<ItemStack> inventory = mc.player.inventory.main;
			int currentIndex = mc.player.inventory.selectedSlot;
			if (wasKeyDown) {

				mc.options.heldItemTooltips = false;

				int scaledWidth = mc.getWindow().getScaledWidth();
				int scaledHeight = mc.getWindow().getScaledHeight();

				int width = (scaledWidth / 2);

				InGameHud gui = mc.inGameHud;

				TextureManager textureManager = mc.getTextureManager();
				ItemRenderer itemRenderer = mc.getItemRenderer();
				TextRenderer fontRenderer = mc.textRenderer;

				RenderSystem.pushMatrix();
				RenderSystem.color4f(1F, 1F, 1F, 1.0F);
				RenderSystem.enableBlend();
				textureManager.bindTexture(WIDGETS_TEXTURE_PATH);

				gui.drawTexture(matrixStack, width - 91, scaledHeight - WIDTH, 0, 0, 182, 22);

				RenderSystem.disableBlend();
				RenderSystem.popMatrix();
				// Render items in re-rendered hotbar
				for (int i1 = 0; i1 < 9; ++i1) {
					int j1 = width - 90 + i1 * 20 + 2;
					int k1 = scaledHeight - 16 - 3;
					renderHotbarItem(j1, k1, tickDelta, mc.player, inventory.get(i1), itemRenderer, fontRenderer);
				}

				RenderSystem.pushMatrix();
				RenderSystem.color4f(1F, 1F, 1F, 1.0F);
				RenderSystem.enableBlend();

				textureManager.bindTexture(TEXTURE);
				// Render the verticalbar
				gui.drawTexture(matrixStack, width - 91 + (currentIndex * (WIDTH - 2)), scaledHeight - WIDTH - HEIGHT,
						0, 0, WIDTH, HEIGHT);

				// Render items in the verticalbar
				for (int i = 9, j = 0; i < 36; i += 9, j += 20) {
					ItemStack stack = inventory.get(currentIndex + i);
					int count = stack.getCount();
					itemRenderer.renderInGui(stack, width - 88 + (currentIndex * (WIDTH - 2)),
							scaledHeight - WIDTH - HEIGHT + 3 + j);
					itemRenderer.renderGuiItemOverlay(fontRenderer, stack, width - 88 + (currentIndex * (WIDTH - 2)),
							scaledHeight - WIDTH - HEIGHT + 3 + j, count == 1 ? "" : Integer.toString(count));
				}
				textureManager.bindTexture(WIDGETS_TEXTURE_PATH);
				// Render the selection square
				gui.drawTexture(matrixStack, width - 92 + (currentIndex * (WIDTH - 2)),
						scaledHeight - WIDTH - HEIGHT + scrollFunc(), 0, 22, 24, 24);

				renderHeldItemTooltip(matrixStack, scaledWidth, scaledHeight, fontRenderer);

				// Reset the icon texture to stop hearts and hunger from being screwed up.
				textureManager.bindTexture(DrawableHelper.GUI_ICONS_TEXTURE);

				renderMountHealth(matrixStack);
				if (mc.player.hasJumpingMount()) {
					renderMountJumpBar(matrixStack, mc.getWindow().getScaledWidth() / 2 - 91);
				} else {
					renderExperienceBar(matrixStack, mc.getWindow().getScaledWidth() / 2 - 91);
				}
				RenderSystem.disableBlend();
				RenderSystem.popMatrix();

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

	public static void renderMountJumpBar(MatrixStack matrices, int x) {
		if (renderCustomMountInfo) {
			mc.getProfiler().push("jumpBar");
			mc.getTextureManager().bindTexture(DrawableHelper.GUI_ICONS_TEXTURE);
			float f = mc.player.method_3151();
			int j = (int) (f * 183.0F);
			int k = /* mc.getWindow().getScaledHeight() - 32 + 3 */ mc.getWindow().getScaledHeight() - 29 - HEIGHT;
			mc.inGameHud.drawTexture(matrices, x, k, 0, 84, 182, 5);
			if (j > 0) {
				mc.inGameHud.drawTexture(matrices, x, k, 0, 89, j, 5);
			}

			mc.getProfiler().pop();
		}
	}

	private static void renderMountHealth(MatrixStack matrices) {
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
							mc.inGameHud.drawTexture(matrices, s, m, 52 + r * 9, 9, 9, 9);
							if (p * 2 + 1 + n < j) {
								mc.inGameHud.drawTexture(matrices, s, m, 88, 9, 9, 9);
							}

							if (p * 2 + 1 + n == j) {
								mc.inGameHud.drawTexture(matrices, s, m, 97, 9, 9, 9);
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

	public static void renderExperienceBar(MatrixStack matrices, int x) {
		if (renderCustomExpBar && isSurvivalorAdventure()) {
			mc.getProfiler().push("expBar");
			mc.getTextureManager().bindTexture(DrawableHelper.GUI_ICONS_TEXTURE);
			int i = mc.player.getNextLevelExperience();
			int m;
			int n;
			if (i > 0) {
				m = (int) (mc.player.experienceProgress * 183.0F);
				n = mc.getWindow().getScaledHeight() - 29 - HEIGHT;
				mc.inGameHud.drawTexture(matrices, x, n, 0, 64, 182, 5);
				if (m > 0) {
					mc.inGameHud.drawTexture(matrices, x, n, 0, 69, m, 5);
				}
			}

			mc.getProfiler().pop();
			if (mc.player.experienceLevel > 0) {
				mc.getProfiler().push("expLevel");
				String string = "" + mc.player.experienceLevel;
				m = (mc.getWindow().getScaledWidth() - mc.textRenderer.getWidth(string)) / 2;
				n = mc.getWindow().getScaledHeight() - 31 - 4 - HEIGHT;
				mc.textRenderer.draw(matrices, (String) string, (float) (m + 1), (float) n, 0);
				mc.textRenderer.draw(matrices, (String) string, (float) (m - 1), (float) n, 0);
				mc.textRenderer.draw(matrices, (String) string, (float) m, (float) (n + 1), 0);
				mc.textRenderer.draw(matrices, (String) string, (float) m, (float) (n - 1), 0);
				mc.textRenderer.draw(matrices, string, (float) m, (float) n, 8453920);
				mc.getProfiler().pop();
			}
		}

	}

	public static void renderHeldItemTooltip(MatrixStack matrices, int scaledWidth, int scaledHeight,
			TextRenderer textRenderer) {
		mc.getProfiler().push("selectedItemName");
		if (remainingHighlightTicks > 0 && !highlightingItemStack.isEmpty()) {
			MutableText mutableText = (new LiteralText("")).append(highlightingItemStack.getName())
					.formatted(highlightingItemStack.getRarity().formatting);
			if (highlightingItemStack.hasCustomName()) {
				mutableText.formatted(Formatting.ITALIC);
			}

			int i = textRenderer.getWidth((StringVisitable) mutableText);
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
				RenderSystem.pushMatrix();
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				int var10001 = j - 2;
				int var10002 = k - 2;
				int var10003 = j + i + 2;
				textRenderer.getClass();
				DrawableHelper.fill(matrices, var10001, var10002, var10003, k + 9 + 2,
						mc.options.getTextBackgroundColor(0));
				textRenderer.drawWithShadow(matrices, (Text) mutableText, (float) j, (float) k, 16777215 + (l << 24));
				RenderSystem.disableBlend();
				RenderSystem.popMatrix();
			}
		}

		mc.getProfiler().pop();
	}

	private static void renderHotbarItem(int x, int y, float tickDelta, PlayerEntity player, ItemStack stack,
			ItemRenderer itemRenderer, TextRenderer textRenderer) {
		if (!stack.isEmpty()) {
			float f = (float) stack.getCooldown() - tickDelta;
			if (f > 0.0F) {
				RenderSystem.pushMatrix();
				float g = 1.0F + f / 5.0F;
				RenderSystem.translatef((float) (x + 8), (float) (y + 12), 0.0F);
				RenderSystem.scalef(1.0F / g, (g + 1.0F) / 2.0F, 1.0F);
				RenderSystem.translatef((float) (-(x + 8)), (float) (-(y + 12)), 0.0F);
			}

			itemRenderer.renderInGuiWithOverrides(player, stack, x, y);
			if (f > 0.0F) {
				RenderSystem.popMatrix();
			}

			itemRenderer.renderGuiItemOverlay(textRenderer, stack, x, y);
		}
	}

	public static void onClientTick() {
		ClientTickEvents.END_CLIENT_TICK.register((client) -> {
			ClientPlayerInteractionManager interactionManager = client.interactionManager;
			PlayerEntity player = client.player;
			if (player != null) {
				if (mc.player.isRiding()) {
					System.out.println("Riding");
				}
				ItemStack stack = client.player.inventory.main.get(getIndex(client.player.inventory.selectedSlot));
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
					int currentIndex = mc.player.inventory.selectedSlot;

					interactionManager.clickSlot(player.playerScreenHandler.syncId, getIndex(currentIndex),
							currentIndex, SlotActionType.SWAP, player);

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
		mc.options.heldItemTooltips = true;
		highlightingItemStack = ItemStack.EMPTY;
		textOffset = 0;
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
