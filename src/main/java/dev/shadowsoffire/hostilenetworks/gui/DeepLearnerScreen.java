package dev.shadowsoffire.hostilenetworks.gui;

import java.util.Arrays;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.client.ghost.GhostEntityPipRenderer;
import dev.shadowsoffire.hostilenetworks.client.ghost.GhostEntityRenderState;
import dev.shadowsoffire.hostilenetworks.client.ghost.GhostEntityStates;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.screen.TickableTextList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class DeepLearnerScreen extends PlaceboContainerScreen<DeepLearnerMenu> {

    public static final int WIDTH = 338;
    public static final int HEIGHT = 235;
    public static final int MAX_TEXT_WIDTH = 200;
    private static final int GHOST_ALPHA = 0xBB;

    public static final Identifier BASE = HostileNetworks.loc("textures/gui/deep_learner.png");
    public static final Identifier PLAYER = HostileNetworks.loc("textures/gui/default_gui.png");
    public static final WidgetSprites LEFT_BUTTON = makeSprites("widget/left", "widget/left_hovered");
    public static final WidgetSprites RIGHT_BUTTON = makeSprites("widget/right", "widget/right_hovered");

    private TickableTextList mainText;
    private TickableTextList infoText;
    private TickableTextList dataText;
    private TickableTextList stats;
    private final Component[] statArray = new Component[3];
    private int numModels = 0;
    private boolean emptyText = true;
    private DataModelInstance[] models = new DataModelInstance[4];
    private int spin = 65;
    private int selectedModel = 0;
    private ImageButton btnLeft, btnRight;
    private int variant = 0;
    private int ticksShown = 0;
    /** Number of wrapped lines the current name line occupies. Used to position {@link #infoText} directly beneath it. */
    private int nameLines = 1;

    public DeepLearnerScreen(DeepLearnerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, WIDTH, HEIGHT);
        Arrays.fill(models, DataModelInstance.EMPTY);
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
        this.btnLeft = this.addRenderableWidget(new ImageButton(this.getLeftPos() - 27, this.getTopPos() + 105, 24, 24, LEFT_BUTTON, btn -> {
            this.selectLeft();
        }));

        this.btnRight = this.addRenderableWidget(new ImageButton(this.getLeftPos() - 1, this.getTopPos() + 105, 24, 24, RIGHT_BUTTON, btn -> {
            this.selectRight();
        }));

        this.stats = new TickableTextList(this.minecraft.font, 100);
        this.stats.addLine(Component.translatable("hostilenetworks.gui.stats").withColor(Color.AQUA));

        // mainText (name) and infoText (trivia) are separate lists so that re-setting the name on a variant change
        // cannot redistribute the shared reveal budget and visibly un-wind the trivia text.
        int textWidth = MAX_TEXT_WIDTH - this.stats.getWidth() + 36;
        this.mainText = new TickableTextList(this.minecraft.font, textWidth);
        this.infoText = new TickableTextList(this.minecraft.font, textWidth);
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
    public void extractBackground(GuiGraphicsExtractor gfx, int pX, int pY, float pPartialTicks) {
        super.extractBackground(gfx, pX, pY, pPartialTicks);
        int left = this.getLeftPos();
        int top = this.getTopPos();
        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + 41, top, 0F, 0F, 256, 140, 256, 256);

        if (this.numModels > 0) {
            DataModelInstance inst = this.getCurrentModel();
            // Each model type that exposes icon-based stats occupies its own icon column in the texture (entities at U=0,
            // blocks at U=96). A column of -1 means the model renders its stats as plain text lines in the stats list.
            int iconColumn = inst.isValid() ? inst.getModel().statIconColumn() : -1;

            if (iconColumn >= 0) {
                for (int i = 0; i < 3; i++) {
                    gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + WIDTH - 49 - this.stats.getWidth(), top + 8 + this.font.lineHeight + (this.font.lineHeight + 2) * i, (float) iconColumn, (float) (140 + 9 * i), 9, 9, 256, 256);
                }
            }

            gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left - 41, top, 9F, 140F, 75, 101, 256, 256);

            if (inst.isValid()) {
                DisplayEntity display = inst.getDisplayEntity(this.minecraft.level, this.variant);
                Entity ent = ClientEntityCache.computeIfAbsent(display, this.minecraft.level);
                if (ent != null) this.submitGhostEntity(gfx, left, top, ent, display, pPartialTicks);
            }

            if (iconColumn >= 0) {
                for (int i = 0; i < 3; i++) {
                    gfx.text(this.font, this.statArray[i], left + WIDTH - 36 - this.stats.getWidth(), top + 9 + this.font.lineHeight + (this.font.lineHeight + 2) * i, (0xFF000000 | Color.WHITE));
                }
            }
        }

        gfx.blit(RenderPipelines.GUI_TEXTURED, PLAYER, left + 81, top + 145, 0F, 0F, 176, 90, 256, 256);
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
    protected void extractLabels(GuiGraphicsExtractor gfx, int pX, int pY) {
        int left = 49;
        int top = 6;
        this.mainText.render(gfx, left, top);
        // infoText sits directly below the name line(s): one line for the "Name" header plus however many the name wraps to.
        this.infoText.render(gfx, left, top + (font.lineHeight + 3) * (1 + this.nameLines));
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
        this.infoText.tick();
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
        Component name = this.variant == 0
            ? entity.getName()
            : Component.translatable("hostilenetworks.gui.variant", entity.getName()).withColor(Color.LIME);
        this.mainText.setLine(1, name, 2);
        this.nameLines = this.minecraft.font.split(name, this.mainText.getMaxWidth()).size();
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
        Component name = inst.getEntity(this.minecraft.level).getName();
        this.mainText.addLine(name);
        this.nameLines = this.minecraft.font.split(name, this.mainText.getMaxWidth()).size();
        this.infoText.addLine(Component.translatable("hostilenetworks.gui.info").withColor(Color.AQUA));
        this.infoText.addLine(Component.translatable(model.triviaKey()));

        ModelTier tier = inst.getTier();
        ModelTier next = ModelTierRegistry.next(tier);
        Component tierName = Component.translatable("hostilenetworks.tier." + tier.name()).withColor(tier.colorValue());
        this.dataText.addLine(Component.translatable("hostilenetworks.gui.tier", tierName));

        this.dataText.addLine(inst.getAccuracyComponent());

        if (!tier.isMax()) {
            if (HostileConfig.actionUpgradesModel) {
                Component nextTierName = Component.translatable("hostilenetworks.tier." + next.name()).withColor(next.colorValue());
                int actions = inst.getActionsNeeded();
                String actionKey = actions > 1 ? model.actionWordKey() + "s" : model.actionWordKey();
                Component actionWord = Component.translatable(actionKey);

                this.dataText.addLine(Component.translatable("hostilenetworks.gui.next_tier", nextTierName, actions, actionWord));
            }
            else {
                this.dataText.addLine(Component.translatable("hostilenetworks.gui.upgrade_disabled"));
            }
        }
        else {
            this.dataText.addLine(Component.translatable("hostilenetworks.gui.max_tier").withStyle(ChatFormatting.RED));
        }

        List<Component> statistics = model.getStatistics(this.minecraft.level);
        for (int i = 0; i < 3; i++) {
            this.statArray[i] = statistics.get(i);
        }
    }

    private void resetText() {
        this.mainText.clear();
        this.infoText.clear();
        this.dataText.clear();
        this.stats.clear();
        this.stats.addLine(Component.translatable("hostilenetworks.gui.stats").withColor(Color.AQUA));
    }

    /**
     * Submits the display entity as a spinning, translucent ghost inside the 75x101 entity panel at (left - 41, top).
     * <p>
     * Replaces the 1.21 {@code renderEntityInInventory}: the entity is drawn into a picture-in-picture texture by
     * {@link GhostEntityPipRenderer}, whose origin is the center of the panel. The 1.21 code placed the entity's feet at
     * {@code (left - 4, top + 90)} with 40 pixels per block.
     */
    private void submitGhostEntity(GuiGraphicsExtractor gfx, int left, int top, Entity entity, DisplayEntity display, float partialTicks) {
        EntityRenderState state = GhostEntityStates.extract(entity, partialTicks);
        if (state == null) return;

        int x0 = left - 41, y0 = top, x1 = left + 34, y1 = top + 101;
        float scale = 40F * display.scale();
        float feetX = left - 4, feetY = top + 90;
        float centerX = (x0 + x1) / 2F, centerY = (y0 + y1) / 2F;
        Vector3f translation = new Vector3f((feetX - centerX) / scale, (feetY - centerY) / scale, 0);

        float spinDeg = (this.spin + partialTicks) * 2.25F % 360;
        Quaternionf rotation = new Quaternionf().rotationZ((float) Math.PI).rotateY(spinDeg * Mth.DEG_TO_RAD);
        Vector3f offset = new Vector3f(display.xOffset(), display.yOffset(), display.zOffset());
        gfx.submitPictureInPictureRenderState(new GhostEntityRenderState(state, translation, rotation, null, offset, GHOST_ALPHA, x0, y0, x1, y1, scale, gfx.peekScissorStack()));
    }

    public static WidgetSprites makeSprites(String base, String hovered) {
        return new WidgetSprites(HostileNetworks.loc(base), HostileNetworks.loc(base), HostileNetworks.loc(hovered), HostileNetworks.loc(hovered));
    }

}
