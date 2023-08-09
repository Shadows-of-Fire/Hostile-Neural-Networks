package dev.shadowsoffire.hostilenetworks.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joml.Quaternionf;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.client.WrappedRTBuffer;
import dev.shadowsoffire.hostilenetworks.data.CachedModel;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.ReflectionThings;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.screen.TickableText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class DeepLearnerScreen extends PlaceboContainerScreen<DeepLearnerContainer> {

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
    private int spin = 65;
    private int selectedModel = 0;
    private ImageButton btnLeft, btnRight;
    private int variant = 0;
    private int ticksShown = 0;

    public DeepLearnerScreen(DeepLearnerContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.setupEmptyText();
        Arrays.fill(models, CachedModel.EMPTY);
        this.minecraft = Minecraft.getInstance();
        pMenu.setNotifyCallback(slotId -> {
            ItemStack stack = pMenu.getSlot(slotId).getItem();
            CachedModel old = this.models[slotId];
            this.models[slotId] = new CachedModel(stack, slotId);
            if (!old.isValid() && this.models[slotId].isValid()) {
                if (++this.numModels == 1) {
                    this.selectedModel = slotId;
                    this.setupModel(this.getCurrentModel());
                    this.emptyText = false;
                }
            }
            else if (old.isValid() && !this.models[slotId].isValid()) {
                this.numModels--;
                if (this.numModels > 0 && slotId == this.selectedModel) this.selectLeft();
            }
            else if (slotId == this.selectedModel && this.models[this.selectedModel].isValid()) this.setupModel(this.models[this.selectedModel]);
        });
    }

    protected CachedModel getCurrentModel() {
        return this.models[this.selectedModel];
    }

    @Override
    public void init() {
        super.init();
        this.btnLeft = this.addRenderableWidget(new ImageButton(this.getGuiLeft() - 27, this.getGuiTop() + 105, 24, 24, 84, 140, 24, BASE, btn -> {
            this.selectLeft();
        }));

        this.btnRight = this.addRenderableWidget(new ImageButton(this.getGuiLeft() - 1, this.getGuiTop() + 105, 24, 24, 108, 140, 24, BASE, btn -> {
            this.selectRight();
        }));
    }

    public void selectLeft() {
        if (this.numModels == 0) return;
        int old = this.selectedModel;
        CachedModel model = this.models[this.clamp(this.selectedModel - 1)];
        while (!model.isValid())
            model = this.models[this.clamp(this.selectedModel - 1)];
        if (model.getSlot() != old) this.setupModel(model);
    }

    public void selectRight() {
        if (this.numModels == 0) return;
        int old = this.selectedModel;
        CachedModel model = this.models[this.clamp(this.selectedModel + 1)];
        while (!model.isValid())
            model = this.models[this.clamp(this.selectedModel + 1)];
        if (model.getSlot() != old) this.setupModel(model);
    }

    private int clamp(int idx) {
        if (idx == -1) idx = 3;
        if (idx == 4) idx = 0;
        return this.selectedModel = idx;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float pPartialTicks, int pX, int pY) {
        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        gfx.blit(BASE, left + 41, top, 0, 0, 256, 140);

        if (this.numModels > 0) {
            for (int i = 0; i < 3; i++) {
                gfx.blit(BASE, left + WIDTH - 49 - this.stats.getWidth(this.font), top + 8 + this.font.lineHeight + (this.font.lineHeight + 2) * i, 0, 140 + 9 * i, 9, 9);
            }

            gfx.blit(BASE, left - 41, top, 9, 140, 75, 101);

            CachedModel model = this.getCurrentModel();

            if (model.isValid()) {
                LivingEntity ent = model.getEntity(this.minecraft.level, this.variant);
                ent.yBodyRot = this.spin % 360;
                this.renderEntityInInventory(left - 4, top + 90, 40, 0, 0, ent);
            }

            for (int i = 0; i < 3; i++) {
                gfx.drawString(this.font, this.statArray[i], left + WIDTH - 36 - this.stats.getWidth(this.font), top + 9 + this.font.lineHeight + (this.font.lineHeight + 2) * i, Color.WHITE);
            }

        }

        gfx.blit(PLAYER, left + 81, top + 145, 0, 0, 176, 90);
        if (this.numModels <= 1) {
            this.btnLeft.visible = false;
            this.btnRight.visible = false;
        }
        else {
            this.btnLeft.visible = true;
            this.btnRight.visible = true;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int pX, int pY) {
        int left = 49;
        int top = 6;
        int spacing = this.font.lineHeight + 3;
        int idx = 0;
        for (TickableText t : this.texts) {
            t.render(this.font, gfx, left, top + spacing * idx);
            if (t.causesNewLine()) {
                idx++;
                left = 49;
            }
            else {
                left += t.getWidth(this.font);
            }
        }
        if (this.numModels > 0) {
            this.stats.render(this.font, gfx, WIDTH - 49 - this.stats.getWidth(this.font), top);
        }
    }

    @Override
    public void containerTick() {
        if (!this.menu.hasModels()) {
            if (!this.emptyText) {
                this.setupEmptyText();
                this.emptyText = true;
            }
        }
        else {
            if (this.emptyText) {
                for (int i = 0; i < 4; i++) {
                    if (this.models[i].isValid()) {
                        this.setupModel(this.models[i]);
                        this.selectedModel = i;
                        this.emptyText = false;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < this.texts.size(); i++) {
            TickableText txt = this.texts.get(i);
            if (!txt.isDone()) {
                txt.tick();
                break;
            }
        }

        this.stats.tick();

        this.spin++;
        if (++this.ticksShown % 80 == 0) this.nextVariant();
    }

    private void nextVariant() {
        CachedModel current = this.getCurrentModel();
        if (!current.isValid()) return;
        int variants = current.getModel().getSubtypes().size();
        if (variants == 0) return;

        this.variant = (this.variant + 1) % (variants + 1);

        LivingEntity entity = current.getEntity(this.minecraft.level, this.variant);
        if (this.variant == 0) {
            this.texts.set(1, new TickableText(entity.getType().getDescription().getString(), Color.WHITE, true, 2).setTicks(9999));
        }
        else {
            this.texts.set(1, new TickableText(I18n.get("hostilenetworks.gui.variant", entity.getType().getDescription().getString()), Color.LIME, true, 2).setTicks(9999));
        }
    }

    private void setupEmptyText() {
        this.resetText();
        for (int i = 0; i < 7; i++) {
            this.addText(I18n.get("hostilenetworks.gui.learner_empty." + i), i == 0 ? Color.AQUA : Color.WHITE);
        }
    }

    private static DecimalFormat fmt = new DecimalFormat("##.##%");

    private void setupModel(CachedModel cache) {
        if (!cache.isValid()) return;
        DataModel model = cache.getModel();
        this.ticksShown = 0;
        this.variant = 0;
        this.resetText();
        this.addText(I18n.get("hostilenetworks.gui.name"), Color.AQUA);
        this.addText(cache.getEntity(this.minecraft.level).getType().getDescription().getString(), Color.WHITE);
        this.addText(I18n.get("hostilenetworks.gui.info"), Color.AQUA);
        String[] trivia = I18n.get(model.getTriviaKey()).split("\\n");
        for (int i = 0; i < Math.min(4, trivia.length); i++) {
            this.addText(trivia[i], Color.WHITE);
        }
        for (int i = trivia.length; i < 5; i++) {
            this.addText("", 0);
        }
        this.addText(I18n.get("hostilenetworks.gui.tier"), Color.WHITE, false);
        ModelTier tier = cache.getTier();
        ModelTier next = tier.next();
        this.addText(I18n.get("hostilenetworks.tier." + tier.name), tier.color.getColor());
        this.addText(I18n.get("hostilenetworks.gui.accuracy"), Color.WHITE, false);
        this.addText(fmt.format(cache.getAccuracy()), tier.color.getColor());
        if (tier != next) {
            this.addText(I18n.get("hostilenetworks.gui.next_tier"), Color.WHITE, false);
            this.addText(I18n.get("hostilenetworks.tier." + next.name), next.color.getColor(), false);
            this.addText(I18n.get("hostilenetworks.gui.next_tier2", cache.getKillsNeeded()), Color.WHITE, false);
            this.addText(I18n.get("hostilenetworks.gui.kill" + (cache.getKillsNeeded() > 1 ? "s" : "")), Color.WHITE);
        }
        else {
            this.addText(I18n.get("hostilenetworks.gui.max_tier"), ChatFormatting.RED.getColor());
        }

        LivingEntity ent = cache.getEntity(this.minecraft.level);

        if (ent == null) {
            for (int i = 0; i < 3; i++)
                this.statArray[i] = "\u00A7k99999";
        }

        this.statArray[0] = String.valueOf((int) (ent.getAttribute(Attributes.MAX_HEALTH).getBaseValue() / 2));
        this.statArray[1] = String.valueOf((int) (ent.getAttribute(Attributes.ARMOR).getBaseValue() / 2));
        this.statArray[2] = String.valueOf(ReflectionThings.getExperienceReward(ent));
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
        float f1 = (float) Math.atan(pMouseY / 40.0F);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate(pPosX, pPosY, 1050.0F);
        posestack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack matrixstack = new PoseStack();
        matrixstack.translate(0.0D, 0.0D, 1000.0D);
        DataModel model = this.getCurrentModel().getModel();

        pScale *= model.getScale();

        matrixstack.scale(pScale, pScale, pScale);
        Quaternionf quaternion = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternion1 = Axis.XP.rotationDegrees(f1 * 20.0F);
        quaternion.mul(quaternion1);
        matrixstack.mulPose(quaternion);
        matrixstack.mulPose(Axis.YP.rotationDegrees((this.spin + this.minecraft.getDeltaFrameTime()) * 2.25F % 360));
        pLivingEntity.setYRot(0);
        pLivingEntity.yBodyRot = pLivingEntity.getYRot();
        pLivingEntity.yHeadRot = pLivingEntity.getYRot();
        pLivingEntity.yHeadRotO = pLivingEntity.getYRot();
        EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conjugate();
        entityrenderermanager.overrideCameraOrientation(quaternion1);
        entityrenderermanager.setRenderShadow(false);
        MultiBufferSource.BufferSource rtBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            entityrenderermanager.render(pLivingEntity, model.getXOffset(), model.getYOffset(), model.getZOffset(), 0.0F, 1, matrixstack, new WrappedRTBuffer(rtBuffer), 15728880);
        });
        rtBuffer.endBatch();
        entityrenderermanager.setRenderShadow(true);
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

}
