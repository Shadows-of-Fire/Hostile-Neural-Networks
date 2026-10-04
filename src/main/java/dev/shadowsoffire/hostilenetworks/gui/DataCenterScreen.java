package dev.shadowsoffire.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.multiblock.DataCenterShell;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.util.DrawsOnLeft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public class DataCenterScreen extends PlaceboContainerScreen<DataCenterMenu> implements DrawsOnLeft {

    public static final int WIDTH = 230;
    public static final int HEIGHT = 223;
    public static final Identifier BASE = HostileNetworks.loc("textures/gui/data_center.png");
    public static final Identifier PLAYER = HostileNetworks.loc("textures/gui/default_gui.png");

    private static final int REDSTONE_X = -22;
    private static final int REDSTONE_Y = 0;

    private static final int ENERGY_X = 213;
    private static final int ENERGY_Y = 19;
    private static final int ENERGY_U = 231;
    private static final int ENERGY_W = 7;
    private static final int ENERGY_H = 100;

    public DataCenterScreen(DataCenterMenu pMenu, Inventory pInv, Component pTitle) {
        super(pMenu, pInv, pTitle, WIDTH, HEIGHT);
        this.inventoryLabelX = DataCenterMenu.PLAYER_INV_X;
        this.inventoryLabelY = DataCenterMenu.PLAYER_INV_Y - 12;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int pX, int pY, float pPartialTicks) {
        super.extractBackground(gfx, pX, pY, pPartialTicks);
        int left = this.getLeftPos();
        int top = this.getTopPos();

        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left, top, (float) 0, (float) 0, 230, 128, 256, 256);

        int cap = Math.max(1, HostileConfig.dataCenterPowerCap);
        int barHeight = Mth.clamp(Mth.floor((float) ENERGY_H * this.menu.getEnergyStored() / cap), 0, ENERGY_H);
        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + ENERGY_X, top + ENERGY_Y + ENERGY_H - barHeight, (float) ENERGY_U, (float) 0, ENERGY_W, barHeight, 256, 256);

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int idx = row * 5 + col;
                FailureState fs = this.menu.getFailState(idx);
                if (fs == FailureState.MODEL) continue;
                int rt = this.menu.getRuntime(idx);
                int barX = left + DataCenterMenu.MODEL_GRID_X + col * 18;
                int barY = top + DataCenterMenu.MODEL_GRID_Y + row * 20 + 16;
                int barW = 16;
                int filled;
                int barV = 129;
                if (fs != FailureState.NONE) {
                    filled = barW;
                    barV += 2;
                }
                else if (rt > 0) {
                    filled = Mth.clamp(barW - Mth.floor((float) barW * rt / DataCenterTileEntity.RUNTIME_TICKS), 0, barW);
                }
                else {
                    continue;
                }
                gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, barX, barY, (float) 0, (float) barV, filled, 2, 256, 256);
            }
        }

        gfx.blit(RenderPipelines.GUI_TEXTURED, BASE, left + REDSTONE_X, top + REDSTONE_Y, (float) 18, (float) 138, 18, 18, 256, 256);
        gfx.blit(RenderPipelines.GUI_TEXTURED, this.menu.getRedstoneState().getResourceLocation(), left + REDSTONE_X + 1, top + REDSTONE_Y + 1, (float) 0, (float) 0, 16, 16, 16, 16);

        gfx.blit(RenderPipelines.GUI_TEXTURED, PLAYER, left + DataCenterMenu.PLAYER_INV_X - 8, top + DataCenterMenu.PLAYER_INV_Y - 14, (float) 0, (float) 0, 176, 90, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int pX, int pY) {
        gfx.text(this.font, this.title, 8, 6, (0xFF000000 | Color.AQUA), false);
        gfx.text(this.font, Component.translatable("hostilenetworks.gui.data_center.io"), 130, 6, (0xFF000000 | Color.AQUA), false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor gfx, int pX, int pY) {
        if (this.isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, pX, pY)) {
            gfx.setTooltipForNextFrame(this.font,
                Component.translatable("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.dataCenterPowerCap), pX, pY);
        }
        if (this.isHovering(REDSTONE_X, REDSTONE_Y, 18, 18, pX, pY)) {
            gfx.setTooltipForNextFrame(this.font, Component.translatable(this.menu.getRedstoneState().getKey()), pX, pY);
        }
        if (!this.menu.isShellValid()) {
            this.renderShellInvalidTooltip(gfx);
            super.extractTooltip(gfx, pX, pY);
            return;
        }
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int sx = DataCenterMenu.MODEL_GRID_X + col * 18;
                int sy = DataCenterMenu.MODEL_GRID_Y + row * 20;
                if (!this.isHovering(sx, sy, 16, 16, pX, pY)) continue;
                int idx = row * 5 + col;
                ItemStack stack = this.menu.getSlot(idx).getItem();
                if (stack.isEmpty()) continue;
                FailureState fs = this.menu.getFailState(idx);
                List<Component> tip = new ArrayList<>();
                tip.add(stack.getHoverName());
                if (fs != FailureState.NONE) {
                    if (fs == FailureState.INPUT) {
                        DynamicHolder<DataModel> failHolder = DataModelItem.getStoredModel(stack);
                        Component inputName = failHolder.isBound()
                            ? failHolder.get().input().items().findFirst().<Component>map(h -> h.value().getName(ItemStack.EMPTY)).orElse(Component.literal("?"))
                            : Component.literal("?");
                        tip.add(Component.translatable(fs.getKey(), inputName).withColor(0xE05050));
                    }
                    else {
                        tip.add(Component.translatable(fs.getKey()).withColor(0xE05050));
                    }
                }
                else {
                    DynamicHolder<DataModel> holder = DataModelItem.getStoredModel(stack);
                    if (holder.isBound()) {
                        DataModel model = holder.get();

                        int cost = Mth.ceil(model.simCost() * HostileConfig.dataCenterSimCostMultiplier);
                        tip.add(Component.translatable("hostilenetworks.gui.data_center.power_cost", cost).withColor(Color.AQUA));

                        FabSelection sel = this.menu.getSelection(idx);
                        int dropIdx = sel.current();
                        List<ItemStack> fabDrops = model.fabDrops().stream().map(ItemStackTemplate::create).toList();
                        if (dropIdx >= 0 && dropIdx < fabDrops.size()) {
                            ItemStack drop = fabDrops.get(dropIdx);
                            Component dropLabel = drop.getCount() == 1
                                ? drop.getHoverName()
                                : Component.literal(drop.getCount() + " ").append(drop.getHoverName());
                            tip.add(Component.translatable("hostilenetworks.gui.data_center.producing", dropLabel).withColor(Color.AQUA));
                        }

                        int runtime = this.menu.getRuntime(idx);
                        if (runtime > 0) {
                            String elapsed = String.format(Locale.ROOT, "%.1f", (DataCenterTileEntity.RUNTIME_TICKS - runtime) / 20f);
                            String total = String.format(Locale.ROOT, "%.1f", DataCenterTileEntity.RUNTIME_TICKS / 20f);
                            tip.add(Component.translatable("hostilenetworks.gui.data_center.progress", elapsed, total).withColor(Color.AQUA));
                        }
                    }
                }
                tip.add(Component.translatable("hostilenetworks.gui.data_center.click_to_config").withColor(0x808080));
                int statusTop = this.getTopPos() + REDSTONE_Y + 40;
                gfx.pose().pushMatrix();
                gfx.pose().translate(-3, 0);
                this.drawOnLeft(gfx, tip, statusTop, Math.min(this.getLeftPos(), 240));
                gfx.pose().popMatrix();
                break;
            }
        }
        super.extractTooltip(gfx, pX, pY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double pX = event.x(), pY = event.y(); int pButton = event.button();
        if (this.isHovering(REDSTONE_X, REDSTONE_Y, 18, 18, pX, pY)) {
            this.click(DataCenterMenu.REDSTONE_BASE + this.menu.getRedstoneState().next().ordinal());
            return true;
        }
        if (pButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    int sx = DataCenterMenu.MODEL_GRID_X + col * 18;
                    int sy = DataCenterMenu.MODEL_GRID_Y + row * 20;
                    if (this.isHovering(sx, sy, 16, 16, pX, pY)) {
                        int idx = row * 5 + col;
                        if (DataModelItem.getStoredModel(this.menu.getSlot(idx).getItem()).isBound()) {
                            Minecraft.getInstance().pushGuiLayer(new DataCenterFabConfigScreen(this.menu, idx));
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void click(int id) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void renderShellInvalidTooltip(GuiGraphicsExtractor gfx) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable("hostilenetworks.fail.shell_broken").withColor(0xE05050));

        DataCenterTileEntity tile = this.menu.getTile();
        if (tile != null && this.minecraft != null && this.minecraft.level != null) {
            DataCenterShell.bestGuessLayout(tile.getBlockPos(), tile.getBlockState(), this.minecraft.level).ifPresent(layout -> {
                List<DataCenterShell.InvalidEntry> invalid = DataCenterShell.findInvalidPositions(layout, this.minecraft.level);
                if (invalid.size() == 1) {
                    DataCenterShell.InvalidEntry entry = invalid.get(0);
                    Component typeName = Component.translatable(entry.type().getKey());
                    tip.add(Component.translatable("hostilenetworks.gui.data_center.shell_invalid.at_pos",
                        typeName, entry.pos().getX(), entry.pos().getY(), entry.pos().getZ()).withColor(0xE08080));
                }
                else if (invalid.size() > 1) {
                    Set<Integer> yLevels = new TreeSet<>();
                    for (DataCenterShell.InvalidEntry entry : invalid) yLevels.add(entry.pos().getY());
                    String yList = yLevels.stream().map(String::valueOf).collect(Collectors.joining(", "));
                    tip.add(Component.translatable("hostilenetworks.gui.data_center.shell_invalid.multi_y", yList).withColor(0xE08080));
                }
            });
        }

        int statusTop = this.getTopPos() + REDSTONE_Y + 40;
        gfx.pose().pushMatrix();
        gfx.pose().translate(-3, 0);
        this.drawOnLeft(gfx, tip, statusTop, Math.min(this.getLeftPos(), 240));
        gfx.pose().popMatrix();
    }
}
