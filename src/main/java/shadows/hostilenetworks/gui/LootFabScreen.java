package shadows.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.placebo.screen.PlaceboContainerScreen;

public class LootFabScreen extends PlaceboContainerScreen<LootFabContainer> {

	private static final int WIDTH = 176;
	private static final int HEIGHT = 178;
	private static final ResourceLocation BASE = new ResourceLocation(HostileNetworks.MODID, "textures/gui/loot_fabricator.png");
	private static final ResourceLocation PLAYER = new ResourceLocation(HostileNetworks.MODID, "textures/gui/default_gui.png");
	private DataModel model = null;
	private int currentPage = 0;
	private ImageButton btnLeft, btnRight;

	public LootFabScreen(LootFabContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		this.imageHeight = HEIGHT;
		this.imageWidth = WIDTH;
	}

	@Override
	public void render(PoseStack stack, int pMouseX, int pMouseY, float pPartialTicks) {
		this.model = MobPredictionItem.getStoredModel(this.menu.getSlot(0).getItem());

		if (this.model != null) {
			this.btnLeft.visible = this.currentPage > 0;
			this.btnRight.visible = this.currentPage < this.model.getFabDrops().size() / 9;
		} else {
			this.btnLeft.visible = false;
			this.btnRight.visible = false;
		}

		super.render(stack, pMouseX, pMouseY, pPartialTicks);
	}

	@Override
	public void init() {
		super.init();
		this.btnLeft = this.addRenderableWidget(new ImageButton(this.getGuiLeft() + 13, this.getGuiTop() + 68, 29, 12, 49, 83, 12, BASE, btn -> {
			if (this.model != null && this.currentPage > 0) this.currentPage--;
		}));

		this.btnRight = this.addRenderableWidget(new ImageButton(this.getGuiLeft() + 46, this.getGuiTop() + 68, 29, 12, 78, 83, 12, BASE, btn -> {
			if (this.model != null && this.currentPage < this.model.getFabDrops().size() / 9) this.currentPage++;
		}));
	}

	@Override
	protected void renderLabels(PoseStack pMatrixStack, int pX, int pY) {

	}

	@Override
	protected void renderTooltip(PoseStack pPoseStack, int pX, int pY) {
		int selection = this.menu.getSelectedDrop(this.model);
		if (selection != -1 && this.isHovering(79, 5, 16, 16, pX, pY)) {
			this.renderComponentTooltip(pPoseStack, Arrays.asList(Component.translatable("hostilenetworks.gui.clear")), pX, pY, this.font);
		} else if (this.isHovering(6, 10, 7, 53, pX, pY)) {
			List<Component> txt = new ArrayList<>(2);
			txt.add(Component.translatable("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.fabPowerCap));
			txt.add(Component.translatable("hostilenetworks.gui.fab_cost", HostileConfig.fabPowerCost));
			this.renderComponentTooltip(pPoseStack, txt, pX, pY, this.font);
		}

		if (this.model != null) {
			List<ItemStack> drops = this.model.getFabDrops();
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					if (y * 3 + x < Math.min(drops.size() - this.currentPage * 9, 9) && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY)) {
						drawOnLeft(pPoseStack, this.getTooltipFromItem(drops.get(this.currentPage * 9 + y * 3 + x)), this.getGuiTop() + 15);
					}
				}
			}
		}

		super.renderTooltip(pPoseStack, pX, pY);
	}

	@Override
	public boolean mouseClicked(double pX, double pY, int pButton) {
		if (this.model != null) {
			List<ItemStack> drops = this.model.getFabDrops();
			int selection = this.menu.getSelectedDrop(this.model);
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					if (y * 3 + x < drops.size() && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY) && selection != y * 3 + x) {
						Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, this.currentPage * 9 + y * 3 + x);
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					}
				}
			}

			if (selection != -1 && this.isHovering(79, 5, 16, 16, pX, pY)) {
				Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, -1);
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}

		}
		return super.mouseClicked(pX, pY, pButton);
	}

	@Override
	protected void renderBg(PoseStack stack, float pPartialTicks, int pX, int pY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, BASE);
		int left = this.getGuiLeft();
		int top = this.getGuiTop();
		GuiComponent.blit(stack, left, top, 0, 0, 176, 83, 256, 256);

		int energyHeight = 53 - Mth.ceil(53F * this.menu.getEnergyStored() / HostileConfig.fabPowerCap);
		GuiComponent.blit(stack, left + 6, top + 10, 0, 83, 7, energyHeight, 256, 256);

		int progHeight = Mth.floor(35F * (this.menu.getRuntime() - pPartialTicks) / 60);
		if (this.menu.getRuntime() == 0) progHeight = 35;
		GuiComponent.blit(stack, left + 84, top + 23, 7, 83, 6, progHeight, 256, 256);

		if (this.model != null) {
			List<ItemStack> drops = this.model.getFabDrops();
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					if (y * 3 + x < Math.min(drops.size() - this.currentPage * 9, 9) && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY)) {
						GuiComponent.blit(stack, left + 16 + 19 * x, top + 8 + 19 * y, 13, 83, 18, 18, 256, 256);
					}
				}
			}
		}

		int selection = this.menu.getSelectedDrop(this.model);

		if (selection != -1 && selection / 9 == this.currentPage) {
			int selIdx = selection - this.currentPage * 9;
			GuiComponent.blit(stack, left + 16 + 19 * (selIdx % 3), top + 8 + 19 * (selIdx / 3), 31, 83, 18, 18, 256, 256);
		}

		RenderSystem.setShaderTexture(0, PLAYER);
		GuiComponent.blit(stack, left, top + 88, 0, 0, 176, 90, 256, 256);
		Minecraft mc = Minecraft.getInstance();

		if (selection != -1) {
			List<ItemStack> drops = this.model.getFabDrops();
			mc.getItemRenderer().renderAndDecorateItem(drops.get(selection), left + 79, top + 5);
			mc.getItemRenderer().renderGuiItemDecorations(this.font, drops.get(selection), left + 79 - 1, top + 5 - 1);
		}

		if (this.model != null) {
			left += 17;
			top += 9;
			List<ItemStack> drops = this.model.getFabDrops();
			int x = 0;
			int y = 0;
			for (int i = 0; i < Math.min(drops.size() - this.currentPage * 9, 9); i++) {
				mc.getItemRenderer().renderAndDecorateItem(drops.get(i + this.currentPage * 9), left + x * 19, top + y * 19);
				mc.getItemRenderer().renderGuiItemDecorations(this.font, drops.get(i + this.currentPage * 9), left + x * 19 - 1, top + y * 19 - 1);
				if (++x == 3) {
					y++;
					x = 0;
				}
			}
		}

	}

	public void drawOnLeft(PoseStack stack, List<Component> list, int y) {
		if (list.isEmpty()) return;
		int xPos = this.getGuiLeft() - 16 - list.stream().map(this.font::width).max(Integer::compare).get();
		int maxWidth = 9999;
		if (xPos < 0) {
			maxWidth = this.getGuiLeft() - 6;
			xPos = -8;
		}

		List<FormattedText> split = new ArrayList<>();
		int lambdastupid = maxWidth;
		list.forEach(comp -> split.addAll(this.font.getSplitter().splitLines(comp, lambdastupid, comp.getStyle())));

		this.renderComponentTooltip(stack, split, xPos, y, this.font);

		//GuiUtils.drawHoveringText(stack, list, xPos, y, width, height, maxWidth, this.font);
	}

}
