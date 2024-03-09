package dev.shadowsoffire.hostilenetworks.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.CachedModel;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.screen.TickableText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class SimChamberScreen extends PlaceboContainerScreen<SimChamberContainer> {

    public static final int WIDTH = 232;
    public static final int HEIGHT = 230;
    private static final ResourceLocation BASE = new ResourceLocation(HostileNetworks.MODID, "textures/gui/sim_chamber.png");
    private static final ResourceLocation PLAYER = new ResourceLocation(HostileNetworks.MODID, "textures/gui/default_gui.png");

    private List<TickableText> body = new ArrayList<>(7);
    private FailureState lastFailState = FailureState.NONE;
    private boolean runtimeTextLoaded = false;

    public SimChamberScreen(SimChamberContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int pX, int pY) {
        if (this.isHovering(211, 48, 7, 87, pX, pY)) {
            List<Component> txt = new ArrayList<>(2);
            txt.add(Component.translatable("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.simPowerCap));
            CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
            if (cModel.isValid()) {
                txt.add(Component.translatable("hostilenetworks.gui.cost", cModel.getModel().simCost()));
            }
            gfx.renderComponentTooltip(this.font, txt, pX, pY);
        }
        else if (this.isHovering(14, 48, 7, 87, pX, pY)) {
            CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
            if (cModel.isValid()) {
                List<Component> txt = new ArrayList<>(1);
                if (cModel.getTier() != cModel.getTier().next()) {
                    txt.add(Component.translatable("hostilenetworks.gui.data", cModel.getData() - cModel.getTierData(), cModel.getNextTierData() - cModel.getTierData()));
                }
                else txt.add(Component.translatable("hostilenetworks.gui.max_data").withStyle(ChatFormatting.RED));
                gfx.renderComponentTooltip(this.font, txt, pX, pY);
            }
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
        CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
        if (cModel.isValid()) {
            int xOff = 18;
            String msg = I18n.get("hostilenetworks.gui.target");
            gfx.drawString(this.font, msg, xOff, 9, Color.WHITE);
            xOff += this.font.width(msg);
            gfx.drawString(this.font, cModel.getModel().name(), xOff, 9, Color.LIME);

            xOff = 18;
            msg = I18n.get("hostilenetworks.gui.tier");
            gfx.drawString(this.font, msg, xOff, 9 + this.font.lineHeight + 3, Color.WHITE);
            xOff += this.font.width(msg);
            msg = I18n.get("hostilenetworks.tier." + cModel.getTier().name);
            gfx.drawString(this.font, msg, xOff, 9 + this.font.lineHeight + 3, cModel.getTier().color());

            xOff = 18;
            msg = I18n.get("hostilenetworks.gui.accuracy");
            gfx.drawString(this.font, msg, xOff, 9 + (this.font.lineHeight + 3) * 2, Color.WHITE);
            xOff += this.font.width(msg);
            DecimalFormat fmt = new DecimalFormat("##.##%");
            msg = fmt.format(cModel.getAccuracy());
            gfx.drawString(this.font, msg, xOff, 9 + (this.font.lineHeight + 3) * 2, cModel.getTier().color());
        }
        int left = 29;
        int top = 51;
        int spacing = this.font.lineHeight + 3;
        int idx = 0;
        for (TickableText t : this.body) {
            t.render(this.font, gfx, left, top + spacing * idx);
            if (t.causesNewLine()) {
                idx++;
                left = 29;
            }
            else {
                left += t.getWidth(this.font);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float pPartialTicks, int pX, int pY) {
        int left = this.getGuiLeft();
        int top = this.getGuiTop();

        gfx.blit(BASE, left + 8, top, 0, 0, 216, 141, 256, 256);
        gfx.blit(BASE, left - 14, top, 0, 141, 18, 18, 256, 256);

        int energyHeight = 87 - Mth.ceil(87F * this.menu.getEnergyStored() / HostileConfig.simPowerCap);

        gfx.blit(BASE, left + 211, top + 48, 18, 141, 7, energyHeight, 256, 256);

        int dataHeight = 87;
        CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
        if (cModel.isValid()) {
            int data = cModel.getData();
            ModelTier tier = cModel.getTier();
            ModelTier next = tier.next();
            if (tier == next) dataHeight = 0;
            else dataHeight = 87 - Mth.ceil(87F * (data - cModel.getTierData()) / (cModel.getNextTierData() - cModel.getTierData()));
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
                String[] msg = I18n.get(this.lastFailState.getKey()).split("\\n");
                if (this.lastFailState == FailureState.INPUT) {
                    CachedModel cModel = new CachedModel(this.menu.getSlot(0).getItem(), 0);
                    Component name = ERROR;
                    if (cModel.isValid()) name = cModel.getModel().input().getHoverName();
                    msg = I18n.get(this.lastFailState.getKey(), name.getString()).split("\\n");
                }
                for (String s : msg)
                    this.body.add(new TickableText(s, Color.WHITE));
            }
            this.runtimeTextLoaded = false;
        }
        else if (!this.runtimeTextLoaded) {
            int ticks = 300 - this.menu.getRuntime();
            float speed = 0.65F;
            this.body.clear();
            int iters = DataModelItem.getIters(this.menu.getSlot(0).getItem());
            for (int i = 0; i < 7; i++) {
                TickableText txt = new TickableText(I18n.get("hostilenetworks.run." + i, iters), Color.WHITE, i != 0 && i != 5, speed);
                this.body.add(txt.setTicks(ticks));
                ticks = Math.max(0, ticks - txt.getMaxUsefulTicks());
                if (i == 0) {
                    txt = new TickableText("v" + HostileNetworks.VERSION, ChatFormatting.GOLD.getColor(), true, speed);
                    this.body.add(txt.setTicks(ticks));
                    ticks = Math.max(0, ticks - txt.getMaxUsefulTicks());
                }
                else if (i == 5) {
                    String key = "hostilenetworks.color_text." + (this.menu.didPredictionSucceed() ? "success" : "failed");
                    txt = new TickableText(I18n.get(key), (this.menu.didPredictionSucceed() ? ChatFormatting.GOLD : ChatFormatting.RED).getColor(), true, speed);
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
