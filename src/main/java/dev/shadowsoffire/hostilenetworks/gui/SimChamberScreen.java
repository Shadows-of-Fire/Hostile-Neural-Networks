package dev.shadowsoffire.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.SimMode;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.screen.TickableTextList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SimChamberScreen extends PlaceboContainerScreen<SimChamberMenu> {

    public static final int WIDTH = 232;
    public static final int HEIGHT = 230;
    public static final int MAX_TEXT_WIDTH = 174;
    public static final float RUNTIME_TEXT_SPEED = 0.65F;

    private static final Identifier BASE = HostileNetworks.loc("textures/gui/sim_chamber.png");
    private static final Identifier PLAYER = HostileNetworks.loc("textures/gui/default_gui.png");

    private TickableTextList body;
    private FailureState lastFailState = FailureState.NONE;
    private boolean runtimeTextLoaded = false;

    public SimChamberScreen(SimChamberMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, WIDTH, HEIGHT);
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(new RedstoneButton(this.getLeftPos() + 228, this.getTopPos()));
        addRenderableWidget(new SimModeButton(this.getLeftPos() + 228, this.getTopPos() + 20));
        this.body = new TickableTextList(this.minecraft.font, MAX_TEXT_WIDTH);
        this.lastFailState = FailureState.NONE;
        this.runtimeTextLoaded = false;
        this.containerTick();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor gfx, int pX, int pY) {
        if (this.isHovering(211, 48, 7, 87, pX, pY)) {
            List<Component> txt = new ArrayList<>(2);
            txt.add(Component.translatable("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.simPowerCap));
            DataModelInstance cModel = new DataModelInstance(this.menu.getSlot(0).getItem(), 0);
            if (cModel.isValid()) {
                txt.add(Component.translatable("hostilenetworks.gui.cost", this.menu.getSimMode().adjustCost(cModel.getModel().simCost())));
            }
            gfx.setTooltipForNextFrame(this.font, txt, java.util.Optional.empty(), pX, pY);
        }
        else if (this.isHovering(14, 48, 7, 87, pX, pY)) {
            DataModelInstance cModel = new DataModelInstance(this.menu.getSlot(0).getItem(), 0);
            if (cModel.isValid()) {
                List<Component> txt = new ArrayList<>(1);
                if (!cModel.getTier().isMax()) {
                    txt.add(Component.translatable("hostilenetworks.gui.data", cModel.getData() - cModel.getTierData(), cModel.getNextTierData() - cModel.getTierData()));
                }
                else {
                    txt.add(Component.translatable("hostilenetworks.gui.max_data").withStyle(ChatFormatting.RED));
                }
                gfx.setTooltipForNextFrame(this.font, txt, java.util.Optional.empty(), pX, pY);
            }
        }
        else if (this.isHovering(229, 1, 16, 16, pX, pY)) {
            gfx.setTooltipForNextFrame(this.font, Component.translatable(this.menu.getRedstoneState().getKey()), pX, pY);
        }
        else if (this.isHovering(229, 19, 16, 16, pX, pY)) {
            if (HostileConfig.simModelUpgrade == 0) {
                gfx.setTooltipForNextFrame(this.font, Component.translatable("hostilenetworks.gui.mode.disabled"), pX, pY);
            }
            else {
                SimMode mode = this.menu.getSimMode();
                List<Component> txt = new ArrayList<>(6);
                txt.add(Component.translatable(mode.getKey()));
                txt.add(Component.translatable(mode.getKey() + ".desc").withStyle(ChatFormatting.GRAY));
                txt.add(Component.translatable(mode.getKey() + ".desc2").withStyle(ChatFormatting.GRAY));
                txt.add(Component.empty());
                txt.add(Component.translatable("hostilenetworks.gui.mode.power", Math.round(mode.getCostMultiplier() * 100)).withColor(Color.AQUA));
                txt.add(Component.translatable("hostilenetworks.gui.mode.speed", Math.round(mode.getSpeedMultiplier() * 100)).withColor(Color.AQUA));
                gfx.setTooltipForNextFrame(this.font, txt, java.util.Optional.empty(), pX, pY);
            }
        }
        else super.extractTooltip(gfx, pX, pY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int pX, int pY) {
        int runtime = this.menu.getRuntime();
        if (runtime > 0) {
            int maxRuntime = this.menu.getSimMode().getRuntime();
            int rTime = Math.min(99, Mth.ceil(100F * (maxRuntime - runtime) / maxRuntime));
            gfx.text(this.font, rTime + "%", 184, 123, (0xFF000000 | Color.AQUA), true);
        }
        DataModelInstance inst = new DataModelInstance(this.menu.getSlot(0).getItem(), 0);
        if (inst.isValid()) {
            int xOff = 18;
            Component msg = Component.translatable("hostilenetworks.gui.target", inst.getModel().name().copy().withColor(Color.LIME));
            gfx.text(this.font, msg, xOff, 9, (0xFF000000 | Color.WHITE));

            msg = Component.translatable("hostilenetworks.gui.tier", inst.getTier().getComponent());
            gfx.text(this.font, msg, xOff, 9 + this.font.lineHeight + 3, (0xFF000000 | Color.WHITE));

            msg = inst.getAccuracyComponent();
            gfx.text(this.font, msg, xOff, 9 + (this.font.lineHeight + 3) * 2, (0xFF000000 | Color.WHITE));
        }
        this.body.render(gfx, 29, 51);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int pX, int pY, float pPartialTicks) {
        super.extractBackground(gfx, pX, pY, pPartialTicks);
        int left = this.getLeftPos();
        int top = this.getTopPos();

        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + 8, top, (float) 0, (float) 0, 216, 141, 256, 256);
        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left - 14, top, (float) 0, (float) 141, 18, 18, 256, 256);

        // Redstone + mode button backgrounds
        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + 228, top, (float) 0, (float) 141, 18, 18, 256, 256);
        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + 228, top + 20, (float) 0, (float) 141, 18, 18, 256, 256);

        int energyHeight = 87 - Mth.ceil(87F * this.menu.getEnergyStored() / HostileConfig.simPowerCap);

        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + 211, top + 48, (float) 18, (float) 141, 7, energyHeight, 256, 256);

        int dataHeight = 87;
        DataModelInstance cModel = new DataModelInstance(this.menu.getSlot(0).getItem(), 0);
        if (cModel.isValid()) {
            int data = cModel.getData();
            ModelTier tier = cModel.getTier();
            if (tier.isMax()) {
                dataHeight = 0;
            }
            else {
                dataHeight = 87 - Mth.ceil(87F * (data - cModel.getTierData()) / (cModel.getNextTierData() - cModel.getTierData()));
            }
        }

        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + 14, top + 48, (float) 18, (float) 141, 7, dataHeight, 256, 256);
        gfx.blit(RenderPipelines.GUI_TEXTURED, PLAYER, left + 28, top + 145, (float) 0, (float) 0, 176, 90, 256, 256);
    }

    private static final Component ERROR = Component.literal("ERROR").withStyle(ChatFormatting.OBFUSCATED);

    @Override
    public void containerTick() {
        if (this.menu.getFailState() != FailureState.NONE) {
            FailureState oState = this.lastFailState;
            this.lastFailState = this.menu.getFailState();
            if (oState != this.lastFailState) {
                this.body.clear();
                MutableComponent msg = Component.translatable(this.lastFailState.getKey());
                if (this.lastFailState == FailureState.INPUT) {
                    DataModelInstance cModel = new DataModelInstance(this.menu.getSlot(0).getItem(), 0);
                    Component name = ERROR;
                    if (cModel.isValid()) {
                        name = cModel.getModel().input().items().findFirst().<Component>map(h -> h.value().getName(ItemStack.EMPTY)).orElse(Component.literal("?"));
                    }
                    msg = Component.translatable(this.lastFailState.getKey(), name);
                }
                this.body.addLine(msg, 1);
            }
            this.runtimeTextLoaded = false;
        }
        else if (!this.runtimeTextLoaded) {
            int ticks = this.menu.getSimMode().getRuntime() - this.menu.getRuntime();
            // The magic constant is scaled for the 300 tick base runtime, but inference mode is faster.
            float speed = RUNTIME_TEXT_SPEED * SimMode.BASE_RUNTIME / this.menu.getSimMode().getRuntime();
            this.body.clear();
            int iters = DataModelItem.getIters(this.menu.getSlot(0).getItem());
            boolean training = this.menu.getSimMode() == SimMode.TRAINING;
            String prefix = training ? "hostilenetworks.run.train." : "hostilenetworks.run.";
            for (int i = 0; i < 7; i++) {
                Component txt = Component.translatable(prefix + i, iters);
                this.body.addLine(txt, speed);
                if (i == 0) {
                    Component version = Component.literal("v" + HostileNetworks.VERSION).withStyle(ChatFormatting.GOLD);
                    this.body.continueLine(version, speed);
                }
                else if (i == 5 && !training) {
                    String key = "hostilenetworks.color_text." + (this.menu.didPredictionSucceed() ? "success" : "failed");
                    Component status = Component.translatable(key).withStyle(this.menu.didPredictionSucceed() ? ChatFormatting.GOLD : ChatFormatting.RED);
                    this.body.continueLine(status, speed);
                }
            }
            this.body.setTicks(ticks);
            this.runtimeTextLoaded = true;
            this.lastFailState = FailureState.NONE;
        }

        this.body.tick();
        if (this.menu.getRuntime() == 0) {
            this.runtimeTextLoaded = false;
        }
    }

    private class RedstoneButton extends AbstractWidget {

        public RedstoneButton(int x, int y) {
            super(x, y, 18, 18, Component.empty());
        }

        /**
         * Sends a {@link ServerboundContainerButtonClickPayload} containing the id of the new redstone state.
         */
        @Override
        public void onClick(MouseButtonEvent event, boolean clientSide) {
            SimChamberScreen scn = SimChamberScreen.this;
            int idx = scn.menu.getRedstoneState().next().ordinal();
            scn.minecraft.gameMode.handleInventoryButtonClick(scn.menu.containerId, idx);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SimChamberScreen.this.menu.getRedstoneState().getResourceLocation(), this.getX() + 1, this.getY() + 1, (float) 0, (float) 0, 16, 16, 16, 16);
        }
    }

    /**
     * Toggles the chamber between Inference and Training modes. Greys out and refuses input while Training Mode is
     * disabled by the {@code simModelUpgrade} config.
     */
    private class SimModeButton extends AbstractWidget {

        public SimModeButton(int x, int y) {
            super(x, y, 18, 18, Component.empty());
        }

        /**
         * Sends a {@link ServerboundContainerButtonClickPayload} containing the id of the new sim mode (offset by 3 to
         * avoid colliding with the redstone button's ids).
         */
        @Override
        public void onClick(MouseButtonEvent event, boolean clientSide) {
            SimChamberScreen scn = SimChamberScreen.this;
            int idx = 3 + scn.menu.getSimMode().next().ordinal();
            scn.minecraft.gameMode.handleInventoryButtonClick(scn.menu.containerId, idx);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Training Mode is unavailable while model upgrading is disabled; an inactive widget ignores clicks.
            this.active = HostileConfig.simModelUpgrade != 0;
            int shade = this.active ? 0xFF : 0x59;
            int color = ARGB.color(0xFF, shade, shade, shade);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SimChamberScreen.this.menu.getSimMode().getResourceLocation(), this.getX() + 1, this.getY() + 1, 0F, 0F, 16, 16, 16, 16, color);
        }
    }

}
