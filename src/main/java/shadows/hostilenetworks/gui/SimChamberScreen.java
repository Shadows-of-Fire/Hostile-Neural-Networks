package shadows.hostilenetworks.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import shadows.hostilenetworks.util.Color;
import shadows.hostilenetworks.util.TickableText;

public class SimChamberScreen extends ContainerScreen<SimChamberContainer> {

	public static final int WIDTH = 232;
	public static final int HEIGHT = 230;
	private static final ResourceLocation BASE = new ResourceLocation(HostileNetworks.MODID, "textures/gui/sim_chamber.png");
	private static final ResourceLocation PLAYER = new ResourceLocation(HostileNetworks.MODID, "textures/gui/default_gui.png");

	private List<TickableText> body = new ArrayList<>(7);
	private FailureState lastFailState = FailureState.NONE;
	private boolean runtimeTextLoaded = false;

	public SimChamberScreen(SimChamberContainer pMenu, PlayerInventory pPlayerInventory, ITextComponent pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		this.imageWidth = WIDTH;
		this.imageHeight = HEIGHT;
	}

	@Override
	public void render(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTicks) {
		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTicks);
		this.renderTooltip(stack, pMouseX, pMouseY);
	}

	@Override
	protected void renderTooltip(MatrixStack pPoseStack, int pX, int pY) {
		if (this.isHovering(211, 48, 7, 87, pX, pY)) {
			List<ITextComponent> txt = new ArrayList<>(2);
			txt.add(new TranslationTextComponent("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.simPowerCap));
			CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
			if (cModel.getModel() != null) {
				txt.add(new TranslationTextComponent("hostilenetworks.gui.cost", cModel.getModel().getSimCost()));
			}
			this.renderWrappedToolTip(pPoseStack, txt, pX, pY, font);
		} else if (this.isHovering(14, 48, 7, 87, pX, pY)) {
			CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
			if (cModel.getModel() != null) {
				List<ITextComponent> txt = new ArrayList<>(1);
				txt.add(new TranslationTextComponent("hostilenetworks.gui.data", cModel.getData() - cModel.getTier().data, cModel.getTier().next().data - cModel.getTier().data));
				this.renderWrappedToolTip(pPoseStack, txt, pX, pY, font);
			}
		} else super.renderTooltip(pPoseStack, pX, pY);
	}

	@Override
	protected void renderLabels(MatrixStack stack, int pX, int pY) {
		int runtime = this.menu.getRuntime();
		if (runtime > 0) {
			int rTime = Math.min(99, MathHelper.ceil(100F * (300 - runtime) / 300));
			this.font.drawShadow(stack, rTime + "%", 184, 123, Color.AQUA);
		}
		CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
		if (cModel.getModel() != null) {
			int xOff = 18;
			String msg = I18n.get("hostilenetworks.gui.target");
			this.font.draw(stack, msg, xOff, 9, Color.WHITE);
			xOff += this.font.width(msg);
			this.font.draw(stack, cModel.getModel().getName(), xOff, 9, Color.LIME);

			xOff = 18;
			msg = I18n.get("hostilenetworks.gui.tier");
			this.font.draw(stack, msg, xOff, 9 + (this.font.lineHeight + 3), Color.WHITE);
			xOff += this.font.width(msg);
			msg = I18n.get("hostilenetworks.tier." + cModel.getTier().name);
			this.font.draw(stack, msg, xOff, 9 + (this.font.lineHeight + 3), cModel.getTier().color.getColor());

			xOff = 18;
			msg = I18n.get("hostilenetworks.gui.accuracy");
			this.font.draw(stack, msg, xOff, 9 + (this.font.lineHeight + 3) * 2, Color.WHITE);
			xOff += this.font.width(msg);
			DecimalFormat fmt = new DecimalFormat("##.##%");
			msg = fmt.format(cModel.getAccuracy());
			this.font.draw(stack, msg, xOff, 9 + (this.font.lineHeight + 3) * 2, cModel.getTier().color.getColor());
		}
		int left = 29;
		int top = 51;
		int spacing = this.font.lineHeight + 3;
		int idx = 0;
		for (TickableText t : this.body) {
			t.render(font, stack, left, top + spacing * idx);
			if (t.causesNewLine()) {
				idx++;
				left = 29;
			} else {
				left += t.getWidth(font);
			}
		}
	}

	@Override
	protected void renderBg(MatrixStack stack, float pPartialTicks, int pX, int pY) {
		Minecraft mc = Minecraft.getInstance();
		mc.getTextureManager().bind(BASE);

		int left = getGuiLeft();
		int top = getGuiTop();

		Screen.blit(stack, left + 8, top, 0, 0, 216, 141, 256, 256);
		Screen.blit(stack, left - 14, top, 0, 141, 18, 18, 256, 256);

		int energyHeight = 87 - MathHelper.ceil(87F * this.menu.getEnergyStored() / HostileConfig.simPowerCap);

		Screen.blit(stack, left + 211, top + 48, 18, 141, 7, energyHeight, 256, 256);

		int dataHeight = 87;
		CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
		if (cModel.getModel() != null) {
			int data = cModel.getData();
			ModelTier tier = cModel.getTier();
			ModelTier next = tier.next();
			if (tier == next) dataHeight = 0;
			else dataHeight = 87 - MathHelper.ceil(87F * (float) (data - tier.data) / (next.data - tier.data));
		}

		Screen.blit(stack, left + 14, top + 48, 18, 141, 7, dataHeight, 256, 256);

		mc.getTextureManager().bind(PLAYER);
		Screen.blit(stack, left + 28, top + 145, 0, 0, 176, 90, 256, 256);
	}

	private static final ITextComponent ERROR = new StringTextComponent("ERROR").withStyle(TextFormatting.OBFUSCATED);

	@Override
	public void tick() {
		if (this.menu.getFailState() != FailureState.NONE) {
			FailureState oState = this.lastFailState;
			this.lastFailState = this.menu.getFailState();
			if (oState != this.lastFailState) {
				this.body.clear();
				String[] msg = I18n.get(this.lastFailState.getKey()).split("\\n");
				if (this.lastFailState == FailureState.INPUT) {
					CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
					ITextComponent name = ERROR;
					if (cModel.getModel() != null) name = cModel.getModel().getInput().getHoverName();
					msg = I18n.get(this.lastFailState.getKey(), name.getString()).split("\\n");
				}
				for (String s : msg)
					this.body.add(new TickableText(s, Color.WHITE));
			}
			this.runtimeTextLoaded = false;
		} else if (!runtimeTextLoaded) {
			int ticks = 300 - this.menu.getRuntime();
			float speed = 0.65F;
			this.body.clear();
			int iters = DataModelItem.getIters(this.menu.getSlot(0).getItem());
			for (int i = 0; i < 7; i++) {
				TickableText txt = new TickableText(I18n.get("hostilenetworks.run." + i, iters), Color.WHITE, i != 0 && i != 5, speed);
				this.body.add(txt.setTicks(ticks));
				ticks = Math.max(0, ticks - txt.getMaxUsefulTicks());
				if (i == 0) {
					txt = new TickableText("v" + HostileNetworks.VERSION, TextFormatting.GOLD.getColor(), true, speed);
					this.body.add(txt.setTicks(ticks));
					ticks = Math.max(0, ticks - txt.getMaxUsefulTicks());
				} else if (i == 5) {
					String key = "hostilenetworks.color_text." + (this.menu.didPredictionSucceed() ? "success" : "failed");
					txt = new TickableText(I18n.get(key), (this.menu.didPredictionSucceed() ? TextFormatting.GOLD : TextFormatting.RED).getColor(), true, speed);
					this.body.add(txt.setTicks(ticks));
					ticks = Math.max(0, ticks - txt.getMaxUsefulTicks());
				}
			}
			this.runtimeTextLoaded = true;
			this.lastFailState = FailureState.NONE;
		}

		TickableText.tickList(this.body);
		if (this.menu.getRuntime() == 0) this.runtimeTextLoaded = false;
	}

}
