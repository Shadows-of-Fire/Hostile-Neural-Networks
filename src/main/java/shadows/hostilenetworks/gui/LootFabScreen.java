package shadows.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.item.MobPredictionItem;

public class LootFabScreen extends ContainerScreen<LootFabContainer> {

	private static final int WIDTH = 176;
	private static final int HEIGHT = 178;
	private static final ResourceLocation BASE = new ResourceLocation(HostileNetworks.MODID, "textures/gui/loot_fabricator.png");
	private static final ResourceLocation PLAYER = new ResourceLocation(HostileNetworks.MODID, "textures/gui/default_gui.png");
	private DataModel model = null;
	private int currentPage = 0;

	public LootFabScreen(LootFabContainer pMenu, PlayerInventory pPlayerInventory, ITextComponent pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		this.imageHeight = HEIGHT;
		this.imageWidth = WIDTH;
	}

	@Override
	public void render(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTicks) {
		this.model = MobPredictionItem.getStoredModel(this.menu.getSlot(0).getItem());

		if (this.model != null) {
			this.buttons.get(0).visible = this.currentPage > 0;
			this.buttons.get(1).visible = this.currentPage < model.getFabDrops().size() / 9;
		} else {
			this.buttons.get(0).visible = false;
			this.buttons.get(1).visible = false;
		}

		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTicks);
		this.renderTooltip(stack, pMouseX, pMouseY);
	}

	@Override
	public void init(Minecraft pMinecraft, int pWidth, int pHeight) {
		super.init(pMinecraft, pWidth, pHeight);
		this.addButton(new ImageButton(this.getGuiLeft() + 13, this.getGuiTop() + 68, 29, 12, 49, 83, 12, BASE, btn -> {
			if (model != null && this.currentPage > 0) this.currentPage--;
		}));

		this.addButton(new ImageButton(this.getGuiLeft() + 46, this.getGuiTop() + 68, 29, 12, 78, 83, 12, BASE, btn -> {
			if (model != null && (this.currentPage < model.getFabDrops().size() / 9)) this.currentPage++;
		}));
	}

	@Override
	protected void renderLabels(MatrixStack pMatrixStack, int pX, int pY) {

	}

	@Override
	protected void renderTooltip(MatrixStack pPoseStack, int pX, int pY) {
		int selection = this.menu.getSelectedDrop(model);
		if (selection != -1 && this.isHovering(79, 5, 16, 16, pX, pY)) {
			this.renderWrappedToolTip(pPoseStack, Arrays.asList(new TranslationTextComponent("hostilenetworks.gui.clear")), pX, pY, font);
		} else if (this.isHovering(6, 10, 7, 53, pX, pY)) {
			List<ITextComponent> txt = new ArrayList<>(2);
			txt.add(new TranslationTextComponent("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.fabPowerCap));
			txt.add(new TranslationTextComponent("hostilenetworks.gui.fab_cost", HostileConfig.fabPowerCost));
			this.renderWrappedToolTip(pPoseStack, txt, pX, pY, font);
		}
		super.renderTooltip(pPoseStack, pX, pY);
	}

	@Override
	public boolean mouseClicked(double pX, double pY, int pButton) {
		if (model != null) {
			List<ItemStack> drops = model.getFabDrops();
			int selection = this.menu.getSelectedDrop(model);
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					if (y * 3 + x < drops.size() && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY) && selection != y * 3 + x) {
						Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, this.currentPage * 9 + y * 3 + x);
						Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					}
				}
			}

			if (selection != -1 && this.isHovering(79, 5, 16, 16, pX, pY)) {
				Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, -1);
				Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}

		}
		return super.mouseClicked(pX, pY, pButton);
	}

	@Override
	protected void renderBg(MatrixStack stack, float pPartialTicks, int pX, int pY) {
		Minecraft mc = Minecraft.getInstance();
		mc.getTextureManager().bind(BASE);
		int left = getGuiLeft();
		int top = getGuiTop();
		Screen.blit(stack, left, top, 0, 0, 176, 83, 256, 256);

		int energyHeight = 53 - MathHelper.ceil(53F * this.menu.getEnergyStored() / HostileConfig.fabPowerCap);
		Screen.blit(stack, left + 6, top + 10, 0, 83, 7, energyHeight, 256, 256);

		int progHeight = MathHelper.floor(35F * (this.menu.getRuntime() - pPartialTicks) / 60);
		if (this.menu.getRuntime() == 0) progHeight = 35;
		Screen.blit(stack, left + 84, top + 23, 7, 83, 6, progHeight, 256, 256);

		if (model != null) {
			List<ItemStack> drops = model.getFabDrops();
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					if (y * 3 + x < Math.min(drops.size() - this.currentPage * 9, 9) && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY)) {
						Screen.blit(stack, left + 16 + 19 * x, top + 8 + 19 * y, 13, 83, 18, 18, 256, 256);
					}
				}
			}
		}

		int selection = this.menu.getSelectedDrop(model);

		if (selection != -1 && selection / 9 == this.currentPage) {
			int selIdx = selection - this.currentPage * 9;
			Screen.blit(stack, left + 16 + 19 * (selIdx % 3), top + 8 + 19 * (selIdx / 3), 31, 83, 18, 18, 256, 256);
		}

		mc.getTextureManager().bind(PLAYER);
		Screen.blit(stack, left, top + 88, 0, 0, 176, 90, 256, 256);

		if (selection != -1) {
			List<ItemStack> drops = model.getFabDrops();
			mc.getItemRenderer().renderAndDecorateItem(drops.get(selection), left + 79, top + 5);
			mc.getItemRenderer().renderGuiItemDecorations(font, drops.get(selection), left + 79 - 1, top + 5 - 1);
		}

		if (model != null) {
			left += 17;
			top += 9;
			List<ItemStack> drops = model.getFabDrops();
			int x = 0;
			int y = 0;
			for (int i = 0; i < Math.min(drops.size() - this.currentPage * 9, 9); i++) {
				mc.getItemRenderer().renderAndDecorateItem(drops.get(i + this.currentPage * 9), left + x * 19, top + y * 19);
				mc.getItemRenderer().renderGuiItemDecorations(font, drops.get(i + this.currentPage * 9), left + x * 19 - 1, top + y * 19 - 1);
				if (++x == 3) {
					y++;
					x = 0;
				}
			}
		}

	}

}
