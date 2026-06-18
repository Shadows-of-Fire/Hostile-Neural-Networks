package dev.shadowsoffire.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.hostilenetworks.util.FabSelection.ProductionMode;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class DataCenterFabConfigScreen extends Screen {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 100;
    public static final ResourceLocation BASE = HostileNetworks.loc("textures/gui/loot_fabricator.png");
    public static final WidgetSprites LEFT_BUTTON = DeepLearnerScreen.makeSprites("widget/fab_left", "widget/fab_left_hovered");
    public static final WidgetSprites RIGHT_BUTTON = DeepLearnerScreen.makeSprites("widget/fab_right", "widget/fab_right_hovered");

    private static final int PREVIEW_X = 79, PREVIEW_Y = 5;
    private static final int MODE_X = WIDTH + 4, MODE_Y = 0;
    private static final int CLEAR_X = MODE_X + 20, CLEAR_Y = MODE_Y;
    private static final int BACK_X = -22, BACK_Y = 0;
    private static final int QUEUE_X = WIDTH + 4, QUEUE_Y = 30, GRID_SLOT_SIZE = 18;
    private static final int QUEUE_COLS = 3, QUEUE_ROWS = 3, QUEUE_VISIBLE = QUEUE_COLS * QUEUE_ROWS;
    private static final int QUEUE_LABEL_Y = 20;
    private static final ResourceLocation CLEAR_QUEUE_ICON = ResourceLocation.withDefaultNamespace("textures/item/barrier.png");

    private final DataCenterMenu parent;
    private final int slotIdx;

    private int leftPos, topPos;
    private DynamicHolder<DataModel> model = DataModelRegistry.INSTANCE.emptyHolder();
    private int currentPage = 0;
    private ImageButton btnLeft, btnRight;

    public DataCenterFabConfigScreen(DataCenterMenu parent, int slotIdx) {
        super(Component.translatable("block.hostilenetworks.data_center"));
        this.parent = parent;
        this.slotIdx = slotIdx;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().popGuiLayer();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - HEIGHT) / 2;
        this.btnLeft = this.addRenderableWidget(new ImageButton(this.leftPos + 13, this.topPos + 68, 29, 12, LEFT_BUTTON, btn -> {
            if (this.model.isBound() && this.currentPage > 0) this.currentPage--;
        }));
        this.btnRight = this.addRenderableWidget(new ImageButton(this.leftPos + 46, this.topPos + 68, 29, 12, RIGHT_BUTTON, btn -> {
            if (this.model.isBound() && this.currentPage < this.model.get().fabDrops().size() / 9) this.currentPage++;
        }));
    }

    private FabSelection selection() {
        return this.parent.getSelection(this.slotIdx);
    }

    private int selectedDropIndex() {
        if (!this.model.isBound()) return -1;
        FabSelection sel = this.selection();
        int idx = sel.current();
        return (idx < 0 || idx >= this.model.get().fabDrops().size()) ? -1 : idx;
    }

    private boolean isHovering(int x, int y, int width, int height, double mx, double my) {
        double lpx = mx - this.leftPos;
        double lpy = my - this.topPos;
        return lpx >= x - 1 && lpx < x + width + 1 && lpy >= y - 1 && lpy < y + height + 1;
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        this.model = this.parent.getModelHolder(this.slotIdx);
        if (this.model.isBound()) {
            this.btnLeft.visible = this.currentPage > 0;
            this.btnRight.visible = this.currentPage < this.model.get().fabDrops().size() / 9;
        }
        else {
            this.btnLeft.visible = false;
            this.btnRight.visible = false;
        }
        this.renderBackground(gfx, mx, my, pt);
        this.renderBg(gfx, pt, mx, my);
        for (Renderable renderable : this.renderables) {
            renderable.render(gfx, mx, my, pt);
        }
        this.renderLabels(gfx);
        this.renderHoverTooltip(gfx, mx, my);
    }

    private void renderLabels(GuiGraphics gfx) {
        if (this.model.isBound()) {
            gfx.drawString(this.font, this.model.get().name(), this.leftPos + 8, this.topPos - 10, Color.AQUA, false);
            if (this.selection().mode() == ProductionMode.QUEUE) {
                gfx.drawString(this.font, Component.translatable("hostilenetworks.gui.queue_current"),
                    this.leftPos + QUEUE_X, this.topPos + QUEUE_LABEL_Y, Color.AQUA);
            }
        }
    }

    private void renderHoverTooltip(GuiGraphics gfx, int mx, int my) {
        if (this.isHovering(BACK_X, BACK_Y, 18, 18, mx, my)) {
            gfx.renderTooltip(this.font, Component.translatable("hostilenetworks.gui.data_center.back"), mx, my);
        }
        if (!this.model.isBound()) return;

        FabSelection sel = this.selection();
        boolean queue = sel.mode() == ProductionMode.QUEUE;
        List<ItemStack> drops = this.model.get().fabDrops();
        int selection = this.selectedDropIndex();

        if (this.isHovering(MODE_X, MODE_Y, 18, 18, mx, my)) {
            gfx.renderTooltip(this.font, Component.translatable(sel.mode().getKey()), mx, my);
        }
        if (selection != -1 && this.isHovering(PREVIEW_X, PREVIEW_Y, 16, 16, mx, my)) {
            if (queue) {
                List<Component> txt = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, drops.get(selection)));
                txt.add(Component.translatable("hostilenetworks.gui.queue_clear_current"));
                gfx.renderComponentTooltip(this.font, txt, mx, my);
            }
            else {
                gfx.renderComponentTooltip(this.font, Arrays.asList(Component.translatable("hostilenetworks.gui.clear")), mx, my);
            }
        }
        if (queue) {
            if (this.isHovering(CLEAR_X, CLEAR_Y, 18, 18, mx, my)) {
                gfx.renderTooltip(this.font, Component.translatable("hostilenetworks.gui.clear_queue"), mx, my);
            }
            List<Integer> entries = sel.entries();
            int size = entries.size();
            boolean overflow = size > QUEUE_VISIBLE;
            int visible = overflow ? QUEUE_VISIBLE - 1 : size;
            for (int i = 0; i < visible; i++) {
                int col = i % QUEUE_COLS;
                int row = i / QUEUE_COLS;
                if (this.isHovering(QUEUE_X + col * GRID_SLOT_SIZE, QUEUE_Y + row * GRID_SLOT_SIZE, 16, 16, mx, my)) {
                    int dropIdx = entries.get(i);
                    if (dropIdx >= 0 && dropIdx < drops.size()) {
                        List<Component> txt = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, drops.get(dropIdx)));
                        txt.add(Component.translatable("hostilenetworks.gui.queue_remove"));
                        gfx.renderComponentTooltip(this.font, txt, mx, my);
                    }
                }
            }
            if (overflow) {
                int col = (QUEUE_VISIBLE - 1) % QUEUE_COLS;
                int row = (QUEUE_VISIBLE - 1) / QUEUE_COLS;
                if (this.isHovering(QUEUE_X + col * GRID_SLOT_SIZE, QUEUE_Y + row * GRID_SLOT_SIZE, 16, 16, mx, my)) {
                    List<Component> txt = new ArrayList<>();
                    for (int j = visible; j < size; j++) {
                        int dropIdx = entries.get(j);
                        if (dropIdx >= 0 && dropIdx < drops.size()) txt.add(drops.get(dropIdx).getHoverName());
                    }
                    if (!txt.isEmpty()) gfx.renderComponentTooltip(this.font, txt, mx, my);
                }
            }
        }
        for (int y = 0; y < QUEUE_ROWS; y++) {
            for (int x = 0; x < QUEUE_COLS; x++) {
                int idx = y * 3 + x;
                if (idx < Math.min(drops.size() - this.currentPage * 9, 9) && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, mx, my)) {
                    List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, drops.get(this.currentPage * 9 + idx)));
                    if (queue) tip.add(Component.translatable("hostilenetworks.gui.queue_add"));
                    gfx.renderComponentTooltip(this.font, tip, mx, my);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (this.isHovering(BACK_X, BACK_Y, 18, 18, mx, my)) {
            this.playClickSound();
            Minecraft.getInstance().popGuiLayer();
            return true;
        }
        if (!this.model.isBound()) return super.mouseClicked(mx, my, btn);

        List<ItemStack> drops = this.model.get().fabDrops();
        FabSelection sel = this.selection();
        boolean queue = sel.mode() == ProductionMode.QUEUE;
        int selection = this.selectedDropIndex();

        if (this.isHovering(MODE_X, MODE_Y, 18, 18, mx, my)) {
            this.click(DataCenterMenu.FAB_CYCLE_MODE_BASE + this.slotIdx);
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int idx = this.currentPage * 9 + y * 3 + x;
                if (idx < drops.size() && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, mx, my)) {
                    if (queue || selection != idx) {
                        this.click(DataCenterMenu.FAB_DROP_BASE + this.slotIdx * DataCenterMenu.FAB_STRIDE + idx);
                    }
                }
            }
        }
        if (selection != -1 && this.isHovering(PREVIEW_X, PREVIEW_Y, 16, 16, mx, my)) {
            if (queue && !sel.entries().isEmpty()) {
                int queuePos = sel.cursor() % sel.entries().size();
                this.click(DataCenterMenu.FAB_QUEUE_REMOVE_BASE + this.slotIdx * DataCenterMenu.FAB_STRIDE + queuePos);
            }
            else if (!queue) {
                this.click(DataCenterMenu.FAB_CLEAR_FIXED_BASE + this.slotIdx);
            }
        }
        if (queue) {
            if (this.isHovering(CLEAR_X, CLEAR_Y, 18, 18, mx, my)) {
                this.click(DataCenterMenu.FAB_CLEAR_QUEUE_BASE + this.slotIdx);
            }
            List<Integer> entries = sel.entries();
            int size = entries.size();
            boolean overflow = size > QUEUE_VISIBLE;
            int visible = overflow ? QUEUE_VISIBLE - 1 : size;
            for (int i = 0; i < visible; i++) {
                int col = i % QUEUE_COLS;
                int row = i / QUEUE_COLS;
                if (this.isHovering(QUEUE_X + col * GRID_SLOT_SIZE, QUEUE_Y + row * GRID_SLOT_SIZE, 18, 18, mx, my)) {
                    this.click(DataCenterMenu.FAB_QUEUE_REMOVE_BASE + this.slotIdx * DataCenterMenu.FAB_STRIDE + i);
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void click(int id) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.parent.containerId, id);
        this.playClickSound();
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void renderBg(GuiGraphics gfx, float pt, int mx, int my) {
        int left = this.leftPos;
        int top = this.topPos;

        gfx.blit(BASE, left, top, 0, 0, 176, 83, 256, 256);

        gfx.blit(DataCenterScreen.BASE, left + BACK_X, top + BACK_Y, 36, 138, 18, 18, 256, 256);

        if (!this.model.isBound()) return;

        List<ItemStack> drops = this.model.get().fabDrops();
        FabSelection sel = this.selection();
        boolean queue = sel.mode() == ProductionMode.QUEUE;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (y * 3 + x < Math.min(drops.size() - this.currentPage * 9, 9) && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, mx, my)) {
                    gfx.blit(BASE, left + 16 + 19 * x, top + 8 + 19 * y, 13, 83, 18, 18, 256, 256);
                }
            }
        }

        int selection = this.selectedDropIndex();
        if (!queue && selection != -1 && selection / 9 == this.currentPage) {
            int selIdx = selection - this.currentPage * 9;
            gfx.blit(BASE, left + 16 + 19 * (selIdx % 3), top + 8 + 19 * (selIdx / 3), 31, 83, 18, 18, 256, 256);
        }
        if (selection != -1) {
            gfx.renderItem(drops.get(selection), left + PREVIEW_X, top + PREVIEW_Y);
            gfx.renderItemDecorations(this.font, drops.get(selection), left + PREVIEW_X - 1, top + PREVIEW_Y - 1);
        }
        if (queue) this.renderQueueGrid(gfx, left, top, sel, drops);

        int gridLeft = left + 17;
        int gridTop = top + 9;
        for (int i = 0; i < Math.min(drops.size() - this.currentPage * 9, 9); i++) {
            int x = i % 3;
            int y = i / 3;
            gfx.renderItem(drops.get(i + this.currentPage * 9), gridLeft + x * 19, gridTop + y * 19);
            gfx.renderItemDecorations(this.font, drops.get(i + this.currentPage * 9), gridLeft + x * 19 - 1, gridTop + y * 19 - 1);
        }

        gfx.blit(BASE, left + MODE_X, top + MODE_Y, 31, 83, 18, 18, 256, 256);
        gfx.blit(sel.mode().getResourceLocation(), left + MODE_X + 1, top + MODE_Y + 1, 0, 0, 16, 16, 16, 16);

        if (queue) {
            gfx.blit(BASE, left + CLEAR_X, top + CLEAR_Y, 31, 83, 18, 18, 256, 256);
            gfx.blit(CLEAR_QUEUE_ICON, left + CLEAR_X + 1, top + CLEAR_Y + 1, 0, 0, 16, 16, 16, 16);
        }
    }

    private void renderQueueGrid(GuiGraphics gfx, int left, int top, FabSelection sel, List<ItemStack> drops) {
        List<Integer> entries = sel.entries();
        int size = entries.size();
        boolean overflow = size > QUEUE_VISIBLE;
        int visible = overflow ? QUEUE_VISIBLE - 1 : size;
        int cursor = entries.isEmpty() ? -1 : sel.cursor() % size;

        for (int i = 0; i < QUEUE_VISIBLE; i++) {
            int col = i % QUEUE_COLS;
            int row = i / QUEUE_COLS;
            int cx = left + QUEUE_X + col * GRID_SLOT_SIZE;
            int cy = top + QUEUE_Y + row * GRID_SLOT_SIZE;
            boolean isCursor = i < visible && i == cursor;
            gfx.blit(BASE, cx, cy, isCursor ? 31 : 13, 83, 18, 18, 256, 256);
            if (i < visible) {
                int dropIdx = entries.get(i);
                if (dropIdx >= 0 && dropIdx < drops.size()) {
                    gfx.renderItem(drops.get(dropIdx), cx + 1, cy + 1);
                    gfx.renderItemDecorations(this.font, drops.get(dropIdx), cx, cy);
                }
            }
            else if (i == QUEUE_VISIBLE - 1 && overflow) {
                String txt = "+" + (size - visible);
                int tx = cx + 1 + (16 - this.font.width(txt)) / 2;
                int ty = cy + 2 + (16 - this.font.lineHeight) / 2;
                gfx.drawString(this.font, txt, tx, ty, Color.WHITE, true);
            }
        }
    }
}
