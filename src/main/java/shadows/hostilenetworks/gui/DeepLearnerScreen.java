package shadows.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.util.Color;

public class DeepLearnerScreen extends ContainerScreen<DeepLearnerContainer> {

	public static final int WIDTH = 338;
	public static final int HEIGHT = 235;
	private static final ResourceLocation BASE = new ResourceLocation(HostileNetworks.MODID, "textures/gui/deep_learner.png");
	private static final ResourceLocation PLAYER = new ResourceLocation(HostileNetworks.MODID, "textures/gui/default_gui.png");

	private List<TickableText> texts = new ArrayList<>();
	private boolean hasModels = false;

	public DeepLearnerScreen(DeepLearnerContainer pMenu, PlayerInventory pPlayerInventory, ITextComponent pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		this.imageWidth = WIDTH;
		this.imageHeight = HEIGHT;
		this.setupEmptyText();
	}

	@Override
	public void init(Minecraft pMinecraft, int pWidth, int pHeight) {
		super.init(pMinecraft, pWidth, pHeight);
		//texts.add(new TickableText(this.menu.getItems().get(0).getDisplayName().getString(), 0xFFFFFF));
	}

	@Override
	public void render(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTicks) {
		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTicks);
		this.renderTooltip(stack, pMouseX, pMouseY);
	}

	@Override
	protected void renderBg(MatrixStack matrix, float pPartialTicks, int pX, int pY) {
		this.getMinecraft().getTextureManager().bind(BASE);
		int left = this.getGuiLeft();
		int top = this.getGuiTop();
		this.blit(matrix, left + 41, top, 0, 0, 256, 140);
		this.getMinecraft().getTextureManager().bind(PLAYER);
		this.blit(matrix, left + 81, top + 145, 0, 0, 176, 90);
	}

	@Override
	protected void renderLabels(MatrixStack stack, int pX, int pY) {
		int left = 49;
		int top = 8;
		int spacing = this.font.lineHeight;
		int idx = 0;
		for (TickableText t : texts) {
			t.render(font, stack, left, top + spacing * idx++);
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.menu.hasModels()) {
			if (hasModels) {
				setupEmptyText();
				hasModels = false;
			}
		} else {

		}

		for (int i = 0; i < texts.size(); i++) {
			TickableText txt = texts.get(i);
			if (!txt.isDone()) {
				txt.tick();
				break;
			}
		}
	}

	private void setupEmptyText() {
		this.texts.clear();
		for (int i = 0; i < 7; i++) {
			addText(I18n.get("hostilenetworks.gui.learner_empty." + i), i == 0 ? Color.AQUA : Color.WHITE);
		}
	}

	private void addText(String msg, int color) {
		this.texts.add(new TickableText(msg, color));
	}

}
