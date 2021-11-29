package shadows.hostilenetworks.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.client.WeirdRenderThings;
import shadows.hostilenetworks.client.WrappedRTBuffer;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.util.Color;
import shadows.hostilenetworks.util.ReflectionThings;
import shadows.hostilenetworks.util.TickableText;

public class DeepLearnerScreen extends ContainerScreen<DeepLearnerContainer> {

	public static final int WIDTH = 338;
	public static final int HEIGHT = 235;
	private static final ResourceLocation BASE = new ResourceLocation(HostileNetworks.MODID, "textures/gui/deep_learner.png");
	private static final ResourceLocation PLAYER = new ResourceLocation(HostileNetworks.MODID, "textures/gui/default_gui.png");

	private List<TickableText> texts = new ArrayList<>();
	private TickableText stats = new TickableText(I18n.get("hostilenetworks.gui.stats"), Color.AQUA);
	private final String[] statArray = new String[3];
	private int numModels = 0;
	private boolean emptyText = true;
	private CachedModel[] models = new CachedModel[4];
	private int spin = 0;
	private int selectedModel = 0;

	public DeepLearnerScreen(DeepLearnerContainer pMenu, PlayerInventory pPlayerInventory, ITextComponent pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		this.imageWidth = WIDTH;
		this.imageHeight = HEIGHT;
		this.setupEmptyText();
		pMenu.setNotifyCallback(slotId -> {
			ItemStack stack = pMenu.getSlot(slotId).getItem();
			CachedModel old = models[slotId];
			models[slotId] = stack.isEmpty() ? null : new CachedModel(stack, slotId);
			if (old == null && models[slotId] != null) {
				if (++numModels == 1) {
					selectedModel = slotId;
					setupModel(models[selectedModel]);
					emptyText = false;
				}
			} else if (old != null && models[slotId] == null) {
				numModels--;
				if (numModels > 0 && slotId == selectedModel) selectLeft();
			} else if (slotId == selectedModel && models[selectedModel] != null) setupModel(models[selectedModel]);
		});
	}

	@Override
	public void init(Minecraft pMinecraft, int pWidth, int pHeight) {
		super.init(pMinecraft, pWidth, pHeight);
		this.addButton(new ImageButton(this.getGuiLeft() - 27, this.getGuiTop() + 105, 24, 24, 84, 140, 24, BASE, btn -> {
			selectLeft();
		}));

		this.addButton(new ImageButton(this.getGuiLeft() - 1, this.getGuiTop() + 105, 24, 24, 108, 140, 24, BASE, btn -> {
			selectRight();
		}));
	}

	public void selectLeft() {
		if (numModels == 0) return;
		int old = selectedModel;
		CachedModel model = models[clamp(selectedModel - 1)];
		while (model == null)
			model = models[clamp(selectedModel - 1)];
		if (model.getSlot() != old) setupModel(model);
	}

	public void selectRight() {
		if (numModels == 0) return;
		int old = selectedModel;
		CachedModel model = models[clamp(selectedModel + 1)];
		while (model == null)
			model = models[clamp(selectedModel + 1)];
		if (model.getSlot() != old) setupModel(model);
	}

	private int clamp(int idx) {
		if (idx == -1) idx = 3;
		if (idx == 4) idx = 0;
		return selectedModel = idx;
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

		if (numModels > 0) {
			for (int i = 0; i < 3; i++) {
				this.blit(matrix, left + WIDTH - 49 - this.stats.getWidth(font), top + 8 + font.lineHeight + (font.lineHeight + 2) * i, 0, 140 + 9 * i, 9, 9);
			}

			this.blit(matrix, left - 41, top, 9, 140, 75, 101);

			LivingEntity ent = models[selectedModel].getEntity(minecraft.level);

			ent.yBodyRot = spin % 360;
			renderEntityInInventory(left - 4, top + 90, 40, 0, 0, ent);

			for (int i = 0; i < 3; i++) {
				this.font.draw(matrix, statArray[i], left + WIDTH - 36 - this.stats.getWidth(font), top + 9 + font.lineHeight + (font.lineHeight + 2) * i, Color.WHITE);
			}

		}

		this.getMinecraft().getTextureManager().bind(PLAYER);
		this.blit(matrix, left + 81, top + 145, 0, 0, 176, 90);
		if (numModels <= 1) {
			this.buttons.get(0).visible = false;
			this.buttons.get(1).visible = false;
		} else {
			this.buttons.get(0).visible = true;
			this.buttons.get(1).visible = true;
		}
	}

	@Override
	protected void renderLabels(MatrixStack stack, int pX, int pY) {
		int left = 49;
		int top = 8;
		int spacing = this.font.lineHeight + 3;
		int idx = 0;
		for (TickableText t : texts) {
			t.render(font, stack, left, top + spacing * idx);
			if (t.causesNewLine()) {
				idx++;
				left = 49;
			} else {
				left += t.getWidth(font);
			}
		}
		if (this.numModels > 0) {
			this.stats.render(font, stack, WIDTH - 49 - this.stats.getWidth(font), top);
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.menu.hasModels()) {
			if (!emptyText) {
				setupEmptyText();
				emptyText = true;
			}
		} else {
			if (emptyText) {
				for (int i = 0; i < 4; i++) {
					if (models[i] != null) {
						setupModel(models[i]);
						selectedModel = i;
						emptyText = false;
						break;
					}
				}
			}
		}

		for (int i = 0; i < texts.size(); i++) {
			TickableText txt = texts.get(i);
			if (!txt.isDone()) {
				txt.tick();
				break;
			}
		}

		this.stats.tick();

		spin++;
	}

	private void setupEmptyText() {
		resetText();
		for (int i = 0; i < 7; i++) {
			addText(I18n.get("hostilenetworks.gui.learner_empty." + i), i == 0 ? Color.AQUA : Color.WHITE);
		}
	}

	private void setupModel(CachedModel cache) {
		DataModel model = cache.getModel();
		if (model == null) {

			return;
		}
		resetText();
		addText(I18n.get("hostilenetworks.gui.name"), Color.AQUA);
		addText(model.getName().getString(), Color.WHITE);
		addText(I18n.get("hostilenetworks.gui.info"), Color.AQUA);
		List<TranslationTextComponent> trivia = model.getTrivia();
		trivia.forEach(t -> addText(t.getString(), Color.WHITE));
		for (int i = trivia.size(); i < 4; i++) {
			addText("", 0);
		}
		addText(I18n.get("hostilenetworks.gui.tier"), Color.WHITE, false);
		ModelTier tier = cache.getTier();
		ModelTier next = tier.next();
		addText(I18n.get("hostilenetworks.tier." + tier.name), tier.color.getColor());
		addText(I18n.get("hostilenetworks.gui.accuracy"), Color.WHITE, false);
		addText(new DecimalFormat("##.##%").format(cache.getAccuracy()), tier.color.getColor());
		if (tier != next) {
			addText(I18n.get("hostilenetworks.gui.next_tier"), Color.WHITE, false);
			addText(I18n.get("hostilenetworks.tier." + next.name), next.color.getColor(), false);
			addText(I18n.get("hostilenetworks.gui.next_tier2", cache.getKillsNeeded()), Color.WHITE, false);
			addText(I18n.get("hostilenetworks.gui.kill" + (cache.getKillsNeeded() > 1 ? "s" : "")), Color.WHITE);
		} else {
			addText(I18n.get("hostilenetworks.gui.max_tier"), Color.WHITE);
		}

		LivingEntity ent = cache.getEntity(this.minecraft.level);

		if (ent == null) {
			for (int i = 0; i < 3; i++)
				statArray[i] = "\u00A7k99999";
		}

		statArray[0] = String.valueOf((int) (ent.getAttribute(Attributes.MAX_HEALTH).getBaseValue() / 2));
		statArray[1] = String.valueOf((int) (ent.getAttribute(Attributes.ARMOR).getBaseValue() / 2));
		statArray[2] = String.valueOf(ReflectionThings.getExperienceReward(ent, this.inventory.player));
	}

	private void addText(String msg, int color) {
		this.texts.add(new TickableText(msg, color, true, 2));
	}

	private void addText(String msg, int color, boolean newline) {
		this.texts.add(new TickableText(msg, color, newline, 2));
	}

	private void resetText() {
		this.texts.clear();
		this.stats.reset();
	}

	@SuppressWarnings("deprecation")
	public void renderEntityInInventory(float pPosX, float pPosY, float pScale, float pMouseX, float pMouseY, LivingEntity pLivingEntity) {
		float f1 = (float) Math.atan((double) (pMouseY / 40.0F));
		RenderSystem.pushMatrix();
		RenderSystem.translatef(pPosX, pPosY, 1050.0F);
		RenderSystem.scalef(1.0F, 1.0F, -1.0F);
		MatrixStack matrixstack = new MatrixStack();
		matrixstack.translate(0.0D, 0.0D, 1000.0D);

		pScale *= this.models[selectedModel].getModel().getScale();

		matrixstack.scale(pScale, pScale, pScale);
		Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
		Quaternion quaternion1 = Vector3f.XP.rotationDegrees(f1 * 20.0F);
		quaternion.mul(quaternion1);
		matrixstack.mulPose(quaternion);
		pLivingEntity.yRot = ((spin + this.minecraft.getDeltaFrameTime()) * 2.25F) % 360;
		pLivingEntity.yBodyRot = pLivingEntity.yRot;
		pLivingEntity.yHeadRot = pLivingEntity.yRot;
		pLivingEntity.yHeadRotO = pLivingEntity.yRot;
		EntityRendererManager entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
		quaternion1.conj();
		entityrenderermanager.overrideCameraOrientation(quaternion1);
		entityrenderermanager.setRenderShadow(false);
		IRenderTypeBuffer.Impl rtBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
		WeirdRenderThings.fullbright = true;
		RenderSystem.runAsFancy(() -> {
			entityrenderermanager.render(pLivingEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1, matrixstack, new WrappedRTBuffer(rtBuffer), 15728880);
		});
		rtBuffer.endBatch();
		WeirdRenderThings.fullbright = false;
		entityrenderermanager.setRenderShadow(true);
		RenderSystem.popMatrix();
	}

}
