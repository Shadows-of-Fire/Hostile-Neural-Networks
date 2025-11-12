package dev.shadowsoffire.hostilenetworks.gui;

import java.util.Arrays;

import org.joml.Quaternionf;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.client.WrappedRTBuffer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.hostilenetworks.util.ReflectionThings;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.screen.TickableTextList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DeepLearnerScreen extends PlaceboContainerScreen<DeepLearnerContainer> {

    public static final int WIDTH = 338;
    public static final int HEIGHT = 235;
    public static final int MAX_TEXT_WIDTH = 200;

    public static final ResourceLocation BASE = HostileNetworks.loc("textures/gui/deep_learner.png");
    public static final ResourceLocation PLAYER = HostileNetworks.loc("textures/gui/default_gui.png");
    public static final WidgetSprites LEFT_BUTTON = makeSprites("widget/left", "widget/left_hovered");
    public static final WidgetSprites RIGHT_BUTTON = makeSprites("widget/right", "widget/right_hovered");

    private TickableTextList mainText;
    private TickableTextList dataText;
    private TickableTextList stats;
    private final String[] statArray = new String[3];
    private int numModels = 0;
    private boolean emptyText = true;
    private DataModelInstance[] models = new DataModelInstance[4];
    private int spin = 65;
    private int selectedModel = 0;
    private ImageButton btnLeft, btnRight;
    private int variant = 0;
    private int ticksShown = 0;

    public DeepLearnerScreen(DeepLearnerContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        Arrays.fill(models, DataModelInstance.EMPTY);
        this.minecraft = Minecraft.getInstance();
        pMenu.setNotifyCallback(slotId -> {
            ItemStack stack = pMenu.getSlot(slotId).getItem();
            DataModelInstance old = this.models[slotId];
            this.models[slotId] = new DataModelInstance(stack, slotId);
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
            else if (slotId == this.selectedModel && this.models[this.selectedModel].isValid()) {
                this.setupModel(this.models[this.selectedModel]);
            }
        });
    }

    protected DataModelInstance getCurrentModel() {
        return this.models[this.selectedModel];
    }

    @Override
    public void init() {
        super.init();
        this.btnLeft = this.addRenderableWidget(new ImageButton(this.getGuiLeft() - 27, this.getGuiTop() + 105, 24, 24, LEFT_BUTTON, btn -> {
            this.selectLeft();
        }));

        this.btnRight = this.addRenderableWidget(new ImageButton(this.getGuiLeft() - 1, this.getGuiTop() + 105, 24, 24, RIGHT_BUTTON, btn -> {
            this.selectRight();
        }));

        this.stats = new TickableTextList(this.minecraft.font, 100);
        this.stats.addLine(Component.translatable("hostilenetworks.gui.stats").withColor(Color.AQUA));

        this.mainText = new TickableTextList(this.minecraft.font, MAX_TEXT_WIDTH - this.stats.getWidth() + 36);
        this.dataText = new TickableTextList(this.minecraft.font, MAX_TEXT_WIDTH);

        this.setupEmptyText();
        this.containerTick();
    }

    public void selectLeft() {
        if (this.numModels == 0) return;
        int old = this.selectedModel;
        DataModelInstance model = this.models[this.clamp(this.selectedModel - 1)];
        while (!model.isValid())
            model = this.models[this.clamp(this.selectedModel - 1)];
        if (model.getSlot() != old) this.setupModel(model);
    }

    public void selectRight() {
        if (this.numModels == 0) return;
        int old = this.selectedModel;
        DataModelInstance model = this.models[this.clamp(this.selectedModel + 1)];
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
                gfx.blit(BASE, left + WIDTH - 49 - this.stats.getWidth(), top + 8 + this.font.lineHeight + (this.font.lineHeight + 2) * i, 0, 140 + 9 * i, 9, 9);
            }

            gfx.blit(BASE, left - 41, top, 9, 140, 75, 101);

            DataModelInstance inst = this.getCurrentModel();

            if (inst.isValid()) {
                DisplayEntity display = inst.getDisplayEntity(this.minecraft.level, this.variant);
                Entity ent = ClientEntityCache.computeIfAbsent(display, this.minecraft.level);
                if (ent instanceof LivingEntity living) {
                    living.yBodyRot = this.spin % 360;
                }
                this.renderEntityInInventory(gfx, left - 4, top + 90, 40, 0, 0, ent, display);
            }

            for (int i = 0; i < 3; i++) {
                gfx.drawString(this.font, this.statArray[i], left + WIDTH - 36 - this.stats.getWidth(), top + 9 + this.font.lineHeight + (this.font.lineHeight + 2) * i, Color.WHITE);
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
        this.mainText.render(gfx, left, top);
        this.dataText.render(gfx, left, top + (font.lineHeight + 3) * 8);
        if (this.numModels > 0) {
            this.stats.render(gfx, WIDTH - 49 - this.stats.getWidth(), top);
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

        this.mainText.tick();
        this.dataText.tick();

        this.stats.tick();

        this.spin++;
        if (++this.ticksShown % 80 == 0) this.nextVariant();
    }

    private void nextVariant() {
        DataModelInstance current = this.getCurrentModel();
        if (!current.isValid()) return;
        int variants = current.getModel().displayVariants(this.minecraft.level).size();
        if (variants == 0) return;

        this.variant = (this.variant + 1) % (variants + 1);

        Entity entity = current.getEntity(this.minecraft.level, this.variant);
        if (this.variant == 0) {
            this.mainText.setLine(1, entity.getName(), 2);
        }
        else {
            this.mainText.setLine(1, Component.translatable("hostilenetworks.gui.variant", entity.getName()).withColor(Color.LIME), 2);
        }
    }

    /**
     * Clears all texts and generates the empty text lines.
     */
    private void setupEmptyText() {
        this.resetText();
        for (int i = 0; i < 7; i++) {
            this.mainText.addLine(Component.translatable("hostilenetworks.gui.learner_empty." + i).withColor(i == 0 ? Color.AQUA : Color.WHITE));
        }
        this.emptyText = true;
    }

    private void setupModel(DataModelInstance inst) {
        if (!inst.isValid()) return;
        DataModel model = inst.getModel();
        this.ticksShown = 0;
        this.variant = 0;
        this.resetText();
        this.mainText.addLine(Component.translatable("hostilenetworks.gui.name").withColor(Color.AQUA));
        this.mainText.addLine(inst.getEntity(this.minecraft.level).getName());
        this.mainText.addLine(Component.translatable("hostilenetworks.gui.info").withColor(Color.AQUA));
        this.mainText.addLine(Component.translatable(model.triviaKey()));

        ModelTier tier = inst.getTier();
        ModelTier next = ModelTierRegistry.next(tier);
        Component tierName = Component.translatable("hostilenetworks.tier." + tier.name()).withColor(tier.colorValue());
        this.dataText.addLine(Component.translatable("hostilenetworks.gui.tier", tierName));

        this.dataText.addLine(inst.getAccuracyComponent());

        if (!tier.isMax()) {
            if (HostileConfig.killModelUpgrade) {
                Component nextTierName = Component.translatable("hostilenetworks.tier." + next.name()).withColor(next.colorValue());
                Component killWord = Component.translatable("hostilenetworks.gui.kill" + (inst.getKillsNeeded() > 1 ? "s" : ""));

                this.dataText.addLine(Component.translatable("hostilenetworks.gui.next_tier", nextTierName, inst.getKillsNeeded(), killWord));
            }
            else {
                this.dataText.addLine(Component.translatable("hostilenetworks.gui.upgrade_disabled"));
            }
        }
        else {
            this.dataText.addLine(Component.translatable("hostilenetworks.gui.max_tier").withStyle(ChatFormatting.RED));
        }

        Entity ent = inst.getEntity(this.minecraft.level);

        if (ent instanceof LivingEntity living) {
            this.statArray[0] = String.valueOf((int) (living.getAttribute(Attributes.MAX_HEALTH).getBaseValue() / 2));
            this.statArray[1] = String.valueOf((int) (living.getAttribute(Attributes.ARMOR).getBaseValue() / 2));
            this.statArray[2] = String.valueOf(ReflectionThings.getBaseExperienceReward(living));
        }
        else {
            for (int i = 0; i < 3; i++) {
                this.statArray[i] = "\u00A7k99999";
            }
        }
    }

    private void resetText() {
        this.mainText.clear();
        this.dataText.clear();
        this.stats.setTicks(0);
    }

    @SuppressWarnings("deprecation")
    public void renderEntityInInventory(GuiGraphics gfx, float pPosX, float pPosY, float scale, float pMouseX, float pMouseY, Entity entity, DisplayEntity display) {
        float partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float mouseAtan = (float) Math.atan(pMouseY / 40.0F);
        PoseStack pose = gfx.pose();
        pose.pushPose();
        scale *= display.scale();

        pose.translate(pPosX, pPosY, 50.0F); // Mirrors magic z value used by InventoryScreen#renderEntityInInventory
        pose.scale(scale, scale, -scale);

        Quaternionf quaternion = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternion1 = Axis.XP.rotationDegrees(mouseAtan * 20.0F);
        quaternion.mul(quaternion1);
        pose.mulPose(quaternion);
        pose.mulPose(Axis.YP.rotationDegrees((this.spin + partialTicks) * 2.25F % 360));
        entity.setYRot(0);
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = entity.getYRot();
            living.yHeadRot = entity.getYRot();
            living.yHeadRotO = entity.getYRot();
        }

        // When rendering an item entity, we want to prevent any bobbing or spinning from occurring.
        // To do that, we have to apply the inverse transforms that would normally be applied so when the real ones apply (in ItemEntityRenderer), they cancel out.
        if (entity instanceof ItemEntity item) {
            ItemStack itemstack = item.getItem();
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            BakedModel bakedmodel = itemRenderer.getModel(itemstack, entity.level(), null, entity.getId());

            boolean shouldBob = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemstack).shouldBobAsEntity(itemstack);
            float f1 = shouldBob ? Mth.sin(((float) item.getAge() + partialTicks) / 10.0F + item.bobOffs) * 0.1F + 0.1F : 0;
            float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();

            float f3 = item.getSpin(partialTicks);
            pose.mulPose(Axis.YP.rotation(-f3));

            pose.translate(0.0F, -(f1 + 0.25F * f2), 0.0F);
        }

        EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conjugate();
        entityrenderermanager.overrideCameraOrientation(quaternion1);
        entityrenderermanager.setRenderShadow(false);
        MultiBufferSource.BufferSource rtBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            entityrenderermanager.render(entity, display.xOffset(), display.yOffset(), display.zOffset(), 0.0F, partialTicks, pose, new WrappedRTBuffer(rtBuffer), 0xF000F0);
        });
        rtBuffer.endBatch();
        entityrenderermanager.setRenderShadow(true);
        pose.popPose();
        Lighting.setupFor3DItems();
    }

    public static WidgetSprites makeSprites(String base, String hovered) {
        return new WidgetSprites(HostileNetworks.loc(base), HostileNetworks.loc(base), HostileNetworks.loc(hovered), HostileNetworks.loc(hovered));
    }

}
