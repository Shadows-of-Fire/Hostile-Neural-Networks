package dev.shadowsoffire.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.screen.TickableTextList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class SimChamberScreen extends PlaceboContainerScreen<SimChamberContainer> {

    public static final int WIDTH = 232;
    public static final int HEIGHT = 230;
    public static final int MAX_TEXT_WIDTH = 174;
    public static final float RUNTIME_TEXT_SPEED = 0.65F;



    private static final ResourceLocation BASE = HostileNetworks.loc("textures/gui/sim_chamber.png");
    private static final ResourceLocation PLAYER = HostileNetworks.loc("textures/gui/default_gui.png");

    private TickableTextList body;
    private FailureState lastFailState = FailureState.NONE;
    private boolean runtimeTextLoaded = false;

    public SimChamberScreen(SimChamberContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(new RedstoneButton(this.getGuiLeft() + 228, this.getGuiTop()));
        addRenderableWidget(new ModeButton(this.getGuiLeft() + 228, this.getGuiTop() + 20));
        this.body = new TickableTextList(this.minecraft.font, MAX_TEXT_WIDTH);
        this.lastFailState = FailureState.NONE;
        this.runtimeTextLoaded = false;
        this.containerTick();
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int pX, int pY) {
        if (this.isHovering(211, 48, 7, 87, pX, pY)) {
            List<Component> txt = new ArrayList<>(2);
            txt.add(Component.translatable("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.simPowerCap));
            DataModelInstance cModel = new DataModelInstance(this.menu.getSlot(0).getItem(), 0);
            if (cModel.isValid()) {
                txt.add(Component.translatable("hostilenetworks.gui.cost", cModel.getModel().simCost()));
            }
            gfx.renderComponentTooltip(this.font, txt, pX, pY);
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
                gfx.renderComponentTooltip(this.font, txt, pX, pY);
            }
        }

        else if (this.isHovering(228, 20, 18, 18, pX, pY)) {
            Component tip = this.menu.isTrainingMode()
                    ? Component.translatable("hostilenetworks.gui.mode.training")
                    : Component.translatable("hostilenetworks.gui.mode.inference");
            gfx.renderTooltip(this.font, tip, pX, pY);
        }

        else if (this.isHovering(229, 1, 16, 16, pX, pY)) {
            gfx.renderTooltip(this.font, Component.translatable(this.menu.getRedstoneState().getKey()), pX, pY);
        }
        else super.renderTooltip(gfx, pX, pY);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int pX, int pY) {
        int runtime = this.menu.getRuntime();
        if (runtime > 0) {
            int rTime = Math.min(99, Mth.ceil(100F * (300 - runtime) / 300));
            gfx.drawString(this.font, rTime + "%", 184, 123, Color.AQUA, true);
        }
        DataModelInstance inst = new DataModelInstance(this.menu.getSlot(0).getItem(), 0);
        if (inst.isValid()) {
            int xOff = 18;
            Component msg = Component.translatable("hostilenetworks.gui.target", inst.getModel().name().copy().withColor(Color.LIME));
            gfx.drawString(this.font, msg, xOff, 9, Color.WHITE);

            msg = Component.translatable("hostilenetworks.gui.tier", inst.getTier().getComponent());
            gfx.drawString(this.font, msg, xOff, 9 + this.font.lineHeight + 3, Color.WHITE);

            msg = inst.getAccuracyComponent();
            gfx.drawString(this.font, msg, xOff, 9 + (this.font.lineHeight + 3) * 2, Color.WHITE);
        }
        this.body.render(gfx, 29, 51);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float pPartialTicks, int pX, int pY) {
        int left = this.getGuiLeft();
        int top = this.getGuiTop();

        gfx.blit(BASE, left + 8, top, 0, 0, 216, 141, 256, 256);
        gfx.blit(BASE, left - 14, top, 0, 141, 18, 18, 256, 256);

        // Redstone background
        gfx.blit(BASE, left + 228, top, 0, 141, 18, 18, 256, 256);

        int energyHeight = 87 - Mth.ceil(87F * this.menu.getEnergyStored() / HostileConfig.simPowerCap);

        gfx.blit(BASE, left + 211, top + 48, 18, 141, 7, energyHeight, 256, 256);

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

        gfx.blit(BASE, left + 14, top + 48, 18, 141, 7, dataHeight, 256, 256);
        gfx.blit(PLAYER, left + 28, top + 145, 0, 0, 176, 90, 256, 256);
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
                        name = cModel.getModel().input().getItems()[0].getHoverName();
                    }
                    msg = Component.translatable(this.lastFailState.getKey(), name);
                }
                this.body.addLine(msg, 1);
            }
            this.runtimeTextLoaded = false;
        }
        else if (!this.runtimeTextLoaded) {
            int ticks = 300 - this.menu.getRuntime();
            float speed = 0.65F;
            this.body.clear();
            int iters = DataModelItem.getIters(this.menu.getSlot(0).getItem());

            DataModelInstance tier = new DataModelInstance(this.menu.getSlot(0).getItem(),0);
            Component tierComp = tier.isValid() ? tier.getTier().getComponent() : ERROR;

            boolean simTraining = SimChamberScreen.this.menu.isTrainingMode();
            for (int i = 0; i < 7; i++) {
                if (simTraining) {
                    Component txt = Component.translatable("hostilenetworks.run.training." + i, iters);
                    this.body.addLine(txt, speed);
                    if (i == 0) {
                        Component version = Component.literal("v" + HostileNetworks.VERSION).withStyle(ChatFormatting.GOLD);
                        this.body.continueLine(version, speed);
                    }
                }
                else {
                    Component txt = Component.translatable("hostilenetworks.run.inference." + i, iters);
                    this.body.addLine(txt, speed);
                    if (i == 0) {
                        Component version = Component.literal("v" + HostileNetworks.VERSION).withStyle(ChatFormatting.GOLD);
                        this.body.continueLine(version, speed);

                    }
                    else if (i == 1) {
                        this.body.continueLine(tierComp, speed);
                    }
                    else if (i == 5) {
                        String key = "hostilenetworks.color_text." + (this.menu.didPredictionSucceed() ? "success" : "failed");
                        Component status = Component.translatable(key).withStyle(this.menu.didPredictionSucceed() ? ChatFormatting.GOLD : ChatFormatting.RED);
                        this.body.continueLine(status, speed);
                    }
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
        public void onClick(double mouseX, double mouseY) {
            SimChamberScreen scn = SimChamberScreen.this;
            int idx = scn.menu.getRedstoneState().next().ordinal();
            scn.minecraft.gameMode.handleInventoryButtonClick(scn.menu.containerId, idx);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(SimChamberScreen.this.menu.getRedstoneState().getResourceLocation(), this.getX() + 1, this.getY() + 1, 0, 0, 16, 16, 16, 16);
        }
    }

    private class ModeButton extends AbstractWidget {

        public ModeButton(int x, int y) {
            super(x, y, 18, 18, Component.empty());
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            SimChamberScreen scn = SimChamberScreen.this;
            scn.minecraft.gameMode.handleInventoryButtonClick(scn.menu.containerId, 3);
        }


        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            gfx.setColor(1f, 1f, 1f, 1f);
            gfx.blit(BASE, this.getX(), this.getY(), 0, 141, 18, 18, 256, 256);
            String s = SimChamberScreen.this.menu.isTrainingMode() ? "T" : "I";
            int x = this.getX() + 6 + ("I".equals(s) ? 1 : 0); //I needs to be to the right by 1 pixel, otherwise looks ugly
            int y = this.getY() + 5;
            gfx.drawString(SimChamberScreen.this.font, s, x, y, 0xFFFFFF, true);


        }
    }




}
