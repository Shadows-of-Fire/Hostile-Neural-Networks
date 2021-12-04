package shadows.hostilenetworks.gui;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.item.MobPredictionItem;

public class LootFabScreen extends ContainerScreen<LootFabContainer> {

	private static final int WIDTH = 176;
	private static final int HEIGHT = 178;
	private static final ResourceLocation BASE = new ResourceLocation(HostileNetworks.MODID, "textures/gui/loot_fabricator.png");
	private static final ResourceLocation PLAYER = new ResourceLocation(HostileNetworks.MODID, "textures/gui/default_gui.png");

	public LootFabScreen(LootFabContainer pMenu, PlayerInventory pPlayerInventory, ITextComponent pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		this.imageHeight = HEIGHT;
		this.imageWidth = WIDTH;
	}

	@Override
	public void render(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTicks) {
		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTicks);
		this.renderTooltip(stack, pMouseX, pMouseY);
	}

	@Override
	protected void renderLabels(MatrixStack pMatrixStack, int pX, int pY) {

	}

	@Override
	protected void renderTooltip(MatrixStack pPoseStack, int pX, int pY) {

		super.renderTooltip(pPoseStack, pX, pY);
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

		int progHeight = 35 - MathHelper.ceil(53F * this.menu.getRuntime() / 120);
		Screen.blit(stack, left + 84, top + 23, 7, 83, 6, progHeight, 256, 256);

		mc.getTextureManager().bind(PLAYER);
		Screen.blit(stack, left, top + 88, 0, 0, 176, 90, 256, 256);

		DataModel model = MobPredictionItem.getStoredModel(this.menu.getSlot(0).getItem());
		if (model != null) {
			left += 17;
			top += 9;
			List<ItemStack> drops = model.getFabDrops();
			int x = 0;
			int y = 0;
			for (int i = 0; i < drops.size(); i++) {
				mc.getItemRenderer().renderAndDecorateItem(drops.get(i), left + x * 18, top + y * 18);
				if (++x == 3) {
					y++;
					x = 0;
				}
			}
		}
	}

}
